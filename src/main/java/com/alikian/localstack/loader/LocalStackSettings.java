package com.alikian.localstack.loader;

import com.alikian.localstack.resources.DynamoDbResource;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class LocalStackSettings extends DockerSettings{
    private Resources resources;
    private String endpoint;
    @Data
    public static class Resources {
        private List<String> sqs;
        private List<String> sns;

        private List<SecretsManagerResource> secretsManager;
        private List<DynamoDbResource> dynamoDb;


        public List<SecretsManagerResource> getSecretsManager() {
            return secretsManager;
        }

        @Data
        public static class SecretsManagerResource {
            private String name;
            private Map<String, String> keyValues;
            private String plaintext;
        }

    }

}
