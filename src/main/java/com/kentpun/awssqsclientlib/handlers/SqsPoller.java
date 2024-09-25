package com.kentpun.awssqsclientlib.handlers;

import com.kentpun.awssqsclientlib.annotations.SqsListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.lang.reflect.Method;
import java.util.List;

@Component
public class SqsPoller {
    private SqsClientFactory sqsClientFactory;
    private ApplicationContext applicationContext;

    @Autowired
    public SqsPoller(SqsClientFactory sqsClientFactory, ApplicationContext applicationContext){
        this.sqsClientFactory = sqsClientFactory;
        this.applicationContext = applicationContext;
        startPolling();
    }

    @Async
    public void startPolling() {
        applicationContext.getBeansWithAnnotation(SqsListener.class).values().forEach(bean -> {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(SqsListener.class)) {
                    SqsListener annotation = method.getAnnotation(SqsListener.class);
                    String queueUrl = getQueueUrl(
                            annotation.queueUrl(),
                            annotation.queueName(),
                            annotation.region(),
                            annotation.accountId());
                    Runnable runnable = createPollingRunnable(
                            queueUrl,
                            annotation.region(),
                            annotation.receiveWaitTimeSeconds(),
                            method,
                            bean);
                    new Thread(runnable).start(); // Start the polling in a new thread
                }
            }
        });
    }

    private String getQueueUrl(String queueUrl, String queueName, String region, String accountId) {
        if (!queueUrl.isEmpty()) {
            return queueUrl;
        }
        // If queueUrl is not provided, construct it from the queueName and region
        // You can customize this logic based on your naming conventions
        return String.format("https://%s.amazonaws.com/%s/%s", region, accountId, queueName);
    }

    private Runnable createPollingRunnable(String queueUrl, String region, int receiveWaitTimeSeconds, Method method, Object bean) {
        return () -> {
            SqsClient sqsClient = sqsClientFactory.createSqsClient(region); // Create SqsClient with region
            while (true) {
                ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .waitTimeSeconds(20) // Long polling
                        .maxNumberOfMessages(10)
                        .build();

                List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();
                for (Message message : messages) {
                    try {
                        method.invoke(bean);
                        // Optionally, delete the message after processing
                        // sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());
                    } catch (Exception e) {
                        e.printStackTrace(); // Handle method invocation exceptions
                    }
                }
            }
        };
    }

}
