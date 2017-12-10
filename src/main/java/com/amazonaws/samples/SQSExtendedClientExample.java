package com.amazonaws.samples;

import java.util.*;
import java.util.logging.Logger;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;

public class SQSExtendedClientExample {

    static Logger log = Logger.getAnonymousLogger();

    private static final String s3BucketName = UUID.randomUUID() + "-" + DateTimeFormat.forPattern("yyMMdd-hhmmss").print(new DateTime());

    private static void initializeLogger() {
        Properties log4jProperties = new Properties();
        log4jProperties.setProperty("log4j.rootLogger", "ERROR, myConsoleAppender");
        log4jProperties.setProperty("log4j.appender.myConsoleAppender", "org.apache.log4j.ConsoleAppender");
        log4jProperties.setProperty("log4j.appender.myConsoleAppender.layout", "org.apache.log4j.PatternLayout");
        log4jProperties.setProperty("log4j.appender.myConsoleAppender.layout.ConversionPattern", "%-5p %c %x - %m%n");
        PropertyConfigurator.configure(log4jProperties);

        BasicConfigurator.configure();

        log.info("Logger intialized");
    }

    public static void main(String[] args) {

        SQSExtendedClientExample.initializeLogger();

        AWSCredentials credentials = null;

        try {
            credentials = new ProfileCredentialsProvider("default").getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the AWS credentials from the expected AWS credential profiles file. "
                            + "Make sure that your credentials file is at the correct "
                            + "location (/home/$USER/.aws/credentials) and is in a valid format.", e);
        }

        AmazonS3 s3 = new AmazonS3Client(credentials);
        Region s3Region = Region.getRegion(Regions.US_WEST_2);
        s3.setRegion(s3Region);

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

        deleteBucketAndAllContents(s3);
        log.info("Deleted the bucket.");

    }

    private static void deleteBucketAndAllContents(AmazonS3 client) {

        ObjectListing objectListing = client.listObjects(s3BucketName);

        while (true) {
            for (Iterator<?> iterator = objectListing.getObjectSummaries().iterator(); iterator.hasNext(); ) {
                S3ObjectSummary objectSummary = (S3ObjectSummary) iterator.next();
                client.deleteObject(s3BucketName, objectSummary.getKey());
            }

            if (objectListing.isTruncated()) {
                objectListing = client.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        };

        VersionListing list = client.listVersions(new ListVersionsRequest().withBucketName(s3BucketName));

        for (Iterator<?> iterator = list.getVersionSummaries().iterator(); iterator.hasNext(); ) {
            S3VersionSummary s = (S3VersionSummary) iterator.next();
            client.deleteVersion(s3BucketName, s.getKey(), s.getVersionId());
        }

        client.deleteBucket(s3BucketName);

    }

}
