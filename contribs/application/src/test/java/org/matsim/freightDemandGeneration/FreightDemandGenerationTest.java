package org.matsim.freightDemandGeneration;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;

/**
 * @author Ricardo Ewert Easy test if the FreightDemandGeneration runs without
 *         exceptions.
 *
 */
public class FreightDemandGenerationTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testMainRun() {
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
		} catch (Exception e) {
			LogManager.getLogger(this.getClass()).error("An error occurred while processing run the test: {}", e.getMessage(), e);
			// if one catches an exception, then one needs to explicitly fail the test:
			Assertions.fail();
		}
	}

	@Test
	void testMainRunWithParcelImplementation() {
		DemandGenerationSpecification demandGenerationSpecificationForParcelDelivery = new DemandGenerationSpecificationForParcelDelivery(0.5, 2.0, true);
		try {
			Path output = Path.of(utils.getOutputDirectory());
			Path vehicleFilePath = Path.of(utils.getPackageInputDirectory() + "testVehicleTypes.xml");
			Path carrierCSVLocation = Path.of(utils.getPackageInputDirectory() + "testCarrierCSV_parcels.csv");
			Path demandCSVLocation = Path.of(utils.getPackageInputDirectory() + "testDemandCSV_parcels.csv");
			Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
			String populationLocation = utils.getPackageInputDirectory() + "testPopulation.xml";
			String network = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
			String shapeCategory = "Ortsteil";
			new FreightDemandGeneration(demandGenerationSpecificationForParcelDelivery).execute(
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
				"--populationSample", "1.0",
				"--populationSamplingTo", "1.0",
				"--defaultJspritIterations", "3"
			);
		} catch (Exception e) {
			LogManager.getLogger(this.getClass()).error("An error occurred while processing run the test: {}", e.getMessage(), e);
			Assertions.fail();
		}
	}
}
