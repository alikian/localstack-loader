package com.alikian.localstack.resources;

import lombok.Data;

import java.util.List;

@Data
public class DynamoDbResource {

    private List<AttributeDefinition> attributeDefinitions;
    private String tableName;
    private List<KeySchema> keySchema;
    private ProvisionedThroughput provisionedThroughput;
    private GlobalSecondaryIndex globalSecondaryIndex;

    @Data
    public static class AttributeDefinition {
        private String attributeName;
        private String attributeType;
    }

    @Data
    public static class KeySchema {
        private String attributeName;
        private String keyType;
    }

    @Data
    public static class ProvisionedThroughput {
        private Long readCapacityUnits;
        private Long writeCapacityUnits;
    }

    @Data
    public static class GlobalSecondaryIndex {
        private String indexName;
        private AttributeDefinition attributeDefinition;
        private List<KeySchema> keySchema;
        private ProvisionedThroughput provisionedThroughput;

    }
}
