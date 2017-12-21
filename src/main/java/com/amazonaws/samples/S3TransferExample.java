package com.amazonaws.samples;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

import java.io.*;
import java.util.Arrays;
import java.util.UUID;

public class S3TransferExample {

    private static final AmazonS3 s3;
    private static final Region usWest2;
    private static final DefaultAWSCredentialsProviderChain credentialProviderChain;
    private static TransferManager tx;

    private static File fileKey;
    private static String sourceBucket;
    private static String destinationBucket;

    static {
        s3 = new AmazonS3Client();
        usWest2 = Region.getRegion(Regions.US_WEST_2);
        s3.setRegion(usWest2);
        credentialProviderChain = new DefaultAWSCredentialsProviderChain();
        fileKey = createTmpFile();
    }

    public static void main(String[] args) {
        createTestBuckets("test-bucket-" + UUID.randomUUID());

        uploadTmpFileToBucket();

        copyBucketToNewLocation();

        //deleteTestBucketsNow();

    }

    private static void copyBucketToNewLocation() {
        System.out.println("Copying bucket " + sourceBucket + " to new bucket " + destinationBucket);
        CopyObjectResult copyObjectResult = s3.copyObject(sourceBucket, fileKey.getName(), destinationBucket, fileKey.getName());
        System.out.println("RESULT charged? " + copyObjectResult.isRequesterCharged());
    }

    public static void uploadTmpFileToBucket() {
        System.out.println("Uploading a file to bucket " + sourceBucket);
        tx = new TransferManager(credentialProviderChain.getCredentials());

        Upload myUpload = tx.upload(sourceBucket, fileKey.getName(), fileKey);

        try {
            myUpload.waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (tx != null) tx.shutdownNow();
    }

    /**
     * Call if you want buckets deleted sooner than their 1-day expiration
     */
    public static void deleteTestBucketsNow() {
        S3Util.deleteEntireBucket(sourceBucket);
        S3Util.deleteEntireBucket(destinationBucket);
    }

    public static void createTestBuckets(String name) {

        BucketLifecycleConfiguration.Rule bucketExpirationRule =
                new BucketLifecycleConfiguration.Rule()
                        .withId("RULE: Delete after 1 day")
                        .withExpirationInDays(1)
                        .withStatus(BucketLifecycleConfiguration.ENABLED.toString());

        BucketLifecycleConfiguration configuration =
                new BucketLifecycleConfiguration()
                        .withRules(Arrays.asList(bucketExpirationRule));

        sourceBucket = name;
        destinationBucket = name + "-dest";

        System.out.println("Creating a test buckets with names: " + sourceBucket + ", " + destinationBucket);
        boolean bucketMissing = true;
        for (Bucket bucket : s3.listBuckets()) {
            System.out.println("bucket: " + bucket.getName());
            if (bucket.getName().equals(name)) {
                System.out.println("Bucket " + name + " already exists.");
                bucketMissing = false;
            }
        }
        if (bucketMissing) {
            System.out.println("Creating bucket " + name + "\n");
            s3.createBucket(sourceBucket);
            s3.setBucketLifecycleConfiguration(sourceBucket, configuration);
            s3.createBucket(destinationBucket);
            s3.setBucketLifecycleConfiguration(destinationBucket, configuration);
        }
    }

    private static File createTmpFile() {
        File tempTestFile = null;
        try {
            tempTestFile = File.createTempFile("aws-java-sdk-copy-test", ".txt");
            tempTestFile.deleteOnExit();

            Writer writer = new OutputStreamWriter(new FileOutputStream(tempTestFile));
            writer.write("abcdefghijklmnopqrstuvwxyz\n");
            writer.write("01234567890112345678901234\n");
            writer.write("!@#$%^&*()-=[]{};':',.<>/?\n");
            writer.write("01234567890112345678901234\n");
            writer.write("abcdefghijklmnopqrstuvwxyz\n");
            writer.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return tempTestFile;
    }



}
