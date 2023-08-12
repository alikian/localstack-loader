package io.github.alikian.aws;

import lombok.Data;

/**
 * Secrets Manager Properties in CloudFormation
 */
@Data
public class SecretManagerProperties {
    String name;
    String secretString;
}
