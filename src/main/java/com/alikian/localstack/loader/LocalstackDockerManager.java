package com.alikian.localstack.loader;

import com.alikian.localstack.resources.DynamoDbResource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class LocalstackDockerManager extends DockerManager {
    private static final Logger logger = LoggerFactory.getLogger(LocalstackDockerManager.class);
    LocalStackSettings localStackSettings;

    public LocalstackDockerManager(DockerSettings dockerSettings, DockerClient dockerClient) {
        super(dockerSettings, dockerClient);
        this.localStackSettings = (LocalStackSettings) dockerSettings;
    }

    public void waitForContainerToStart() {

        for (int i = 0; i < localStackSettings.getContainerStartTimeout(); i++) {
            logger.debug("Pinging container ");
            boolean ping = pingURL("http://localhost:4566/health", 100);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (ping) {
                logger.debug("Ping container success");
                return;
            }
        }
        throw new RuntimeException("Container timeout failed");

    }

    public void createResources() {
        try {
            if (!alreadyStarted) {
                createSqsResources();
                createSnsResources();
                createSecretsManager();
                createDynamoDbResources();
            }
        } catch (URISyntaxException | JsonProcessingException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void createSecretsManager() throws URISyntaxException, JsonProcessingException {
        if (localStackSettings.getResources().getSecretsManager() == null) {
            return;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        SecretsManagerClient secretsClient = SecretsManagerClient.builder()
                .endpointOverride(new URI(localStackSettings.getEndpoint()))
                .region(Region.US_WEST_2)
                .build();
        for (LocalStackSettings.Resources.SecretsManagerResource secretsManagerResource : localStackSettings.getResources().getSecretsManager()) {
            String secretValue = "";
            if (secretsManagerResource.getKeyValues() != null) {
                secretValue = objectMapper.writeValueAsString(secretsManagerResource.getKeyValues());
            }
            if (secretsManagerResource.getPlaintext() != null) {
                secretValue = secretsManagerResource.getPlaintext();
                secretValue = secretValue.replace("\n", "");
                secretValue = secretValue.replaceAll("\\s+", " ");
            }
            CreateSecretRequest secretRequest = CreateSecretRequest.builder()
                    .name(secretsManagerResource.getName())
                    .secretString(secretValue)
                    .build();
            secretsClient.createSecret(secretRequest);
            logger.debug("SecretsManager {} create", secretsManagerResource.getName());
        }
    }

    private void createSnsResources() throws URISyntaxException {
        if (localStackSettings.getResources().getSns() == null) {
            return;
        }
        SnsClient snsClient = SnsClient.builder()
                .endpointOverride(new URI(localStackSettings.getEndpoint()))
                .region(Region.US_WEST_2)
                .build();
        for (String topicName : localStackSettings.getResources().getSns()) {
            CreateTopicRequest request = CreateTopicRequest.builder()
                    .name(topicName)
                    .build();
            snsClient.createTopic(request);
            logger.debug("Sns topic {} created", topicName);
        }
    }

    private void createSqsResources() throws URISyntaxException {
        if (localStackSettings.getResources().getSqs() == null) {
            return;
        }
        SqsClient sqs = SqsClient.builder()
                .endpointOverride(new URI(localStackSettings.getEndpoint()))
                .region(Region.US_WEST_2)
                .build();
        for (String queName : localStackSettings.getResources().getSqs()) {
            sqs.createQueue(CreateQueueRequest.builder().queueName(queName).build());
            logger.debug("Que {} created", queName);
        }
    }

    private void createDynamoDbResources() throws URISyntaxException {
        if (localStackSettings.getResources().getDynamoDb() == null) {
            return;
        }
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(new URI(localStackSettings.getEndpoint()))
                .region(Region.US_WEST_2)
                .build();
        for (DynamoDbResource dynamoDbResource : localStackSettings.getResources().getDynamoDb()) {
            CreateTableRequest.Builder builder = CreateTableRequest.builder().tableName(dynamoDbResource.getTableName());

            List<AttributeDefinition> attributeDefinitionList = new ArrayList<>();
            List<KeySchemaElement> keySchemaElementList = new ArrayList<>();
            dynamoDbResource.getAttributeDefinitions().forEach(attributeDefinition -> attributeDefinitionList.add(AttributeDefinition.builder()
                    .attributeName(attributeDefinition.getAttributeName())
                    .attributeType(attributeDefinition.getAttributeType())
                    .build()));
            dynamoDbResource.getKeySchema().forEach(keySchema -> keySchemaElementList.add(KeySchemaElement.builder()
                    .attributeName(keySchema.getAttributeName())
                    .keyType(keySchema.getKeyType())
                    .build()));
            builder.provisionedThroughput(ProvisionedThroughput.builder()
                    .readCapacityUnits(dynamoDbResource.getProvisionedThroughput().getReadCapacityUnits())
                    .writeCapacityUnits(dynamoDbResource.getProvisionedThroughput().getWriteCapacityUnits())
                    .build());
            builder.attributeDefinitions(attributeDefinitionList);
            builder.keySchema(keySchemaElementList);

            if (dynamoDbResource.getGlobalSecondaryIndex() != null) {
                DynamoDbResource.GlobalSecondaryIndex gsi = dynamoDbResource.getGlobalSecondaryIndex();
                GlobalSecondaryIndex.Builder gsiBuilder = GlobalSecondaryIndex.builder()
                        .indexName(gsi.getIndexName())
                        .provisionedThroughput(ProvisionedThroughput.builder()
                                .readCapacityUnits(gsi.getProvisionedThroughput().getReadCapacityUnits())
                                .writeCapacityUnits((gsi.getProvisionedThroughput().getWriteCapacityUnits()))
                                .build())
                        .projection(Projection.builder()
                                .projectionType(ProjectionType.ALL)
                                .build());

                List<KeySchemaElement> keySchemaElementListGsi = new ArrayList<>();
                gsi.getKeySchema().forEach(keySchema -> keySchemaElementListGsi.add(KeySchemaElement.builder()
                        .attributeName(keySchema.getAttributeName())
                        .keyType(keySchema.getKeyType())
                        .build()));

                builder.globalSecondaryIndexes(gsiBuilder
                        .keySchema(keySchemaElementListGsi)
                        .build());
            }

            dynamoDbClient.createTable(builder.build());
            logger.debug("DynamoDb Table {} create", dynamoDbResource.getTableName());
        }
    }

}
