package com.amazonaws.samples;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.*;

public class SQSExtendedClientExample {

    private static final Logger log = Logger.getLogger(SQSExtendedClientExample.class.getName());

    private static final String s3BucketName = UUID.randomUUID() + "-" + DateTimeFormat.forPattern("yyMMdd-hhmmss").print(new DateTime());

    public static void main(String[] args) {

        AWSCredentials credentials = null;

        try {
            credentials = new ProfileCredentialsProvider("default").getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the AWS credentials from the expected AWS credential profiles file. "
                            + "Make sure that your credentials file is at the correct "
                            + "location (/home/$USER/.aws/credentials) and is in a valid format.", e);
        }

        AmazonS3 s3 = AmazonS3ClientBuilder
                .standard()
                .withRegion(Regions.US_WEST_2)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();

        // Set the Amazon S3 bucket name, and set a lifecycle rule on the bucket to
        // permanently delete objects a certain number of days after
        // each object's creation date.
        // Then create the bucket, and enable message objects to be stored in the bucket.
        BucketLifecycleConfiguration.Rule expirationRule = new BucketLifecycleConfiguration.Rule();
        expirationRule.withExpirationInDays(14).withStatus("Enabled");
        BucketLifecycleConfiguration lifecycleConfig = new BucketLifecycleConfiguration().withRules(expirationRule);

        s3.createBucket(s3BucketName);
        s3.setBucketLifecycleConfiguration(s3BucketName, lifecycleConfig);
        log.info("Bucket created and configured.");

        // Set the SQS extended client configuration with large payload support enabled.
        ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration()
                .withLargePayloadSupportEnabled(s3, s3BucketName);

        AmazonSQS sqsExtended = new AmazonSQSExtendedClient(new AmazonSQSClient(credentials), extendedClientConfig);
        Region sqsRegion = Region.getRegion(Regions.US_WEST_2);
        sqsExtended.setRegion(sqsRegion);

        // Create a long string of characters for the message object to be stored in the bucket.
        int stringLength = 300000;
        char[] chars = new char[stringLength];
        Arrays.fill(chars, 'x');
        String myLongString = new String(chars);

        // Create a message queue for this example.
        String QueueName = "QueueName" + UUID.randomUUID().toString();
        CreateQueueRequest createQueueRequest = new CreateQueueRequest(QueueName);
        String myQueueUrl = sqsExtended.createQueue(createQueueRequest).getQueueUrl();
        log.info("Queue created.");

        // Send the message.
        SendMessageRequest myMessageRequest = new SendMessageRequest(myQueueUrl, myLongString);
        sqsExtended.sendMessage(myMessageRequest);
        log.info("Sent the message.");

        // Receive messages, and then print general information about them.
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(myQueueUrl);
        List<Message> messages = sqsExtended.receiveMessage(receiveMessageRequest).getMessages();

        for (Message message : messages) {
            log.info("\nMessage received:");
            log.info("  ID: " + message.getMessageId());
            log.info("  Receipt handle: " + message.getReceiptHandle());
            log.info("  Message body (first 5 characters): " + message.getBody().substring(0, 5));
        }

        // Delete the message, the queue, and the bucket.
        String messageReceiptHandle = messages.get(0).getReceiptHandle();
        sqsExtended.deleteMessage(new DeleteMessageRequest(myQueueUrl, messageReceiptHandle));
        log.info("Deleted the message.");

        sqsExtended.deleteQueue(new DeleteQueueRequest(myQueueUrl));
        log.info("Deleted the queue.");

        S3Util.deleteEntireBucket(s3BucketName);
        log.info("Deleted the bucket.");

    }

}
