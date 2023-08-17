# localstack-loader

![Build](https://github.com/alikian/localstack-loader/actions/workflows/maven.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.alikian/localstack-loader)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.github.alikian%22)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://raw.githubusercontent.com/alikian/localstack-loader/main/LICENSE)

A Library to start localstack, no spring dependencies, so it can be used with any version of spring.
- Pull localstack docker image
- Start localstack docker container 
- Create Resources based on cloudformation template
- - with Full Cloudformation temple, is slow (takes more 30 seconds)
- - with Simple Cloudformation is fast but is doesn't check dependent resources, it has support for Secrets Managers, DynamoDB and SQS 
- Creat AWS Clients using AWS SDK v2

Create AWS resources using AWS Cloudformation template

Example without Spring:

pom.xml
```xml
  <dependency>
    <groupId>io.github.alikian</groupId>
    <artifactId>localstack-loader</artifactId>
    <version>1.0.23</version>
  </dependency>


```

```java
  LocalstackManager localstackManager =
        LocalstackManager.builder()
        .withRebuild(false)
        .withPort(4566)
        .withSimpleCloudformation("cloudformation.yaml")
        .withImageName("localstack/localstack:2.2.0")
        .buildSimple();
  SecretsManagerClient secretsClient = localstackManager.getSecretsManagerClient();

```

Spring boot example:
```java
package com.example;

import io.github.alikian.LocalstackManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@Configuration
@Profile({"test", "local"})
public class AwsConfig {

    LocalstackManager localstackManager = 
            LocalstackManager
                    .builder()
                    .withSimpleCloudformation("cloudformation.yaml")
                    .buildSimple();

    @Primary
    @Bean
    public DynamoDbClient getDynamoDbClient() {
        return localstackManager.getDynamoDbClient();
    }

    public SecretsManagerClient getSecretsManagerClient(){
        return localstackManager.getSecretsManagerClient();
    }

    @Primary
    @Bean
    public DynamoDbEnhancedClient getDynamoDbEnhancedClient() {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(getDynamoDbClient())
                .build();
    }
}

```
