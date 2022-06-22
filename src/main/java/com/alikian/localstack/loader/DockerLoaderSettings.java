package com.alikian.localstack.loader;

import lombok.Data;

@Data
public class DockerLoaderSettings {
    LocalStackSettings localStack;
    DockerSettings mysql;
}
