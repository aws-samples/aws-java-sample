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
import java.util.Arrays;
import java.util.Iterator;

/**
 * Requires that you created a file ~/.aws/credentials
 */
public class S3TestUtil
{
    private static final AmazonS3 s3;
    private static final Region usWest2;
    private static final DefaultAWSCredentialsProviderChain credentialProviderChain;
    private static TransferManager tx;

    static {
        s3 = new AmazonS3Client();
        usWest2 = Region.getRegion(Regions.US_WEST_2);
        s3.setRegion(usWest2);
        credentialProviderChain = new DefaultAWSCredentialsProviderChain();
    }

    public static AmazonS3 getS3() {
        return s3;
    }

    public static void deleteEntireBucket(String bucket_name) {
        System.out.println("Deleting S3 bucket: " + bucket_name);
        try {
            System.out.println(" - removing objects from bucket");
            ObjectListing object_listing = s3.listObjects(bucket_name);
            while (true) {
                for (Iterator<?> iterator = object_listing.getObjectSummaries().iterator(); iterator.hasNext(); ) {
                    S3ObjectSummary summary = (S3ObjectSummary) iterator.next();
                    s3.deleteObject(bucket_name, summary.getKey());
                }
                if (object_listing.isTruncated()) {
                    object_listing = s3.listNextBatchOfObjects(object_listing);
                } else {
                    break;
                }
            }

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

    public static void copyEntireBucket(String src_bucket, String dest_bucket) {
        System.out.println("Copying S3 bucket '" + src_bucket + "' to destination '" + dest_bucket + "' ...");
        try {
            System.out.println(" - copying objects from bucket");
            ObjectListing object_listing = s3.listObjects(src_bucket);
            while (true) {
                for (Iterator<?> iterator = object_listing.getObjectSummaries().iterator(); iterator.hasNext(); ) {
                    S3ObjectSummary summary = (S3ObjectSummary) iterator.next();
                    s3.copyObject(src_bucket, summary.getKey(), dest_bucket, summary.getKey());
                }
                if (object_listing.isTruncated()) {
                    object_listing = s3.listNextBatchOfObjects(object_listing);
                } else {
                    break;
                }
            }
            //System.out.println(" - copying versions from bucket");
            //VersionListing version_listing = s3.listVersions(new ListVersionsRequest().withBucketName(src_bucket));
            //while (true) {
            //    for (Iterator<?> iterator = version_listing.getVersionSummaries().iterator(); iterator.hasNext(); ) {
            //        S3VersionSummary vs = (S3VersionSummary) iterator.next();
            //        String key = vs.getKey();
            //        String ver = vs.getVersionId();
            //        // how??
            //    }
            //    if (version_listing.isTruncated()) {
            //        version_listing = s3.listNextBatchOfVersions(version_listing);
            //    } else {
            //        break;
            //    }
            //}
            //s3.deleteBucket(src_bucket);
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        }
        System.out.println("Done copying bucket '" + src_bucket + "' to new bucket '" + dest_bucket + "'");
    }

    public static File createTmpFile() {
        File tempTestFile = null;
        try {
            tempTestFile = File.createTempFile("aws-java-sdk-copy-test-", ".txt");
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

    public static void createExpiringBucket(String name) {
        BucketLifecycleConfiguration.Rule bucketExpirationRule =
                new BucketLifecycleConfiguration.Rule()
                        .withId("RULE: Delete bucket after 1 day")
                        .withExpirationInDays(1) // expiration date must be midnight GMT and is effectively GMT-8, or around 4pm
                        .withStatus(BucketLifecycleConfiguration.ENABLED.toString());

        BucketLifecycleConfiguration configuration =
                new BucketLifecycleConfiguration()
                        .withRules(Arrays.asList(bucketExpirationRule));

        boolean bucketMissing = !bucketExists(name);

        if (bucketMissing) {
            System.out.println("Creating 10 minute bucket " + name + "\n");
            s3.createBucket(name);
            s3.setBucketLifecycleConfiguration(name, configuration);
        }
    }

    public static boolean bucketExists(String name) {
        for (Bucket bucket : s3.listBuckets()) {
            if (bucket.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static void showAllBuckets() {
        for (Bucket bucket : s3.listBuckets()) {
            System.out.println("bucket: " + bucket.getName());
        }
    }

    public static void uploadTmpFileToBucket(String bucketName, File fileKey) {
        System.out.println("Uploading a file to bucket " + bucketName);
        tx = new TransferManager(credentialProviderChain.getCredentials());

        Upload myUpload = tx.upload(bucketName, fileKey.getName(), fileKey);

        try {
            myUpload.waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (tx != null) tx.shutdownNow();
    }

}
