# AWS SDK for Java Sample Project

A simple Java application illustrating usage of the AWS SDK for Java.

## Requirements

You have a choice of Maven or Gradle

The only requirement of this application is Maven. All other dependencies can
be installed by building the maven package:
    
    mvn package

Otherwise with gradle you can run the gradlew wrapper and all dependencies will be installed:

    gradlew build

## Basic Configuration

You need to set up your AWS security credentials before the sample code is able
to connect to AWS. You can do this by creating a file named "credentials" at ~/.aws/ 
(C:\Users\USER_NAME\.aws\ for Windows users) and saving the following lines in the file:

    [default]
    aws_access_key_id = <your access key id>
    aws_secret_access_key = <your secret key>

See the [Security Credentials](http://aws.amazon.com/security-credentials) page
for more information on getting your keys.

## Running the S3 sample

### Prerequisites
You will need to go to [IAM policies page](https://console.aws.amazon.com/iam/home?#policies), search for the String "S3,"
and "Attach" the "AmazonS3FullAccess" policy to the user whose credentials exist in 
your `~/.aws/credentials` file. Otherwise, you will likely get a `AmazonServiceException`/`Access Denied`/`403` error.

This sample application connects to Amazon's [Simple Storage Service (S3)](http://aws.amazon.com/s3),
creates a bucket, and uploads a file to that bucket. The code will generate a
bucket name for you, as well as an example file to upload. All you need to do
is run it.

Maven:

    mvn clean compile exec:java

Gradle:

    gradlew clean build run


When you start making your own buckets, the S3 documentation provides a good overview
of the [restrictions for bucket names](http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html).

## License

This sample application is distributed under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

