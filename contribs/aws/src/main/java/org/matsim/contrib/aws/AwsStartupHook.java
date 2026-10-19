package org.matsim.contrib.aws;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author nkuehnel / MOIA
 * @deprecated S3 URL handling is now automatic via {@link S3URLStreamHandlerProvider} (Java SPI).
 * Simply having the aws contrib on the classpath is sufficient — no explicit registration needed.
 */
@Deprecated
public class AwsStartupHook {

    private final static Logger logger = LogManager.getLogger(AwsStartupHook.class);

    /**
     * @deprecated No longer needed. The S3 URL stream handler is now registered automatically
     * via Java's {@link java.net.spi.URLStreamHandlerProvider} SPI mechanism.
     * Simply having the aws contrib on the classpath is sufficient.
     */
    @Deprecated
    public static synchronized void registerS3UrlHandler() {
        logger.info("S3 URL handling is now automatic via SPI (S3URLStreamHandlerProvider). "
                + "This explicit registration call is no longer necessary and can be removed.");
    }
}