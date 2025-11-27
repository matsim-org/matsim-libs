package org.matsim.contrib.aws;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.FailedFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;

import java.nio.file.Path;

/**
 * @author nkuehnel / MOIA
 */
public final class AwsS3OutputSync implements ShutdownListener {

    private static final Logger log = LogManager.getLogger( AwsS3OutputSync.class );

    private final ControllerConfigGroup controllerConfigGroup;
    private final AwsConfig awsConfig;

    public AwsS3OutputSync(ControllerConfigGroup controllerConfigGroup, AwsConfig awsConfig) {
        this.controllerConfigGroup = controllerConfigGroup;
        this.awsConfig = awsConfig;
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        Path localDirectory = Path.of(controllerConfigGroup.getOutputDirectory());

        S3AsyncClient s3 = S3AsyncClient.builder()
                .region(awsConfig.getS3BucketRegion())
                .credentialsProvider(DefaultCredentialsProvider.create())
                .serviceConfiguration(S3Configuration.builder()
                        .checksumValidationEnabled(true)
                        .build())
                .build();

        S3TransferManager transferManager = S3TransferManager.builder()
                .s3Client(s3)
                .build();

        UploadDirectoryRequest uploadRequest = UploadDirectoryRequest.builder()
                .source(localDirectory)
                .bucket(awsConfig.getS3Bucket())
                .s3Prefix(awsConfig.getOutputS3KeyPrefix())
                .build();

        CompletedDirectoryUpload response = transferManager.uploadDirectory(uploadRequest)
                .completionFuture()
                .join();

        transferManager.close();
        s3.close();

        if(!response.failedTransfers().isEmpty()) {
            log.warn("Failed to upload " + response.failedTransfers() + " files.");
            int i = 0;
            for (FailedFileUpload failedFileUpload : response.failedTransfers()) {
                log.warn(failedFileUpload.toString());
                i++;
                if(i == 5) {
                    log.warn("Omitted remaining files from log...");
                    break;
                }
            }
        }

        log.info("Finished uploading directory");
    }
}
