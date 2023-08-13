package io.github.alikian;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class LocalstackManagerTest {
    LocalstackManager localstackManager;

    @Before
    public void setup() {
        localstackManager = LocalstackManager.builder().buildSimple();
    }

    @Test
    public void testSecretsManagers() {
        Assert.assertNotNull(localstackManager);
        Assert.assertNotNull(localstackManager.getSecretsManagerClient());
        GetSecretValueRequest request=GetSecretValueRequest.builder().secretId("/HeroesSecrets").build();
        GetSecretValueResponse response= localstackManager.getSecretsManagerClient().getSecretValue(request);
        String secretString=response.secretString();
        Assert.assertEquals("{\"key1\":\"value1\",\"key2\":\"value2\"}",secretString,secretString);
    }
}
