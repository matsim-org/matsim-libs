package org.matsim.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingVehicleHandler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.PopulationComparison;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

public class ParkingTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testParking() {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controller controller = ControllerUtils.createController(scenario);
		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				addVehicleHandlerBinding().to(ParkingVehicleHandler.class);
			}
		});

		controller.run();

		String eventsFile = "output_events.xml.gz";
		String populationFile = "output_plans.xml.gz";

		ComparisonResult comparisonResult = EventsUtils.compareEventsFiles(utils.getInputDirectory() + eventsFile, utils.getOutputDirectory() + eventsFile);
		Assertions.assertEquals(ComparisonResult.FILES_ARE_EQUAL, comparisonResult);

		PopulationComparison.Result result = PopulationUtils.comparePopulations(utils.getInputDirectory() + populationFile, utils.getOutputDirectory() + populationFile);
		Assertions.assertEquals(PopulationComparison.Result.equal, result);
	}
}
