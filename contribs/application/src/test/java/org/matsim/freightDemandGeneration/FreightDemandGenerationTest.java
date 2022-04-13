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
			String network = "https://raw.githubusercontent.com/matsim-org/matsim/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
			new FreightDemandGeneration().execute(
					"--output", output.toString(),
					"--carrierOption", "createCarriersFromCSV",
					"--demandOption", "createDemandFromCSV",
					"--populationOption", "useNoPopulation",
					"--populationSamplingOption", "noPopulationSampling",
					"--VRPSolutionsOption", "runJspritAndMATSim",
					"--combineSimilarJobs", "false",
					"--carrierFileLocation", "",
					"--carrierVehicleFileLocation", vehicleFilePath.toString(),
//					"--shapeFileLocation", "",
//					"--shapeCRS", "",
//					"--shp", "",
					"--populationFileLocation", "",
					"--network", network,
					"--networkCRS", "",
					"--networkChangeEvents", "",
					"--inputCarrierCSV", carrierCSVLocation.toString(),
					"--inputDemandCSV", demandCSVLocation.toString(),
//					"--populationSample", "",
//					"--populationSamplingTo", "",
					"--defaultJspriIterations", "3"
			);
		} catch (Exception ee) {
			Logger.getLogger(this.getClass()).fatal("there was an exception: \n" + ee);
			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}
}
