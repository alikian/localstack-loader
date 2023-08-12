package io.github.alikian.aws;

import lombok.Data;

import java.util.List;

/**
 * DynamoDB Properties in Cloudformation
 */
@Data
public class DynamoDBProperties {
    List<AttrNameType> attributeDefinitions;
    List<AttrNameKeyType> keySchema;
    ProvisionedThroughput provisionedThroughput;
    String tableName;
    List<GlobalSecondaryIndex> globalSecondaryIndexes;

    /**
     * DynamoDB AttrNameType
     */
    @Data
    static public class AttrNameType{
        String attributeName;
        String AttributeType;
    }

    /**
     * DynamoDB attributeName and key
     */
    @Data
    static public class AttrNameKeyType{
        String attributeName;
        String keyType;
    }

    /**
     * ProvisionedThroughput
     */
    @Data
    static public class ProvisionedThroughput{
        String readCapacityUnits;
        String writeCapacityUnits;
    }

    /**
     * DynamoDB GlobalSecondaryIndex
     */
    @Data
    static public class GlobalSecondaryIndex{
        String indexName;
        List<AttrNameKeyType> keySchema;
        Projection projection;
        ProvisionedThroughput provisionedThroughput;
    }

    /**
     * DynamoDB GSI Projection
     */
    @Data
    static public class Projection{
        String projectionType;
    }
}
