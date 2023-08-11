package com.alikian;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Duration;

@Slf4j
public class LocalstackDockerContainer {

    String imageName;

    LocalstackDockerContainer(String imageName){
        this.imageName= imageName;

        ExposedPort tcp4566 = ExposedPort.tcp(4566);
        Ports portBindings = new Ports();
        portBindings.bind(tcp4566,Ports.Binding.bindPort(4566));

        DockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }
    DockerClient dockerClient;
    public void pull() {
        log.info("Pull Image " + imageName + " Start");
        try {
            dockerClient
                    .pullImageCmd(imageName)
                    .exec(new PullImageResultCallback())
                    .awaitCompletion();
        } catch (InterruptedException e) {
            log.error("Pull Image Error", e);
            throw new RuntimeException("Pull Image Error");
        }
        log.info("Pull Image " + imageName + " Success");
    }

    public String getRegion(){
        return "us-west-2";
    }

    @SneakyThrows
    public URI getEndpoint(){
        return new URI("http://localhost:4566");
    }

    public void start() {
        pull();
        dockerClient.pingCmd().exec();
    }

    public String getAccessKey() {
        return "123";
    }

    public String getSecretKey() {
        return "123";
    }
}
