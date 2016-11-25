package playground.dziemke.analysis.srv;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author GabrielT on 08.11.2016.
 */
public class TripAnalyzerSrVV2Test {

	private static final Logger LOG = Logger.getLogger(TripAnalyzerSrVV2Test.class);

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private static final String INPUT_TRIPS_FILENAME = "W2008_Berlin_Weekday_Sample.dat";
	private static final String INPUT_PERSONS_FILENAME = "P2008_Berlin2_Sample.dat";
	private static final String INPUT_NETWORK_FILE = "emptyNetwork.xml";

	@Test
	public void TestAnalyze() {

		final String inputTripsFile = utils.getInputDirectory() + INPUT_TRIPS_FILENAME;
		final String inputPersonsFile = utils.getInputDirectory() + INPUT_PERSONS_FILENAME;
		final String inputNetworkFile = utils.getInputDirectory() + INPUT_NETWORK_FILE;
		LOG.info("Start analyzing");
		TripAnalyzerSrVV2.analyze(inputTripsFile, inputPersonsFile, inputNetworkFile, utils.getOutputDirectory());
		LOG.info("Finished analyzing");

		LOG.info("Start checking checksums");
		testChecksums("correctActivityTypes.txt", "activityTypes.txt", "activityTypes.txt is different");
		testChecksums("correctAverageTripSpeedBeeline.txt", "averageTripSpeedBeeline.txt",
				"averageTripSpeedBeeline.txt is different");
		testChecksums("correctAverageTripSpeedBeelineCumulative.txt", "averageTripSpeedBeelineCumulative.txt",
				"averageTripSpeedBeelineCumulative.txt is different");
		testChecksums("correctDepartureTime.txt", "departureTime.txt", "departureTime.txt is different");
		testChecksums("correctOtherInformation.txt", "otherInformation.txt", "otherInformation.txt is different");
		testChecksums("correctTripDistanceBeeline.txt", "tripDistanceBeeline.txt", "tripDistanceBeeline.txt is different");
		testChecksums("correctTripDistanceBeelineCumulative.txt", "tripDistanceBeelineCumulative.txt",
				"tripDistanceBeelineCumulative.txt is different");
		testChecksums("correctTripDistanceBeelineCumulative.txt", "tripDistanceBeelineCumulative.txt",
				"tripDistanceBeelineCumulative.txt is different");
		testChecksums("correctTripDistanceRouted.txt", "tripDistanceRouted.txt", "tripDistanceRouted.txt is different");
		testChecksums("correctTripDuration.txt", "tripDuration.txt", "tripDuration.txt is different");
		testChecksums("correctTripDurationCumulative.txt", "tripDurationCumulative.txt",
				"tripDurationCumulative.txt is different");
		LOG.info("Finished checking checksums");

	}

	private void testChecksums(String referenceFilename, String compareFilename, String errorOutput) {
		long checksum_ref = CRCChecksum.getCRCFromFile(utils.getInputDirectory() + referenceFilename);
		long checksum_run = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "_wt_carp_dist/" + compareFilename);
		Assert.assertEquals(errorOutput, checksum_ref, checksum_run);
	}

}
