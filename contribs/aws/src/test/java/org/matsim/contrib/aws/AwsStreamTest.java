package org.matsim.contrib.aws;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.TimeDependentNetwork;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.net.URL;

/**
 * @author nkuehnel / MOIA
 */
public class AwsStreamTest {

    private final static String NETWORK_TEST_S3_URI = "s3://../network.xml.gz";
    private final static String PLANS_TEST_S3_URI = "s3://../plans.xml.gz";
    private final static String VEHICLE_TEST_S3_URI = "s3://../vehicles.xml.gz";
    private final static String TRANSIT_TEST_S3_URI = "s3://../schedule.xml.gz";
    private final static String CHANGE_EVENTS_TEST_S3_URI = "s3://../nces.xml.gz";
    private final static String TEST_S3_BUCKET = "bucket";
    private final static String TEST_S3_KEY = "key/key/key/network.xml.gz";

    @RegisterExtension
    private MatsimTestUtils utils = new MatsimTestUtils();


    @Test
    @Disabled
    /**
     * Run with valid credentials and existing S3 URI only
     */
    void testWithRegistrationURL() {
        Assertions.assertDoesNotThrow(AwsStartupHook::registerS3UrlHandler);

        Network network = NetworkUtils.createNetwork();
        Assertions.assertDoesNotThrow(() -> new MatsimNetworkReader(network).readURL(new URL(NETWORK_TEST_S3_URI)));
        Assertions.assertFalse(network.getLinks().isEmpty());
    }

    @Test
    @Disabled
    /**
     * Run with valid credentials and existing S3 URI only
     */
    void testWithRegistrationURLConfig() {
        Assertions.assertDoesNotThrow(AwsStartupHook::registerS3UrlHandler);

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(NETWORK_TEST_S3_URI);

        URL networkUrl = config.network().getInputFileURL(config.getContext());
        String inputCRS = config.network().getInputCRS();

        Network network = NetworkUtils.createNetwork();
        MatsimNetworkReader reader =
                new MatsimNetworkReader(
                        inputCRS,
                        config.global().getCoordinateSystem(),
                        network);
        reader.parse(networkUrl);
        Assertions.assertFalse(network.getLinks().isEmpty());
    }

    @Test
    @Disabled
    /**
     * Run with valid credentials and existing S3 URI only
     */
    void testWithRegistrationDirectStream() {
        Assertions.assertDoesNotThrow(AwsStartupHook::registerS3UrlHandler);

        Network network = NetworkUtils.createNetwork();
        ResponseInputStream<GetObjectResponse> s3InputStream = AwsS3Util.getS3InputStream(TEST_S3_BUCKET, TEST_S3_KEY);
        new MatsimNetworkReader(network).parse(s3InputStream);
        Assertions.assertFalse(network.getLinks().isEmpty());
    }


    @Test
    @Disabled
    /**
     * Run with valid credentials and existing S3 URI only
     */
    void testWithRegistrationFullScenario() {
        Assertions.assertDoesNotThrow(AwsStartupHook::registerS3UrlHandler);

        Config config = ConfigUtils.createConfig();

        config.transit().setUseTransit(true);
        config.transit().setTransitScheduleFile(TRANSIT_TEST_S3_URI);

        config.plans().setInputFile(PLANS_TEST_S3_URI);

        config.vehicles().setVehiclesFile(VEHICLE_TEST_S3_URI);

        config.network().setInputFile(NETWORK_TEST_S3_URI);
        config.network().setTimeVariantNetwork(true);
        config.network().setChangeEventsInputFile(CHANGE_EVENTS_TEST_S3_URI);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Assertions.assertFalse(scenario.getTransitSchedule().getFacilities().isEmpty());
        Assertions.assertFalse(scenario.getPopulation().getPersons().isEmpty());
        Assertions.assertFalse(scenario.getNetwork().getLinks().isEmpty());
        Assertions.assertFalse(((TimeDependentNetwork) scenario.getNetwork()).getNetworkChangeEvents().isEmpty());
    }
}
