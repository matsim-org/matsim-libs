package org.matsim.codeexamples.extensions.freight;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.util.Objects;

/**
 * This test runs the freight example for the retailing Berlin scenario.
 * It checks whether events and carriers fiel are created and equal to the expected ones.
 */
public class RunFreightFoodRetailingBerlinExampleTest {
	private static final Logger log = LogManager.getLogger( RunFreightFoodRetailingBerlinExampleTest.class );
	@RegisterExtension public MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testMain(){
		final String pathToInput = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/freight/foodRetailing_wo_rangeConstraint/input/";

		try{
			final String outputDirectory = utils.getOutputDirectory();
			String[] args = {
					"--config:controller.outputDirectory", outputDirectory,
					"--config:controller.lastIteration", "0",
					"--config:controller.overwriteFiles", "deleteDirectoryIfExists",
					"--config:global.coordinateSystem", "EPSG:31468",
					"--config:network.inputNetworkFile", "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/output-berlinv5.5/berlin-v5.5.3-10pct.output_network.xml.gz",
					"--config:plans.inputPlansFile", "null",
					"--config:freightCarriers.carriersFile", pathToInput + "CarrierLEH_v2_withFleet_Shipment_OneTW_PickupTime_ICEV.xml",
					"--config:freightCarriers.carriersVehicleTypeFile", pathToInput + "vehicleTypesBVWP100_DC_noTax.xml"
			};
			RunFreightFoodRetailingBerlinExample.run(args, false);

//			{ //commented out because the attribute "jspritComputationTime" is not fix and depends on the machine.
//				String expected = utils.getInputDirectory() + "output_carriers.xml.gz";
//				String actual = utils.getOutputDirectory() + "output_carriers.xml.gz";
//				MatsimTestUtils.assertEqualFilesLineByLine( expected, actual );
//			}

			{
				String expected = utils.getInputDirectory() + "output_events.xml.gz";
				String actual = utils.getOutputDirectory() + "output_events.xml.gz";
				MatsimTestUtils.assertEqualEventsFiles( expected, actual );
			}

			{
				File inputAnalysisDir = new File(utils.getInputDirectory(), "analysis");
				for (File expectedFile : Objects.requireNonNull(inputAnalysisDir.listFiles())) {
					log.info("Checking {}", expectedFile.getName());
					File actualFile = new File(utils.getOutputDirectory()+ "analysis", expectedFile.getName());
					MatsimTestUtils.assertEqualFilesLineByLine(expectedFile.getAbsolutePath(), actualFile.getAbsolutePath());
				}
			}



		} catch( Exception ee ){
			log.fatal("there was an exception: \n{}", String.valueOf(ee));
			ee.printStackTrace();

			// if one catches an exception, then one needs to explicitly fail the test:
			Assertions.fail();
		}
	}
}
