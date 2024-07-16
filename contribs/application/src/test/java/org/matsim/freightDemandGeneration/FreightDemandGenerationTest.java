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
	void testMain() {
		try {
			Path output = Path.of(utils.getOutputDirectory());
			Path vehicleFilePath = Path.of(utils.getPackageInputDirectory() + "DHL_vehicleTypes.xml");
			Path carrierCSVLocation = Path.of(utils.getPackageInputDirectory() + "DHL_CarrierCSV.csv");
			Path demandCSVLocation = Path.of(utils.getPackageInputDirectory() + "DHL_DemandCSV.csv");
			//Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
			//Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "lor_planungsraeume_2021.shp/lor_planungsraeume_2021.shp");
			Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "Liefergebiete_16_manuell/Liefergebiete_16_manuell.shp");
			//String populationLocation = utils.getPackageInputDirectory() + "testPopulation.xml";
			String populationLocation = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.3/input/berlin-v6.3-10pct.plans-initial.xml.gz";
			//String network = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
			String network = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.3/input/berlin-v6.3-network.xml.gz";
			//String shapeCategory = "Ortsteil";
			//String shapeCategory = "BEZIRK";
			//String shapeCategory = "PLR_ID";
			String shapeCategory = "Liefergebi";
			new FreightDemandGeneration().execute(
					"--output", output.toString(),
					"--carrierOption", "createCarriersFromCSV",

					"--demandOption", "createDemandFromCSVAndUsePopulation",
					//"--demandOption", "createDemandFromCSV",

					"--populationOption", "usePopulationInShape",
					//"--populationOption", "useHolePopulation",
					//"--populationOption", "useNoPopulation",

					//"--populationSamplingOption", "createMoreLocations",
					"--populationSamplingOption", "increaseDemandOnLocation",
					//"--populationSamplingOption", "noPopulationSampling",

					//"--VRPSolutionsOption", "runJsprit",
					//"--VRPSolutionsOption","runJspritAndMATSim",
					"--VRPSolutionsOption", "createNoSolutionAndOnlyWriteCarrierFile",

					"--combineSimilarJobs", "false",

					"--carrierFileLocation", "",

					"--carrierVehicleFileLocation", vehicleFilePath.toString(),

					"--shapeFileLocation", shapeFilePath.toString(),
					//"--shapeCRS", "WGS84",
					"--shapeCRS", "EPSG:25832",

					"--populationFileLocation", populationLocation,
					"--populationCRS", "EPSG:25832",
					//"--populationCRS", "WGS84",

					"--network", network,
					//"--networkCRS", "WGS84",
					"--networkCRS", "EPSG:25832",

					"--networkChangeEvents", "",
					"--shapeCategory", shapeCategory,
					"--inputCarrierCSV", carrierCSVLocation.toString(),
					"--inputDemandCSV", demandCSVLocation.toString(),
					"--populationSample", "0.1",
					"--populationSamplingTo", "1.0",
					"--defaultJspritIterations", "3"
			);
		} catch (Exception ee) {
			LogManager.getLogger(this.getClass()).fatal("there was an exception: \n" + ee);
			ee.printStackTrace();
			// if one catches an exception, then one needs to explicitly fail the test:
			Assertions.fail();
		}
	}
}
