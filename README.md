# AWS SDK for Java Sample Project

A simple Java application illustrating usage of the AWS SDK for Java.

## Requirements

The only requirement of this application is Maven. All other dependencies can
be installed by building the maven package:
    
    mvn package

## Basic Configuration

You need to set up your AWS security credentials before the sample code is able
to connect to AWS. An example is provided as `src/main/resources/AwsCredentials.properties.sample`.
Copy the example to `src/main/resources/AwsCredentials.properties` and edit to
use your access and secret keys:

    accessKey = <your access key id>
    secretKey = <your secret key>

See the [Security Credentials](http://aws.amazon.com/security-credentials) page
for more information on getting your keys.

## Running the S3 sample

This sample application connects to Amazon's [Simple Storage Service (S3)](http://aws.amazon.com/s3),
creates a bucket, and uploads a file to that bucket. The code will generate a
bucket name for you, as well as an example file to upload. All you need to do
is run it:

    mvn clean compile exec:java

When you start making your own buckets, the S3 documentation provides a good overview
of the [restrictions for bucket names](http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html).

## License

This sample application is distributed under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

