package io.github.alikian;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
    }
}
