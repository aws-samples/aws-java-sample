package com.amazonaws.samples;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

import java.io.*;
import java.util.UUID;

/**
 * Requires that you created a file ~/.aws/credentials
 */
public class S3TransferExample {

    private static final AmazonS3 s3;
    private static final Region usWest2;
    private static final String bucketName;
    private static final DefaultAWSCredentialsProviderChain credentialProviderChain;

    private static TransferManager tx;
    private static S3TransferExample trxManager;
    private static File fileKey;

    static {
         s3 = new AmazonS3Client();
         usWest2 = Region.getRegion(Regions.US_WEST_2);
         s3.setRegion(usWest2);
         bucketName = "s3-bucket-sync-test-" + UUID.randomUUID();
         credentialProviderChain = new DefaultAWSCredentialsProviderChain();
         fileKey = createSampleFile();
    }

    public static void main(String[] args)
    {
        trxManager = new S3TransferExample();
        //S3Object[] objects = s3Service.listObjects(YourBucketNameString);
        //AmazonS3Client s3 = new AmazonS3Client(myCredentials);
        //for ( S3VersionSummary summary : S3Versions.forPrefix(s3, "my-bucket", "photos/") ) {
        //    System.out.printf("Version '%s' of key '%s'n", summary.getVersionId(), summary.getKey());
        //}

        createATestBucket();

        uploadTmpFileToBucket();

        downloadFileFromBucketToTmpFile();

    }

    public static void uploadTmpFileToBucket()
    {
        tx = new TransferManager(credentialProviderChain.getCredentials());
        fileKey = createSampleFile();

        Upload myUpload = tx.upload(bucketName, fileKey.getName(), fileKey);

        try {
            myUpload.waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (tx != null) tx.shutdownNow();
    }

    public static void downloadFileFromBucketToTmpFile()
    {
        tx = new TransferManager(credentialProviderChain.getCredentials());

        Download myDownload = tx.download(bucketName, fileKey.getName(), new File("aws-java-sdk-copy-test.txt" + UUID.randomUUID()));

        try {
            myDownload.waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (tx != null) tx.shutdownNow();
    }

    public static void createATestBucket()
    {
        boolean bucketMissing = true;
        for (Bucket bucket : s3.listBuckets()) {
            System.out.println("bucket: " + bucket.getName());
            if (bucket.getName().equals(bucketName)) {
                System.out.println("Bucket already exists.");
                bucketMissing = false;
            }
        }
        if (bucketMissing) {
            System.out.println("Creating bucket " + trxManager.bucketName + "\n");
            s3.createBucket(trxManager.bucketName);
        }
    }

    private static File createSampleFile() {
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
        } catch (IOException ioe ) {
            ioe.printStackTrace();
        }

        return tempTestFile;
    }

}
