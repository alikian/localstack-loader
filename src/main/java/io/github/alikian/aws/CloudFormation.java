package io.github.alikian.aws;

import lombok.Data;
import java.util.Map;

/**
 * Cloudformation Template
 */
@Data
public class CloudFormation {
    private String aWSTemplateFormatVersion;
    private Map<String, Object> resources;

}
