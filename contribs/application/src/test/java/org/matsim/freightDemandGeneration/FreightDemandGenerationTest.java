package org.matsim.freightDemandGeneration;

import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;

/**
 * @author Ricardo Ewert Easy test if the FreightDemandGeneration runs without
 *         exceptions.
 *
 */
public class FreightDemandGenerationTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testMain() {
		try {
			Path output = Path.of(utils.getOutputDirectory());
			Path vehicleFilePath = Path.of(utils.getPackageInputDirectory() + "testVehicleTypes.xml");
			Path carrierCSVLocation = Path.of(utils.getPackageInputDirectory() + "testCarrierCSV.csv");
			Path demandCSVLocation = Path.of(utils.getPackageInputDirectory() + "testDemandCSV.csv");
			Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
			String populationLocation = utils.getPackageInputDirectory() + "testPopulation.xml";
			String network = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
			String shapeCategory = "Ortsteil";
			new FreightDemandGeneration().execute(
					"--output", output.toString(),
					"--carrierOption", "createCarriersFromCSV",
					"--demandOption", "createDemandFromCSVAndUsePopulation",
					"--populationOption", "usePopulationInShape",
					"--populationSamplingOption", "createMoreLocations",
					"--VRPSolutionsOption", "runJspritAndMATSim",
					"--combineSimilarJobs", "false",
					"--carrierFileLocation", "",
					"--carrierVehicleFileLocation", vehicleFilePath.toString(),
					"--shapeFileLocation", shapeFilePath.toString(),
					"--shapeCRS", "WGS84",
					"--populationFileLocation", populationLocation,
					"--populationCRS", "WGS84",
					"--network", network,
					"--networkCRS", "WGS84",
					"--networkChangeEvents", "",
					"--shapeCategory", shapeCategory,
					"--inputCarrierCSV", carrierCSVLocation.toString(),
					"--inputDemandCSV", demandCSVLocation.toString(),
					"--populationSample", "0.5",
					"--populationSamplingTo", "1.0",
					"--defaultJspritIterations", "3"
			);
		} catch (Exception ee) {
			LogManager.getLogger(this.getClass()).fatal("there was an exception: \n" + ee);
			ee.printStackTrace();
			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}
}
