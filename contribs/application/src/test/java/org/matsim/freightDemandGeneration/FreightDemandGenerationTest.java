package org.matsim.freightDemandGeneration;

import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.stat.Frequency;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.util.Random;

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

			//Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
			//Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "Liefergebiete_16_manuell/Liefergebiete_16_manuell.shp");
			//String populationLocation = utils.getPackageInputDirectory() + "testPopulation.xml";

			//Berlin
			Path carrierCSVLocation = Path.of(utils.getPackageInputDirectory() + "DHL_CarrierCSV_Berlin_small.csv");
			Path demandCSVLocation = Path.of(utils.getPackageInputDirectory() + "DHL_DemandCSV_Berlin_small.csv");
			//Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "PLZ_Berlin/PLZ_Berlin.shp");
			Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "PLZ_Gebiete_Berlin/PLZ_Gebiete_Berlin.shp");
			//String populationLocation = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.3/input/berlin-only-v6.3-100pct.plans_NOT-fully-calibrated.xml.gz";
			String populationLocation = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.3/input/berlin-v6.3-1pct.plans.xml.gz";
			String network = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.3/input/berlin-v6.3-network.xml.gz";

			//Lausitz
/*
			String populationLocation = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/lausitz/input/v2024.2/lausitz-v2024.2-100pct.plans-initial.xml.gz";
			String network = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/lausitz/input/v2024.2/lausitz-v2024.2-network.xml.gz";
			Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "PLZ_Gebiete_Görlitz/PLZ_Gebiete_Görlitz.shp");
			Path carrierCSVLocation = Path.of(utils.getPackageInputDirectory() + "DHL_CarrierCSV_Lausitz.csv");
			Path demandCSVLocation = Path.of(utils.getPackageInputDirectory() + "DHL_DemandCSV_Lausitz.csv");
*/

			String shapeCategory = "plz";
			//String shapeCategory = "Liefergebi";
			new FreightDemandGeneration().execute(
					"--output", output.toString(),
					"--carrierOption", "createCarriersFromCSV",

					"--demandOption", "createDemandFromCSVAndUsePopulation",
					//"--demandOption", "createDemandFromCSV",

					"--populationOption", "usePopulationInShape",
					//"--populationOption", "useHolePopulation",
					//"--populationOption", "useNoPopulation",

					//"--populationSamplingOption", "createMoreLocations",
					//"--populationSamplingOption", "increaseDemandOnLocation",
					"--populationSamplingOption", "noPopulationSampling",

					//"--VRPSolutionsOption", "runJsprit",
					//"--VRPSolutionsOption","runJspritAndMATSim",
					"--VRPSolutionsOption", "createNoSolutionAndOnlyWriteCarrierFile",

					"--combineSimilarJobs", "true",

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
					"--populationSample", "1.0",
					"--populationSamplingTo", "1.0",
					"--defaultJspritIterations", "3",
					"--totalDemandGenerationOption","demandForShape",
					"--packagesPerPerson","0.0686", //0.14*0.49
					"--demandDistributionOption","toPersonsByAge",
					"--packagesPerRecipient","1.6"
			);
		} catch (Exception ee) {
			LogManager.getLogger(this.getClass()).fatal("there was an exception: \n" + ee);
			ee.printStackTrace();
			// if one catches an exception, then one needs to explicitly fail the test:
			Assertions.fail();
		}
	}
}
