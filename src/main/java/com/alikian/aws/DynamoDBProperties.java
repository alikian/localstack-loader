package com.alikian.aws;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DynamoDBProperties {
    List<AttrNameType> attributeDefinitions;
    List<AttrNameKeyType> keySchema;
    ProvisionedThroughput provisionedThroughput;
    String tableName;
    List<GlobalSecondaryIndex> globalSecondaryIndexes;
    @Data
    static public class AttrNameType{
        String attributeName;
        String AttributeType;
    }
    @Data
    static public class AttrNameKeyType{
        String attributeName;
        String keyType;
    }
    @Data
    static public class ProvisionedThroughput{
        String readCapacityUnits;
        String writeCapacityUnits;
    }
    @Data
    static public class GlobalSecondaryIndex{
        String indexName;
        List<AttrNameKeyType> keySchema;
        Projection projection;
        ProvisionedThroughput provisionedThroughput;
    }
    @Data
    static public class Projection{
        String projectionType;
    }
}
