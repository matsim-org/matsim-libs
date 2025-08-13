package org.matsim.contrib.aws;

import org.matsim.core.config.ReflectiveConfigGroup;
import software.amazon.awssdk.regions.Region;

import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class AwsConfig extends ReflectiveConfigGroup {


    public static final String GROUP_NAME = "aws";

    private static final String S3_BUCKET = "s3bucket";
    private static final String S3_BUCKET_REGION = "s3bucketRegion";
    private static final String OUTPUT_S3_KEY_PREFIX = "outputS3KeyPrefix";

    private String s3Bucket;
    private Region s3BucketRegion;
    private String outputS3KeyPrefix;


    public AwsConfig() {
        super(GROUP_NAME);
    }

    @Override
    public final Map<String, String> getComments() {
        Map<String,String> map = super.getComments();

        map.put(S3_BUCKET, "Name of the S3 bucket where output will be written to.");
        map.put(S3_BUCKET_REGION, "Region of the S3 bucket.");
        map.put(OUTPUT_S3_KEY_PREFIX, "S3 key prefix for each file (i.e., folder name).");

        return map;
    }

    @StringSetter( S3_BUCKET )
    public void setS3Bucket(final String s3Bucket) {
        this.s3Bucket = s3Bucket;
    }

    @StringGetter( S3_BUCKET )
    public String getS3Bucket() {
        return this.s3Bucket;
    }

    @StringSetter( S3_BUCKET_REGION )
    public void setS3BucketRegion(String s3BucketRegion) {
        this.s3BucketRegion = Region.of(s3BucketRegion);
    }

    @StringGetter( S3_BUCKET_REGION )
    public Region getS3BucketRegion() {
        return this.s3BucketRegion;
    }

    @StringSetter( OUTPUT_S3_KEY_PREFIX )
    public void setOutputS3KeyPrefix(final String outputS3KeyPrefix) {
        this.outputS3KeyPrefix = outputS3KeyPrefix;
    }

    @StringGetter( OUTPUT_S3_KEY_PREFIX )
    public String getOutputS3KeyPrefix() {
        return this.outputS3KeyPrefix;
    }
}
