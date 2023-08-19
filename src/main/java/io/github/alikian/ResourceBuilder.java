package io.github.alikian;

import io.github.alikian.aws.DynamoDBProperties;
import io.github.alikian.aws.SQSProperties;
import io.github.alikian.aws.SecretManagerProperties;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AWS Resource Builder for Simple CloudFormation
 */
@Slf4j
public class ResourceBuilder {
    ObjectMapper objectMapper;

    LocalstackManager.AwsClients awsClients;

    Map cloudFormation;

    ResourceBuilder(String cloudformationFileName,
                    LocalstackManager.AwsClients awsClients) {
        Yaml yaml = new Yaml();

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(cloudformationFileName)) {
            this.cloudFormation = yaml.loadAs(input, Map.class);
            ;
            this.awsClients = awsClients;
            objectMapper = new ObjectMapper();
//      Use this if all properties are not in the class
//      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Build individual AWS resources
     */
    public void buildResources() {
        Map<String, Object> resources = (Map<String, Object>) cloudFormation.get("Resources");
        for (Map.Entry<String, Object> entry : resources.entrySet()) {

            Map resource = (Map) entry.getValue();
            String type = (String) resource.get("Type");
            Map properties = (Map) resource.get("Properties");

            switch (type) {
                case "AWS::SecretsManager::Secret":
                    SecretManagerProperties secretManagerProperties
                            = objectMapper.convertValue(properties, SecretManagerProperties.class);
                    createSecrets(secretManagerProperties);
                    break;
                case "AWS::DynamoDB::Table":
                    DynamoDBProperties dynamoDBProperties
                            = objectMapper.convertValue(properties, DynamoDBProperties.class);
                    createDynamoDBs(dynamoDBProperties);
                    break;
                case "AWS::SQS::Queue":
                    SQSProperties sqsProperties
                            = objectMapper.convertValue(properties, SQSProperties.class);
                    createSQSs(sqsProperties);
            }
        }
    }

    private void createSQSs(SQSProperties sqsProperties) {
        Map<QueueAttributeName, String> attributes = new HashMap<>();
        if(sqsProperties.getDeduplicationScope()!=null) {
            attributes.put(QueueAttributeName.DEDUPLICATION_SCOPE, sqsProperties.getDeduplicationScope());
        }
        if (sqsProperties.getDelaySeconds() != null) {
            attributes.put(QueueAttributeName.DELAY_SECONDS, sqsProperties.getDelaySeconds().toString());
        }
        CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                .attributes(attributes)
                .queueName(sqsProperties.getQueueName())
                .build();
        awsClients.getSqsClient().createQueue(createQueueRequest);
        log.info("SQS created: {}", sqsProperties.getQueueName());
    }

    private void createDynamoDBs(DynamoDBProperties ddbProperties) {
        CreateTableRequest.Builder requestBuilder = CreateTableRequest.builder();
        requestBuilder.tableName(ddbProperties.getTableName());
        List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
        for (DynamoDBProperties.AttrNameType attrNameType : ddbProperties.getAttributeDefinitions()) {
            attributeDefinitions.add(AttributeDefinition.builder()
                    .attributeName(attrNameType.getAttributeName())
                    .attributeType(attrNameType.getAttributeType())
                    .build());
        }
        requestBuilder.attributeDefinitions(attributeDefinitions);

        List<KeySchemaElement> keySchemaElements = new ArrayList<>();
        for (DynamoDBProperties.AttrNameKeyType attrNameKeyType : ddbProperties.getKeySchema()) {
            keySchemaElements.add(KeySchemaElement.builder()
                    .attributeName(attrNameKeyType.getAttributeName())
                    .keyType(attrNameKeyType.getKeyType())
                    .build());
        }
        requestBuilder.keySchema(keySchemaElements);

        requestBuilder.provisionedThroughput(ProvisionedThroughput.builder()
                .readCapacityUnits(
                        Long.valueOf(ddbProperties.getProvisionedThroughput().getReadCapacityUnits()))
                .writeCapacityUnits(
                        Long.valueOf(ddbProperties.getProvisionedThroughput().getWriteCapacityUnits()))

                .build());
        List<GlobalSecondaryIndex> globalSecondaryIndices = createGSI(ddbProperties.getGlobalSecondaryIndexes());

        requestBuilder.globalSecondaryIndexes(globalSecondaryIndices);
        requestBuilder.build();
        awsClients.getDynamoDbClient().createTable(requestBuilder.build());
        log.info("Dynamodb Table created: {}", ddbProperties.getTableName());
    }

    private List<GlobalSecondaryIndex> createGSI(List<DynamoDBProperties.GlobalSecondaryIndex> globalSecondaryIndexes) {
        List<GlobalSecondaryIndex> globalSecondaryIndicesAws = new ArrayList<>();
        for (DynamoDBProperties.GlobalSecondaryIndex globalSecondaryIndex : globalSecondaryIndexes) {
            List<KeySchemaElement> keySchemaElementsGsi = new ArrayList<>();
            for (DynamoDBProperties.AttrNameKeyType attrNameKeyType : globalSecondaryIndex.getKeySchema()) {
                keySchemaElementsGsi.add(KeySchemaElement.builder()
                        .keyType(attrNameKeyType.getKeyType())
                        .attributeName(attrNameKeyType.getAttributeName()).build());
            }
            DynamoDBProperties.ProvisionedThroughput provisionedThroughput =
                    globalSecondaryIndex.getProvisionedThroughput();
            ProvisionedThroughput provisionedThroughputAWS = ProvisionedThroughput.builder()
                    .writeCapacityUnits(Long.valueOf(provisionedThroughput.getWriteCapacityUnits()))
                    .readCapacityUnits(Long.valueOf(provisionedThroughput.getReadCapacityUnits()))
                    .build();
            GlobalSecondaryIndex globalSecondaryIndexAws = GlobalSecondaryIndex.builder()
                    .indexName(globalSecondaryIndex.getIndexName())
                    .keySchema(keySchemaElementsGsi)
                    .provisionedThroughput(provisionedThroughputAWS)
                    .projection(Projection.builder()
                            .projectionType(globalSecondaryIndex.getProjection().getProjectionType()).build())
                    .build();

            globalSecondaryIndicesAws.add(globalSecondaryIndexAws);
        }
        return globalSecondaryIndicesAws;
    }

    void createSecrets(SecretManagerProperties secretManagerProperties) {
        CreateSecretRequest createSecretRequest =
                CreateSecretRequest.builder()
                        .secretString(secretManagerProperties.getSecretString())
                        .name(secretManagerProperties.getName()).build();
        SecretsManagerClient secretsManagerClient = awsClients.getSecretsManagerClient();
        secretsManagerClient.createSecret(createSecretRequest);
        log.info("Secrets Manager created {}", secretManagerProperties.getName());
    }
}
