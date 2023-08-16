package io.github.alikian;

import io.github.alikian.docker.DockerManager;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Localstack AWS Client
 */
public class LocalstackClientBuilder {
    DockerManager dockerManager;
    AwsCredentialsProvider awsCredentialsProvider;

    LocalstackClientBuilder(DockerManager dockerManager) {
        this.dockerManager = dockerManager;
        AwsBasicCredentials awsBasicCredentials =
                AwsBasicCredentials.create(dockerManager.getAccessKey(),
                        dockerManager.getSecretKey());
        awsCredentialsProvider = StaticCredentialsProvider.create(awsBasicCredentials);

    }

    public SecretsManagerClient getSecretsManagerClient() {
        return SecretsManagerClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(dockerManager.getRegion())
                .endpointOverride(dockerManager.getEndpointURI()).build();
    }

    public CloudFormationClient getCfClient() {
        return CloudFormationClient.builder().
                credentialsProvider(awsCredentialsProvider)
                .region(dockerManager.getRegion())
                .endpointOverride(dockerManager.getEndpointURI())
                .build();
    }

    /**
     * Get Dynamo Db Client
     * @return DynamoDbClient
     */
    public DynamoDbClient getDynamoDbClient(){
        return DynamoDbClient.builder()
                .endpointOverride(dockerManager.getEndpointURI())
                .region(dockerManager.getRegion())
                .credentialsProvider(awsCredentialsProvider)
                .build();
    }

    public SqsClient getSqsClient() {
        return SqsClient.builder()
                .endpointOverride(dockerManager.getEndpointURI())
                .region(dockerManager.getRegion())
                .credentialsProvider(awsCredentialsProvider)
                .build();
    }

    public LocalstackManager.AwsClients buildAwsClients() {
        LocalstackManager.AwsClients awsClients=new LocalstackManager.AwsClients();
        awsClients.setDynamoDbClient(getDynamoDbClient());
        awsClients.setSecretsManagerClient(getSecretsManagerClient());
        awsClients.setCloudFormationClient(getCfClient());
        awsClients.setSqsClient(getSqsClient());
        return awsClients;
    }
}