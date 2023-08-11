package com.alikian;

import org.apache.http.util.Asserts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LocalstackManagerTest {
    LocalstackManager localstackManager;

    @BeforeEach
    public void setup() {
        localstackManager = LocalstackManager.builder().buildSimple();
    }

    @Test
    public void testSecretsManagers() {
        Asserts.notNull(localstackManager,"localstackManager");
        Asserts.notNull(localstackManager.getSecretsManagerClient(),"localstackManager");
    }
}
