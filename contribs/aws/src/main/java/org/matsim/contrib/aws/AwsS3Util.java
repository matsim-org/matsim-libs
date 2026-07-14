package org.matsim.contrib.aws;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * @author nkuehnel / MOIA
 */
public class AwsS3Util {

    private final static S3Client S3 = S3Client.create();

    public static ResponseInputStream<GetObjectResponse> getS3InputStream(String bucket, String key) {
        GetObjectRequest request = GetObjectRequest
                .builder()
                .bucket(bucket)
                .key(key)
                .build();
        return S3.getObject(request);
    }
}
