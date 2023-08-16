package com.alikian.docker;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class DockerSettings {
    private Map<String, String> environment = new HashMap<>();
    private String services;
    private String containerName = "localstack-loader";
    private String imageName = "localstack/localstack:2.2.0";
    private Integer imagePullTimeout = 60;
    private Integer containerStartTimeout = 60;
    private Integer port = 4566;
    private boolean enabled = true;
    private boolean containerRebuild = true;

}
