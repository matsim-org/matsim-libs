package org.matsim.contrib.parking.parkingsearch.facilityBasedParking;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.parking.parkingsearch.ParkingSearchStrategy;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchConfigGroup;
import org.matsim.contrib.parking.parkingsearch.sim.SetupParking;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

public abstract class AbstractParkingTest {
	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	abstract ParkingSearchStrategy getParkingSearchStrategy();

	@Test
	void testParking1() {
		Config config = getConfig(getParkingConfig());
		config.plans().setInputFile("population1.xml");
		run(config);
		validate();
	}

	@Test
	void testParking10() {
		Config config = getConfig(getParkingConfig());
		config.plans().setInputFile("population10.xml");
		run(config);
		validate();
	}

	@Test
	void testParking100() {
		Config config = getConfig(getParkingConfig());
		run(config);
		validate();
	}

	@Test
	void testParking1_onlyFacilityParking() {
		ParkingSearchConfigGroup parkingConfig = getParkingConfig();
		parkingConfig.setCanParkOnlyAtFacilities(true);
		Config config = getConfig(parkingConfig);
		config.plans().setInputFile("population1.xml");
		run(config);
		validate();
	}

	/**
	 * Tests the behaviour of the parking search when only parking at facilities is allowed.
	 * Result: Agents are trying to park at facilities and are searching so long for a parking space until they found one.
	 */
	@Test
	void testParking10_onlyFacilityParking() {
		ParkingSearchConfigGroup parkingConfig = getParkingConfig();
		parkingConfig.setCanParkOnlyAtFacilities(true);
		Config config = getConfig(parkingConfig);
		config.plans().setInputFile("population10.xml");
		run(config);
		validate();
	}

	private ParkingSearchConfigGroup getParkingConfig() {
		ParkingSearchConfigGroup parkingSearchConfigGroup = new ParkingSearchConfigGroup();
		parkingSearchConfigGroup.setParkingSearchStrategy(getParkingSearchStrategy());
		return parkingSearchConfigGroup;
	}

	private Config getConfig(ParkingSearchConfigGroup parkingSearchConfigGroup) {
		Config config = ConfigUtils.loadConfig("parkingsearch/config.xml", parkingSearchConfigGroup);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		return config;
	}

	private void run(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controller controller = ControllerUtils.createController(scenario);
		SetupParking.installParkingModules(controller);
		controller.run();
	}

	private void validate() {
		comparePopulation();
		compareEvents();
		compareCsv("0.parkingStats.csv");
		compareCsv("0.parkingStatsPerTimeSteps.csv");
	}

	private void compareCsv(String fileName) {
		String parkingStatsOut = utils.getOutputDirectory() + "/ITERS/it.0/" + fileName;
		String parkingStatsIn = utils.getInputDirectory() + "/" + fileName;
		MatsimTestUtils.assertEqualFilesLineByLine(parkingStatsIn, parkingStatsOut);
	}

	private void compareEvents() {
		String expectedEventsFile = utils.getInputDirectory() + "/output_events.xml.gz";
		String actualEventsFile = utils.getOutputDirectory() + "/output_events.xml.gz";
		ComparisonResult result = EventsUtils.compareEventsFiles(expectedEventsFile, actualEventsFile);
		Assertions.assertEquals(ComparisonResult.FILES_ARE_EQUAL, result);
	}

	private void comparePopulation() {
		Population expected = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationUtils.readPopulation(expected, utils.getInputDirectory() + "/output_plans.xml.gz");

		Population actual = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationUtils.readPopulation(actual, utils.getOutputDirectory() + "/output_plans.xml.gz");

		for (Id<Person> personId : expected.getPersons().keySet()) {
			double scoreReference = expected.getPersons().get(personId).getSelectedPlan().getScore();
			double scoreCurrent = actual.getPersons().get(personId).getSelectedPlan().getScore();
			Assertions.assertEquals(scoreReference, scoreCurrent, MatsimTestUtils.EPSILON, "Scores of person=" + personId + " are different");
		}
	}
}
