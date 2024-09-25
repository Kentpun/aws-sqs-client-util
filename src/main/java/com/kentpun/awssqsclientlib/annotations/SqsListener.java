package com.kentpun.awssqsclientlib.annotations;

import software.amazon.awssdk.regions.Region;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SqsListener {
    String queueUrl() default "";
    String queueName() default "";
    int receiveWaitTimeSeconds() default 0; // default short polling; if value is greater than 0, long polling is in effect
    String region() default "us-east-1";
    String accountId() default "";
}
