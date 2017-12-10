package com.amazonaws.samples;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
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

    private static final String s3BucketName = UUID.randomUUID() + "-"
            + DateTimeFormat.forPattern("yyMMdd-hhmmss").print(new DateTime());

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
        System.out.println("Bucket created and configured.");

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
        System.out.println("Queue created.");

        // Send the message.
        SendMessageRequest myMessageRequest = new SendMessageRequest(myQueueUrl, myLongString);
        sqsExtended.sendMessage(myMessageRequest);
        System.out.println("Sent the message.");

        // Receive messages, and then print general information about them.
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(myQueueUrl);
        List<Message> messages = sqsExtended.receiveMessage(receiveMessageRequest).getMessages();

        for (Message message : messages) {
            System.out.println("\nMessage received:");
            System.out.println("  ID: " + message.getMessageId());
            System.out.println("  Receipt handle: " + message.getReceiptHandle());
            System.out.println("  Message body (first 5 characters): " + message.getBody().substring(0, 5));
        }

        // Delete the message, the queue, and the bucket.
        String messageReceiptHandle = messages.get(0).getReceiptHandle();
        sqsExtended.deleteMessage(new DeleteMessageRequest(myQueueUrl, messageReceiptHandle));
        System.out.println("Deleted the message.");

        sqsExtended.deleteQueue(new DeleteQueueRequest(myQueueUrl));
        System.out.println("Deleted the queue.");

        //deleteBucketAndAllContents(s3);
        System.out.println("Deleted the bucket.");

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
