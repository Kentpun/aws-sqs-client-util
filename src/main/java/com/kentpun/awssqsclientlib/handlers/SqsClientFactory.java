package com.kentpun.awssqsclientlib.handlers;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Component
public class SqsClientFactory {

    public SqsClient createSqsClient(String region) {
        return SqsClient.builder()
                .region(Region.of(region))
                .build();
    }


}