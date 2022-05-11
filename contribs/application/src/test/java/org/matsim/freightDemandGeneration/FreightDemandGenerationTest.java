package org.matsim.freightDemandGeneration;

import java.nio.file.Path;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

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
					"--populationFileLocation", populationLocation.toString(),
					"--populationCRS", "WGS84",
					"--network", network,
					"--networkCRS", "WGS84",
					"--networkChangeEvents", "",
					"--inputCarrierCSV", carrierCSVLocation.toString(),
					"--inputDemandCSV", demandCSVLocation.toString(),
					"--populationSample", "0.5",
					"--populationSamplingTo", "1.0",
					"--defaultJspriIterations", "3"
			);
		} catch (Exception ee) {
			Logger.getLogger(this.getClass()).fatal("there was an exception: \n" + ee);
			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}
}
