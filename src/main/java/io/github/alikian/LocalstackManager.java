package io.github.alikian;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.OnFailure;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * LocalstackManager
 */
@Slf4j
public class LocalstackManager {
    String imageName;
    String simpleCloudformationFileName;
    String fullCloudformationFileName;

    /**
     * getInstance
     * @return LocalstackManager
     */
    public static LocalstackManager getInstance() {
        return instance;
    }

    /**
     * Get DynamoDbClient
     * @return DynamoDbClient
     */
    public DynamoDbClient getDynamoDbClient() {
        return awsClients.getDynamoDbClient();
    }

    /**
     * Get AWS Secrets Manager Client
     * @return SecretsManagerClient
     */
    public SecretsManagerClient getSecretsManagerClient() {
        return awsClients.getSecretsManagerClient();
    }

    /**
     * Get AWS Cloudformation client
     * @return CloudFormationClient
     */
    public CloudFormationClient getCloudFormationClient() {
        return awsClients.getCloudFormationClient();
    }

    @Data
    static class AwsClients {
        SecretsManagerClient secretsManagerClient;
        CloudFormationClient cloudFormationClient;
        DynamoDbClient dynamoDbClient;
    }

    AwsClients awsClients;

    /**
     * Instantiate Builder
     * @return Builder
     */
    static public Builder builder() {
        return new Builder();
    }

    /**
     * Builder Class
     */
    public static class Builder {
        private Builder() {

        }

        String imageName = "localstack/localstack:2.2.0";
        String simpleCloudformationFileName = "cloudformation.yaml";
        String fullCloudformationFileName = "cloudformation.yaml";

        /**
         * Localstack Image name
         * @param imageName localstack image name
         * @return Builder
         */
        public Builder withImageName(String imageName) {
            this.imageName = imageName;
            return this;
        }

        /**
         * Simple doesn't have full Cloudformation capability
         * @param simpleCloudformationFileName Simple Cloudformation FileName
         * @return Builder
         */
        public Builder withSimpleCloudformation(String simpleCloudformationFileName) {
            this.simpleCloudformationFileName = simpleCloudformationFileName;
            return this;
        }

        /**
         * Full name of Cloudformation
         * @param fullCloudformationFileName file name
         * @return Builder
         */
        public Builder withFullCloudformation(String fullCloudformationFileName) {
            this.fullCloudformationFileName = fullCloudformationFileName;
            return this;
        }

        /**
         * Simple Build
         * @return LocalstackManager
         */
        public LocalstackManager buildSimple() {
            if (instance == null) {
                instance = new LocalstackManager(imageName, simpleCloudformationFileName);
                instance.start();

                return instance;
            } else {
                throw new RuntimeException("Build already called");
            }
        }

        /**
         * Full Cloudformation build
         * @return LocalstackManager
         */
        public LocalstackManager buildFull() {
            if (instance == null) {
                instance = new LocalstackManager(imageName, fullCloudformationFileName);
                instance.start();

                return instance;
            } else {
                throw new RuntimeException("Build already called");
            }
        }
    }

    private void start() {
        startDocker();
        buildClients();
        createResources();
    }

    private void buildClients() {
        LocalstackClientBuilder localstackClientBuilder = new LocalstackClientBuilder(localstack);
        awsClients = localstackClientBuilder.buildAwsClients();

    }

    private LocalstackManager(String imageName, String simpleCloudformationFileName) {
        this.imageName = imageName;
        this.simpleCloudformationFileName = simpleCloudformationFileName;
    }

    private LocalStackContainer localstack;


    private static LocalstackManager instance;

    private void startDocker() {
        log.info("startDocker");
        DockerImageName localstackImage = DockerImageName.parse(imageName);

        localstack = new LocalStackContainer(localstackImage);
        localstack.start();
    }


    private void createResources() {
        ResourceBuilder resourceBuilder = new ResourceBuilder(simpleCloudformationFileName, awsClients);
        resourceBuilder.buildResources();
    }

    private void createStack() {
        String stackName = "heroes";
        log.debug("creating stack");
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fullCloudformationFileName);
            InputStreamReader streamReader =
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(streamReader);
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            String template = line;
            CloudFormationWaiter waiter = getCloudFormationClient().waiter();
            CreateStackRequest stackRequest = CreateStackRequest.builder()
                    .stackName(stackName)
                    .templateBody(template)
                    .onFailure(OnFailure.ROLLBACK)
                    .build();
            getCloudFormationClient().createStack(stackRequest);
            DescribeStacksRequest stacksRequest = DescribeStacksRequest.builder()
                    .stackName(stackName)
                    .build();

            WaiterResponse<DescribeStacksResponse> waiterResponse = waiter.waitUntilStackCreateComplete(stacksRequest);
            waiterResponse.matched().response().ifPresent(System.out::println);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


}