package com.alikian.localstack.loader;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import javax.annotation.PreDestroy;


@Data
public class DockerLoaderManager {
    private static final Logger logger = LoggerFactory.getLogger(DockerLoaderManager.class);
    DockerLoaderSettings dockerLoaderSettings;
    LocalstackDockerManager localstackDockerManager;
    MysqlDockerManager mysqlDockerManager;

    public DockerLoaderManager(DockerLoaderSettings dockerLoaderSettings) {
        //building default docker configuration
        DefaultDockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .build();

        //building http docker client using the docker config
        DockerHttpClient dockerHttpClient = new OkDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        //building docker client using the above config and httpclient
        DockerClient dockerClient = DockerClientImpl.getInstance(config, dockerHttpClient);

        /**
         * Working for mac but not working for windows
         *
         * DockerClient dockerClient1 = DockerClientBuilder.getInstance().build();
         */

        this.dockerLoaderSettings = dockerLoaderSettings;

        LocalStackSettings localStackSettings = dockerLoaderSettings.getLocalStack();
        this.localstackDockerManager = new LocalstackDockerManager(localStackSettings, dockerClient);

        DockerSettings mysqlSettings = dockerLoaderSettings.getMysql();
        this.mysqlDockerManager = new MysqlDockerManager(mysqlSettings, dockerClient);
    }

    public void start() {
        localstackDockerManager.start();
        mysqlDockerManager.start();
    }

//    @PreDestroy
    public void stop() {
        logger.debug("Shutting down docker");
        localstackDockerManager.stop();
        mysqlDockerManager.stop();
    }

}
