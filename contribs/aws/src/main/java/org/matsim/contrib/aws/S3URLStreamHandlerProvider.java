package org.matsim.contrib.aws;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;

/**
 * SPI-based S3 URL stream handler. Automatically discovered via ServiceLoader
 * when the aws contrib is on the classpath. No explicit registration needed.
 *
 * @author nkuehnel / MOIA
 */
public class S3URLStreamHandlerProvider extends URLStreamHandlerProvider {

    private static final Logger logger = LogManager.getLogger(S3URLStreamHandlerProvider.class);

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("s3".equals(protocol)) {
            return new S3URLStreamHandler();
        }
        return null;
    }

    private static class S3URLStreamHandler extends URLStreamHandler {

        private volatile S3Client s3;

        private S3Client getClient() {
            if (s3 == null) {
                synchronized (this) {
                    if (s3 == null) {
                        s3 = S3Client.create();
                        logger.info("Initialized S3 client for s3:// URL handling.");
                    }
                }
            }
            return s3;
        }

        @Override
        protected URLConnection openConnection(URL url) {
            return new S3URLConnection(url, getClient());
        }
    }

    private static class S3URLConnection extends URLConnection {

        private final S3Client s3;

        S3URLConnection(URL url, S3Client s3) {
            super(url);
            this.s3 = s3;
        }

        @Override
        public void connect() {
        }

        @Override
        public InputStream getInputStream() {
            String bucket = url.getHost();
            String key = url.getPath().substring(1);
            ResponseInputStream<GetObjectResponse> response = s3.getObject(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
            return response;
        }
    }
}