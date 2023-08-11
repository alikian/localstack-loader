package com.alikian;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * Localstack AWS Client
 */
public class LocalstackClientBuilder {
    LocalstackDockerContainer localstackDockerContainer;
    AwsCredentialsProvider awsCredentialsProvider;

    LocalstackClientBuilder(LocalstackDockerContainer localstackDockerContainer) {
        this.localstackDockerContainer = localstackDockerContainer;
        AwsBasicCredentials awsBasicCredentials =
                AwsBasicCredentials.create(localstackDockerContainer.getAccessKey(),
                        localstackDockerContainer.getSecretKey());
        awsCredentialsProvider = StaticCredentialsProvider.create(awsBasicCredentials);

    }

    public SecretsManagerClient getSecretsManagerClient() {
        return SecretsManagerClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(Region.of(localstackDockerContainer.getRegion()))
                .endpointOverride(localstackDockerContainer.getEndpoint()).build();
    }

    public CloudFormationClient getCfClient() {
        return CloudFormationClient.builder().
                credentialsProvider(awsCredentialsProvider)
                .region(Region.of(localstackDockerContainer.getRegion()))
                .endpointOverride(localstackDockerContainer.getEndpoint())
                .build();
    }

    /**
     * Get Dynamo Db Client
     * @return DynamoDbClient
     */
    public DynamoDbClient getDynamoDbClient(){
        return DynamoDbClient.builder()
                .endpointOverride(localstackDockerContainer.getEndpoint())
                .region(Region.of(localstackDockerContainer.getRegion()))
                .credentialsProvider(awsCredentialsProvider)
                .build();
    }

    public LocalstackManager.AwsClients buildAwsClients() {
        LocalstackManager.AwsClients awsClients=new LocalstackManager.AwsClients();
        awsClients.setDynamoDbClient(getDynamoDbClient());
        awsClients.setSecretsManagerClient(getSecretsManagerClient());
        awsClients.setCloudFormationClient(getCfClient());
        return awsClients;
    }
}
