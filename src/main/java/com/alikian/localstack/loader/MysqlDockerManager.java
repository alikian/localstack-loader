package com.alikian.localstack.loader;

import com.github.dockerjava.api.DockerClient;

public class MysqlDockerManager extends DockerManager{
    public MysqlDockerManager(DockerSettings dockerSettings, DockerClient dockerClient) {
        super(dockerSettings, dockerClient);
    }

    @Override
    public void waitForContainerToStart() {

    }

    @Override
    public void createResources() {

    }
}
