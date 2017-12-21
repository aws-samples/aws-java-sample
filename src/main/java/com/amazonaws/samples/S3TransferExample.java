package com.amazonaws.samples;

import com.amazonaws.services.s3.model.*;
import java.io.*;
import java.util.UUID;

public class S3TransferExample
{
    private static File fileKey1;
    private static File fileKey2;
    private static String sourceBucket;
    private static String destinationBucket;

    public static void main(String[] args) {
        createSourceAndDestinationBuckets("test-bucket-" + UUID.randomUUID());
        fileKey1 = S3TestUtil.createTmpFile();
        S3TestUtil.uploadTmpFileToBucket(sourceBucket, fileKey1);
        fileKey2 = S3TestUtil.createTmpFile();
        S3TestUtil.uploadTmpFileToBucket(sourceBucket, fileKey2);
        copyBucketToNewLocation();
        S3TestUtil.showAllBuckets();
        //deleteTestBucketsNow();
    }

    private static void copyBucketToNewLocation() {
        System.out.println("Copying bucket " + sourceBucket + " to new bucket " + destinationBucket);
        CopyObjectResult copyObjectResult = S3TestUtil.getS3().copyObject(sourceBucket, fileKey1.getName(), destinationBucket, fileKey1.getName());
        System.out.println("RESULT charged? " + copyObjectResult.isRequesterCharged());
    }

    public static void createSourceAndDestinationBuckets(String name)
    {
        sourceBucket = name;
        destinationBucket = name + "-dest";
        if (!S3TestUtil.bucketExists(sourceBucket)) S3TestUtil.createExpiringBucket(sourceBucket);
        if (!S3TestUtil.bucketExists(destinationBucket)) S3TestUtil.createExpiringBucket(destinationBucket);
    }

    public static void deleteTestBucketsNow() {
        S3TestUtil.deleteEntireBucket(sourceBucket);
        S3TestUtil.deleteEntireBucket(destinationBucket);
    }

}
