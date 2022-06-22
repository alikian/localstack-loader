package com.alikian.localstack.loader;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

public class DockerLoaderClient {
    private static DockerLoaderClient dockerLoaderClient;

    DockerLoaderManager dockerLoaderManager;

    public static DockerLoaderClient getInstance(String configFile) {
        if (dockerLoaderClient == null) {
            try {
                dockerLoaderClient = new DockerLoaderClient(configFile);
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException("Config not found");
            }
        }
        return dockerLoaderClient;
    }

    private DockerLoaderClient(String configFile) throws IOException, URISyntaxException {
        InputStream inputStream = this.getClass().getResourceAsStream(configFile);
        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(new Constructor(DockerLoaderSettings.class),representer);
        DockerLoaderSettings dockerLoaderSettings = yaml.load(inputStream);
        dockerLoaderManager = new DockerLoaderManager(dockerLoaderSettings);
    }

    public DockerLoaderManager getDockerLoaderManager() {
        return dockerLoaderManager;
    }

    public void start(){
        dockerLoaderManager.start();
    }

    public void stop(){
        dockerLoaderManager.stop();
    }
}
