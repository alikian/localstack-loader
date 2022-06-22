package com.alikian.localstack.loader;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.*;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
public abstract class DockerManager {
    private static final Logger logger = LoggerFactory.getLogger(DockerManager.class);
    private DockerSettings dockerSettings;
    private DockerClient dockerClient;
    private String containerId;
    private String containerName;
    boolean started;
    // To set after resource creation completed
    boolean success;
    boolean alreadyStarted;
    boolean enabled;

    protected DockerManager(DockerSettings dockerSettings, DockerClient dockerClient) {
        if(dockerSettings == null){
            enabled = false;
            return;
        }
        this.enabled = dockerSettings.isEnabled();
        this.dockerSettings = dockerSettings;
        this.containerName = dockerSettings.getContainerName();
        this.dockerClient = dockerClient;
    }

    private Container findContainer() {
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        Container containerFound = null;
        for (Container container : containers) {
            if (containerName.equals(container.getNames()[0].substring(1))) {
                containerFound = container;
                containerId = container.getId();
                break;
            }
        }
        return containerFound;
    }

    private void checkAndPullImage() {
        List<Image> images = dockerClient.listImagesCmd().exec();
        boolean imageFound = false;
        for (Image image : images) {
            if (image.getRepoTags() != null
                    && image.getRepoTags().length > 0
                    && image.getRepoTags()[0].equals(dockerSettings.getImageName())) {
                logger.debug("localstack image found");
                imageFound = true;
                break;
            }
        }
        if (!imageFound) {
            logger.debug("localstack image not found, pulling image");
            try {
                dockerClient.pullImageCmd(dockerSettings.getImageName())
                        .exec(new PullImageResultCallback())
                        .awaitCompletion(dockerSettings.getImagePullTimeout(), TimeUnit.SECONDS);
                logger.debug("localstack image not found, pulling image success");
            } catch (InterruptedException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }


    public void start() {

        if (!enabled) {
            return;
        }

        Container containerFound = findContainer();
        alreadyStarted = false;

        if (containerFound != null) {
            logger.debug("Container Found name {}: {}", dockerSettings.getContainerName(), containerId);
            String state = containerFound.getState();
            if (dockerSettings.isContainerRebuild()) {
                logger.debug("Rebuilding container", dockerSettings.getContainerName(), containerId);
                if ("running".equals(state)) {
                    started = true;
                    logger.debug("Stopping existing container {}", dockerSettings.getContainerName());
                    stop();
                }
                logger.debug("Deleting existing container id {}", containerId);
                dockerClient.removeContainerCmd(containerId).exec();
                createAndStartContainer();
            } else {
                if ("exited".equals(state)) {
                    logger.debug("Starting existing container {}", dockerSettings.getContainerName());
                    dockerClient.startContainerCmd(containerId).exec();
                    waitForContainerToStart();
                }
                if ("running".equals(state)) {
                    alreadyStarted = true;
                    logger.debug("Container already running");
                }
            }
        } else {

            checkAndPullImage();

            createAndStartContainer();
        }
        started = true;
        if (!alreadyStarted) {
            createResources();
        }
        success = true;
    }

    private void createAndStartContainer() {
        logger.debug("Create and start new Container");

        Ports ports = getPorts();
        List<String> environment = new ArrayList<>();
        for (Map.Entry<String, String> entry : dockerSettings.getEnvironment().entrySet()) {
            environment.add(entry.getKey() + "=" + entry.getValue());
        }
        CreateContainerCmd createContainerCmd =
                dockerClient.createContainerCmd(dockerSettings.getImageName());
        CreateContainerResponse createContainerResponse
                = createContainerCmd
                .withHostConfig(HostConfig.newHostConfig().withPortBindings(ports))
                .withName(dockerSettings.getContainerName())
                .exec();
        containerId = createContainerResponse.getId();
        dockerClient.startContainerCmd(containerId).exec();

        createContainerCmd.withEnv(environment);
        waitForContainerToStart();
        logger.debug("Started Container: {}", containerId);
    }

    private Ports getPorts() {
        Ports portBindings = new Ports();
        for (String portMapping : dockerSettings.getPorts()) {
            String[] ports = portMapping.split(":");
            ExposedPort exposedPort = ExposedPort.tcp(Integer.parseInt(ports[0]));
            portBindings.bind(exposedPort, Ports.Binding.bindPort(Integer.parseInt(ports[1])));

        }
        return portBindings;
    }

    public static boolean pingURL(String url, int timeout) {

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode <= 399);
        } catch (IOException exception) {
            return false;
        }
    }

    public void stop() {
        if (started) {
            logger.debug("Shutting down docker {}", dockerSettings.getContainerName());
            dockerClient.stopContainerCmd(containerId).exec();
            logger.debug("Stopped Container {} image: {}", dockerSettings.getContainerName(), containerId);
        }
    }

    public abstract void waitForContainerToStart();

    public abstract void createResources();

}
