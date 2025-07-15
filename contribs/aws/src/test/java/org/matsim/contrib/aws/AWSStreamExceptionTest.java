package org.matsim.contrib.aws;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.testcases.MatsimTestUtils;

import java.net.MalformedURLException;
import java.net.URL;

public class AWSStreamExceptionTest {

    private final static String TEST_S3_URI = "s3://matsim-jobs-input-443370681674/input/mob1156/scenario0/nowel4_network.xml";


    @RegisterExtension
    private MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    void testAWSException() {
        Assertions.assertThrows(RuntimeException.class, AWSStartupHook::registerS3UrlHandler);
    }

    @Test
    void testWithoutRegistration() {
        Assertions.assertThrows(MalformedURLException.class, () -> new MatsimNetworkReader(NetworkUtils.createNetwork()).readURL(new URL(TEST_S3_URI)));
    }
}
