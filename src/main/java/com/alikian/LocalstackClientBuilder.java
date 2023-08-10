package com.alikian;

import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class LocalstackClientBuilder {
    LocalStackContainer localstack;
    AwsCredentialsProvider awsCredentialsProvider;

    LocalstackClientBuilder(LocalStackContainer localstack) {
        this.localstack = localstack;
        AwsBasicCredentials awsBasicCredentials =
                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey());
        awsCredentialsProvider = StaticCredentialsProvider.create(awsBasicCredentials);

    }

    public SecretsManagerClient getSecretsManagerClient() {
        return SecretsManagerClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(Region.of(localstack.getRegion()))
                .endpointOverride(localstack.getEndpoint()).build();
    }

    public CloudFormationClient getCfClient() {
        return CloudFormationClient.builder().
                credentialsProvider(awsCredentialsProvider)
                .region(Region.of(localstack.getRegion()))
                .endpointOverride(localstack.getEndpoint())
                .build();
    }

    public DynamoDbClient getDynamoDbClient(){
        return DynamoDbClient.builder()
                .endpointOverride(localstack.getEndpoint())
                .region(Region.of(localstack.getRegion()))
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
