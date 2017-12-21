package com.amazonaws.samples;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

import java.io.*;
import java.util.Iterator;
import java.util.UUID;

/**
 * Requires that you created a file ~/.aws/credentials
 */
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

        deleteTestBuckets();

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

    public static void deleteTestBuckets() {
        deleteEntireBucket(sourceBucket);
        deleteEntireBucket(destinationBucket);
    }

    public static void createTestBuckets(String name) {
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
            s3.createBucket(destinationBucket);
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

    public static void deleteEntireBucket(String bucket_name) {
        System.out.println("Deleting S3 bucket: " + bucket_name);
        final AmazonS3 s3 = new AmazonS3Client();

        try {
            System.out.println(" - removing objects from bucket");
            ObjectListing object_listing = s3.listObjects(bucket_name);
            while (true) {
                for (Iterator<?> iterator = object_listing.getObjectSummaries().iterator(); iterator.hasNext(); ) {
                    S3ObjectSummary summary = (S3ObjectSummary) iterator.next();
                    s3.deleteObject(bucket_name, summary.getKey());
                    summary.getETag();
                }
                if (object_listing.isTruncated()) {
                    object_listing = s3.listNextBatchOfObjects(object_listing);
                } else {
                    break;
                }
            }
            ;

            System.out.println(" - removing versions from bucket");
            VersionListing version_listing = s3.listVersions(new ListVersionsRequest().withBucketName(bucket_name));
            while (true) {
                for (Iterator<?> iterator = version_listing.getVersionSummaries().iterator(); iterator.hasNext(); ) {
                    S3VersionSummary vs = (S3VersionSummary) iterator.next();
                    s3.deleteVersion(bucket_name, vs.getKey(), vs.getVersionId());
                }
                if (version_listing.isTruncated()) {
                    version_listing = s3.listNextBatchOfVersions(version_listing);
                } else {
                    break;
                }
            }
            s3.deleteBucket(bucket_name);
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        }
        System.out.println("Done removing bucket: " + bucket_name);
    }

}
