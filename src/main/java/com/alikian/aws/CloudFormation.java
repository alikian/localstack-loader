package com.alikian.aws;

import lombok.Data;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Data
public class CloudFormation {
    private String aWSTemplateFormatVersion;
    private Map<String, Object> resources;

}
