package com.amazonaws.samples;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
 
public class QuickStart {

    public static void main(String[] args) {
        // Load the credentials file
        AWSCredentialsProvider credentialsProvider
            = new ClasspathPropertiesFileCredentialsProvider();

        // Instantiate an S3 client
        AmazonS3 s3 = new AmazonS3Client(credentialsProvider);

        // Create a bucket
        String accessKey = credentialsProvider.getCredentials().getAWSAccessKeyId();
        String bucketName = "aws-java-sdk-sample-" + accessKey.toLowerCase();
        s3.createBucket(bucketName);
    }

}
