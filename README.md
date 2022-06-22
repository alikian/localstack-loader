# localstack-loader
A helper library for docker and localstack
This starter will spin up the docker container using docker-java

## Motivation
Usually in local development we need to use Docker for example localstack or mysql
Localstack simulate AWS services in your local, but having it running is not enough we
often need create shell scripts to create resources. This library add following
functionality to spring boot:

- During the App Startup Spring Docker Loader container will do followings:
    - It will pull the image if not exist
    - It will create container if not exist
    - It will start the container if is not started already
    - For localstack container it creates resources bases on configuration properties
- During the App Shutdown Spring Docker Loader container will do followings:
    - Stop container

## configuration example:

```yaml
dockerloader:
  enabled: true
  localstack:
    enabled: true
    containerRebuild: true
    environment:
      SERVICES: sqs,sns,secretsmanager
      DEBUG: 1
    containerName: localstack
    imageName: localstack/localstack:latest
    imagePullTimeout: 60
    ports:
      - '4566:4566'
      - '4571:4571'
    endpoint: "http://localhost:4566"
    resources:
      sqs:
        - sqs-notification
      sns:
        - sns-notification
      secretsManager:
        - name: adyen-payment-gateway
          keyValues:
            user: test
            password: hello123
        - name: json-test
          plaintext: |+
            {
              "keys":{
                "apiKey": "test",
                "hmacKey": "test",
              }
            }

      dynamoDb:
        - tableName: firstTable
          attributeDefinitions:
            - attributeName: Album
              attributeType: S
            - attributeName: Artist
              attributeType: S
          keySchema:
            - attributeName: Album
              keyType: HASH
            - attributeName: Artist
              keyType: RANGE
          provisionedThroughput:
            readCapacityUnits: 5
            writeCapacityUnits: 5
        - tableName: secondTable
          attributeDefinitions:
            - attributeName: Name
              attributeType: S
            - attributeName: Id
              attributeType: S
          keySchema:
            - attributeName: Name
              keyType: HASH
            - attributeName: Id
              keyType: RANGE
          provisionedThroughput:
            readCapacityUnits: 5
            writeCapacityUnits: 5
  mysql:
    enabled: true
    containerRebuild: false
    imageName: mysql:5.7
    containerName: mysql
    imagePullTimeout: 60
    environment:
      MYSQL_DATABASE: 'paygateway'
      MYSQL_ROOT_PASSWORD: 'root'
    ports:
      - '3306:3306'

```
Note: For Dynamo DB Resources, we need to maintain same number of attributes for key schema and attribute definitions.
## Requirements
* JDK 8+
* Tested with SpringBoot 2.3.3.RELEASE, should work fine with any SpringBoot 2.x versions

## How to use?

### Add dependencies

**Maven**

```xml
<dependencies>
    <dependency>
        <groupId>com.alikian</groupId>
        <artifactId>localstack-loader</artifactId>
        <version>0.0.1</version>
    </dependency>
</dependencies>
```

### Enable DockerLoader AutoConfiguration
You can enable LocalStack AutoConfiguration by adding `@EnableDockerLoader` annotation to either main entrypoint class or
any `@Configuration` class.


```java
package com.alikian.localstack.loader.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootApplication
@EnableDockerLoader
public class DockerLoaderStarterDemoApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(LocalStackStarterDemoApplication.class, args);
    }
}
```
## TODOs
- Test MySQL is up
- Add support for multiple generic container refactoring required
