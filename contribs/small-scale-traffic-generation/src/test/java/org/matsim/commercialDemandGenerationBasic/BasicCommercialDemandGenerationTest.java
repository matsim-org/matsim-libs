package org.matsim.commercialDemandGenerationBasic;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

import java.nio.file.Path;

/**
 * @author Ricardo Ewert Easy test if the FreightDemandGeneration runs without
 *         exceptions.
 *
 */
public class BasicCommercialDemandGenerationTest{

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
			new BasicCommercialDemandGeneration().execute(
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

		Config config = ConfigUtils.createConfig();
		Scenario scenarioSolution = ScenarioUtils.createScenario(config);
		Scenario scenarioToCompare = ScenarioUtils.createScenario(config);

		String carriersToCompareLocation = utils.getPackageInputDirectory() + "output_carriers.xml.gz";
		String carriersSolutionLocation = utils.getOutputDirectory() + "output_carriers.xml.gz";
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "testVehicleTypes.xml");

		freightCarriersConfigGroup.setCarriersFile(carriersToCompareLocation);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenarioSolution);
		freightCarriersConfigGroup.setCarriersFile(carriersSolutionLocation);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenarioToCompare);

		Carriers carriersSolution = CarriersUtils.getCarriers(scenarioSolution);
		Carriers carriersToCompare = CarriersUtils.getCarriers(scenarioToCompare);

		for (Carrier thisCarrier : carriersSolution.getCarriers().values()){
			Assertions.assertTrue(carriersToCompare.getCarriers().containsKey(thisCarrier.getId()));
			Carrier inputCarrier = carriersToCompare.getCarriers().get(thisCarrier.getId());
			Assertions.assertEquals(inputCarrier.getSelectedPlan().getScore(), thisCarrier.getSelectedPlan().getScore());
			Assertions.assertEquals(inputCarrier.getSelectedPlan().getScheduledTours().size(), thisCarrier.getSelectedPlan().getScheduledTours().size());
		}

		// compare events
		String expected = utils.getPackageInputDirectory() + "output_events.xml.gz";
		String actual = utils.getOutputDirectory() + "output_events.xml.gz" ;
		ComparisonResult result = EventsUtils.compareEventsFiles( expected, actual );
		Assertions.assertEquals( ComparisonResult.FILES_ARE_EQUAL, result );
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
			new BasicCommercialDemandGeneration(demandGenerationSpecificationForParcelDelivery).execute(
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
