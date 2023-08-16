package io.github.alikian.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.regions.Region;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Data
@Slf4j
public class DockerManager {
    private DockerSettings dockerSettings;
    private DockerClient dockerClient;
    private Container containerFound;
    boolean started;
    // To set after resource creation completed
    boolean success;
    boolean alreadyStarted;
    boolean enabled;
    boolean containerCreated;

    public DockerManager(DockerSettings dockerSettings) {
        DefaultDockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .build();

        //building http docker client using the docker config
        DockerHttpClient dockerHttpClient = new OkDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        //building docker client using the above config and httpclient
        this.dockerClient = DockerClientImpl.getInstance(config, dockerHttpClient);

        this.enabled = dockerSettings.isEnabled();
        this.dockerSettings = dockerSettings;
    }

    private Container findContainer() {
        log.info("looking for container name: {}", dockerSettings.getContainerName());
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        Container containerFound = null;
        for (Container container : containers) {
            if (dockerSettings.getContainerName().equals(container.getNames()[0].substring(1))) {
                containerFound = container;
                break;
            }
        }
        return containerFound;
    }

    private void checkAndPullImage() {
        log.info("Checking for image: {}", dockerSettings.getImageName());
        List<Image> images = dockerClient.listImagesCmd().exec();
        boolean imageFound = false;
        for (Image image : images) {
            if (image.getRepoTags() != null
                    && image.getRepoTags().length > 0
                    && image.getRepoTags()[0].equals(dockerSettings.getImageName())) {
                log.info("localstack image found");
                imageFound = true;
                break;
            }
        }
        if (!imageFound) {
            log.info("localstack image not found, pulling image timeout: {} Seconds",
                    dockerSettings.getImagePullTimeout());
            try {
                dockerClient.pullImageCmd(dockerSettings.getImageName())
                        .exec(new LoggedPullImageResultCallback(log))
                        .awaitCompletion(dockerSettings.getImagePullTimeout(), TimeUnit.SECONDS);
                log.info("localstack image not found, pulling image success");
            } catch (InterruptedException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }


    public void start() {

        if (!enabled) {
            return;
        }

        containerFound = findContainer();
        alreadyStarted = false;

        if (containerFound != null) {
            log.info("Container Found name {}: {}", dockerSettings.getContainerName(), containerFound.getId());
            String state = containerFound.getState();
            if (!containerFound.getImage().equals(dockerSettings.getImageName())) {
                log.info("Container found with different image name, rebuilding");
                dockerSettings.setContainerRebuild(true);
            }
            if (dockerSettings.isContainerRebuild()) {
                log.info("Rebuilding container {},{}", dockerSettings.getContainerName(), containerFound.getId());
                if ("running".equals(state)) {
                    started = true;
                    log.info("Stopping existing container {}", dockerSettings.getContainerName());
                    stop();
                }
                log.info("Deleting existing container id {}", containerFound.getId());
                dockerClient.removeContainerCmd(containerFound.getId()).exec();
                createAndStartContainer();
            } else {
                if ("exited".equals(state)) {
                    log.info("Starting existing container {}", dockerSettings.getContainerName());
                    dockerClient.startContainerCmd(containerFound.getId()).exec();
                    waitForContainerToStart();
                }
                if ("running".equals(state)) {
                    alreadyStarted = true;
                    log.info("Container already running");
                }
            }
        } else {

            checkAndPullImage();

            createAndStartContainer();
        }
        started = true;
        success = true;
    }

    private void createAndStartContainer() {
        log.debug("Create and start new Container");

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
        String containerId = createContainerResponse.getId();
        dockerClient.startContainerCmd(containerId).exec();

        createContainerCmd.withEnv(environment);
        waitForContainerToStart();
        log.debug("Started Container: {}", containerId);
        containerCreated = true;
    }

    private Ports getPorts() {
        Ports portBindings = new Ports();
        ExposedPort exposedPort = ExposedPort.tcp(4566);
        portBindings.bind(exposedPort, Ports.Binding.bindPort(dockerSettings.getPort()));
        return portBindings;
    }

    public boolean pingURL(String url, int timeout) {
        log.info("pinging url:{}", url);

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            boolean success = (200 == responseCode);
            if (success) {
                log.info("Localstack health: ");
                BufferedReader br = new BufferedReader(new InputStreamReader((connection.getInputStream())));
                String output = br.lines().collect(Collectors.joining());
                log.info("health: {}", output);
            }

            return success;
        } catch (IOException exception) {
            return false;
        }
    }

    public void stop() {
        if (started) {
            log.info("Shutting down docker {}", dockerSettings.getContainerName());
            dockerClient.stopContainerCmd(containerFound.getId()).exec();
            log.info("Stopped Container {} image: {}", dockerSettings.getContainerName(), containerFound.getId());
        }
    }

    public void waitForContainerToStart() {
        log.info("Pinging container timeout: {}", dockerSettings.getContainerStartTimeout());

        for (int i = 0; i < dockerSettings.getContainerStartTimeout(); i++) {
            boolean ping = pingURL(getEndpoint() + "_localstack/health", 1000);
            if (ping) {
                log.debug("Ping container success");
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        throw new RuntimeException("Container timeout failed");

    }


    public String getAccessKey() {
        return "123";
    }

    public String getSecretKey() {
        return "123";
    }

    public Region getRegion() {
        return Region.of("us-west-2");
    }

    public String getEndpoint() {
        return "http://localhost:" + dockerSettings.getPort() + "/";
    }

    @SneakyThrows
    public URI getEndpointURI() {
        return new URI(getEndpoint());
    }

    @Override
    protected void finalize() {
        stop();
    }
}