package com.alikian.aws;

import lombok.Data;

@Data
public class SecretManagerProperties {
    String name;
    String secretString;
}
