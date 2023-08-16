# localstack-loader

![Build](https://github.com/alikian/localstack-loader/actions/workflows/maven.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.alikian/localstack-loader)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.github.alikian%22)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://raw.githubusercontent.com/alikian/localstack-loader/main/LICENSE)


Localstack Loader

Example:
```java
public class LocalstackManagerTest {
    LocalstackManager localstackManager;

    @BeforeEach
    public void setup() {
        localstackManager =
                LocalstackManager.builder()
                        .withRebuild(true)
                        .withPort(4566)
                        .withImageName("localstack/localstack:2.2.0")
                        .buildSimple();
    }

    @Test
    public void testSecretsManagers() {
        Asserts.notNull(localstackManager,"localstackManager");
        Asserts.notNull(localstackManager.getSecretsManagerClient(),"localstackManager");
    }
}

```