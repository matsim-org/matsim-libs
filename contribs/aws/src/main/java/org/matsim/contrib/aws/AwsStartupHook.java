package org.matsim.contrib.aws;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * @author nkuehnel / MOIA
 */
public class AwsStartupHook {

    private final static Logger logger = LogManager.getLogger(AwsStartupHook.class);

    private static boolean registeredS3UrlHandler = false;

    public static synchronized void registerS3UrlHandler() {
        if (!registeredS3UrlHandler) {
            try {
                S3Client S3 = S3Client.create();
                URL.setURLStreamHandlerFactory(protocol -> {
                    if ("s3".equals(protocol)) {
                        return new S3UrlStreamHandler(S3);
                    }
                    return null;
                });
            } catch (SdkClientException e) {
                logger.warn("Make sure to provide valid AWS credentials in your environment variables:");
                logger.warn("AWS_ACCESS_KEY_ID");
                logger.warn("AWS_SECRET_ACCESS_KEY");
                logger.warn("AWS_DEFAULT_REGION");
                logger.warn("(opt.) AWS_SESSION_TOKEN");
                throw new RuntimeException(e);
            }
            registeredS3UrlHandler = true;
        }
    }

    private static class S3UrlStreamHandler extends URLStreamHandler {
        private final S3Client s3;

        public S3UrlStreamHandler(S3Client s3) { this.s3 = s3; }
        @Override protected URLConnection openConnection(URL url) {
            return new S3URLConnection(url, s3);
        }
    }

    private static class S3URLConnection extends URLConnection {
        private final S3Client s3;
        public S3URLConnection(URL url, S3Client s3) { super(url); this.s3 = s3; }
        @Override public void connect() { /* no-op */ }
        @Override public InputStream getInputStream() {
            ResponseInputStream<GetObjectResponse> resp = s3.getObject(GetObjectRequest.builder()
                    .bucket(url.getHost()).key(url.getPath().substring(1)).build());
            return resp;
        }
    }
}
