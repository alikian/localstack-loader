package com.alikian.localstack.loader;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DockerSettings {
    private Map<String, String> environment;
    private String services;
    private String containerName;
    private String imageName;
    private Integer imagePullTimeout;
    private Integer containerStartTimeout;
    private List<String> ports;
    private boolean enabled;
    private boolean containerRebuild;

}
