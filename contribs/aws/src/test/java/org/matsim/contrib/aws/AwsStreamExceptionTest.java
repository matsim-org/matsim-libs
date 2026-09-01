package org.matsim.contrib.aws;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.net.URL;

public class AwsStreamExceptionTest {

    private final static String TEST_S3_URI = "s3://nonexistent-bucket/nonexistent-key.xml";

    @RegisterExtension
    private MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    void testSpiProviderIsRegistered() {
        // With the SPI provider on the classpath, s3:// URLs should be parseable
        Assertions.assertDoesNotThrow(() -> new URL(TEST_S3_URI));
    }

    @Test
    void testInvalidBucketThrows() {
        // Connecting to a non-existent bucket should throw when trying to read
        Assertions.assertThrows(Exception.class, () -> {
            URL url = new URL(TEST_S3_URI);
            url.openStream();
        });
    }
}