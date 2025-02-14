package org.matsim.examples;

import com.google.inject.Singleton;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.parking.*;
import org.matsim.core.network.kernel.ConstantNetworkKernelFunction;
import org.matsim.core.network.kernel.NetworkKernelFunction;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.PopulationComparison;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

import static org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingCapacityInitializer.LINK_OFF_STREET_SPOTS;
import static org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingCapacityInitializer.LINK_ON_STREET_SPOTS;

public class ParkingTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testNoParking() {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controller controller = ControllerUtils.createController(scenario);

		controller.run();

		checkResult();
	}

	@Test
	void testParking_constantTime() {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controller controller = ControllerUtils.createController(scenario);

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				addVehicleHandlerBinding().to(ParkingVehicleHandler.class);
				addParkingSearchTimeCalculatorBinding().toInstance(new ConstantParkingSearchTime(100));
			}
		});

		controller.run();

		checkResult();
	}

	@Test
	void testParking_BellocheTime() {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		for (Link value : scenario.getNetwork().getLinks().values()) {
			value.getAttributes().putAttribute(LINK_ON_STREET_SPOTS, 1);
			value.getAttributes().putAttribute(LINK_OFF_STREET_SPOTS, 0);
		}

		Controller controller = ControllerUtils.createController(scenario);

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ParkingObserver.class).in(Singleton.class);
				bind(ParkingCapacityInitializer.class).to(ZeroParkingCapacityInitializer.class);
				bind(NetworkKernelFunction.class).to(ConstantNetworkKernelFunction.class);
				bind(ParkingSearchTimeFunction.class).toInstance(new BellochePenaltyFunction(0.4, -6));

				addVehicleHandlerBinding().to(ParkingVehicleHandler.class);
				bind(ParkingOccupancyObservingSearchTimeCalculator.class).in(Singleton.class);
				addParkingSearchTimeCalculatorBinding().to(ParkingOccupancyObservingSearchTimeCalculator.class);
			}
		});

		controller.run();

		checkResult();
	}

	private void checkResult() {
		String eventsFile = "output_events.xml.gz";
		String populationFile = "output_plans.xml.gz";

		utils.createInputDirectory();
		utils.copyFileFromOutputToInput(eventsFile);
		utils.copyFileFromOutputToInput(populationFile);

		ComparisonResult comparisonResult = EventsUtils.compareEventsFiles(utils.getInputDirectory() + eventsFile, utils.getOutputDirectory() + eventsFile);
		Assertions.assertEquals(ComparisonResult.FILES_ARE_EQUAL, comparisonResult);

		PopulationComparison.Result result = PopulationUtils.comparePopulations(utils.getInputDirectory() + populationFile, utils.getOutputDirectory() + populationFile);
		Assertions.assertEquals(PopulationComparison.Result.equal, result);
	}


}
