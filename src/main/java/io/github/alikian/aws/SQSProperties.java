package io.github.alikian.aws;

import lombok.Data;

/**
 * SQSProperties for Cloudformation
 */
@Data
public class SQSProperties {
    Boolean contentBasedDeduplication;
    String deduplicationScope;
    Integer delaySeconds;
    boolean fifoQueue;
    String fifoThroughputLimit;
    int kmsDataKeyReusePeriodSeconds;
    String kmsMasterKeyId;
    int maximumMessageSize;
    int messageRetentionPeriod;
    String queueName;
    int receiveMessageWaitTimeSeconds;
    String redriveAllowPolicy;
    String redrivePolicy;
    boolean sqsManagedSseEnabled;
    int visibilityTimeout;
}
