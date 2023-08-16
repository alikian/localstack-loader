package io.github.alikian;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class LocalstackManagerTest {
    LocalstackManager localstackManager;

    @BeforeEach
    public void setup() {
        localstackManager =
                LocalstackManager.builder()
                        .withRebuild(false)
                        .withPort(4566)
                        .withImageName("localstack/localstack:2.2.0")
                        .buildSimple();
    }

    @Test
    public void testSecretsManagers() {
        Assertions.assertNotNull(localstackManager);
        Assertions.assertNotNull(localstackManager.getSecretsManagerClient());
        GetSecretValueRequest request=GetSecretValueRequest.builder().secretId("/HeroesSecrets").build();
        GetSecretValueResponse response= localstackManager.getSecretsManagerClient().getSecretValue(request);
        String secretString=response.secretString();
        Assertions.assertEquals("{\"key1\":\"value1\",\"key2\":\"value2\"}",secretString,secretString);
    }
}
