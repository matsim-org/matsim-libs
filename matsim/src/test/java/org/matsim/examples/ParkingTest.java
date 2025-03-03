package org.matsim.examples;

import com.google.inject.Singleton;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.VehicleEndsParkingSearch;
import org.matsim.api.core.v01.events.VehicleStartsParkingSearch;
import org.matsim.api.core.v01.events.handler.VehicleEndsParkingSearchEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleStartsParkingSearchEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.parking.*;
import org.matsim.core.network.kernel.ConstantKernelDistance;
import org.matsim.core.network.kernel.DefaultKernelFunction;
import org.matsim.core.network.kernel.KernelDistance;
import org.matsim.core.network.kernel.NetworkKernelFunction;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.PopulationComparison;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

import java.util.List;

import static org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingCapacityInitializer.LINK_OFF_STREET_SPOTS;
import static org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingCapacityInitializer.LINK_ON_STREET_SPOTS;

public class ParkingTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testNoParking() {
		Config config = getConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controller controller = ControllerUtils.createController(scenario);

		TestEventHandler testEventHandler = new TestEventHandler(List.of(), List.of());
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(testEventHandler);
			}
		});

		controller.run();

		checkResult(testEventHandler);
	}

	@Test
	void testParking_constantTime() {
		Config config = getConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controller controller = ControllerUtils.createController(scenario);

		List<VehicleStartsParkingSearch> starts = List.of(
			createStartsParking(22500, "20"),
			createStartsParking(23200, "20"),
			createStartsParking(38240, "1")
		);
		List<VehicleEndsParkingSearch> ends = List.of(
			createEndsParking(22600, "20"),
			createEndsParking(23300, "20"),
			createEndsParking(38340, "1")
		);

		TestEventHandler testEventHandler = new TestEventHandler(starts, ends);
		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				addVehicleHandlerBinding().to(ParkingVehicleHandler.class);
				addParkingSearchTimeCalculatorBinding().toInstance(new ConstantParkingSearchTime(100));
			}
		});
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(testEventHandler);
			}
		});

		controller.run();

		checkResult(testEventHandler);
	}

	@Test
	void testParking_BellocheTime() {
		Config config = getConfig();
		config.controller().setLastIteration(0);
		config.eventsManager().setNumberOfThreads(1);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		for (Link value : scenario.getNetwork().getLinks().values()) {
			value.getAttributes().putAttribute(LINK_ON_STREET_SPOTS, 1);
			value.getAttributes().putAttribute(LINK_OFF_STREET_SPOTS, 0);
		}

		Controller controller = ControllerUtils.createController(scenario);

		//all always two seconds parking search time, because there is only one agents and for each parking request, the Occ is 0, thus the result of the Belloche function is always alpha (=2)
		List<VehicleStartsParkingSearch> starts = List.of(
			createStartsParking(22500, "20"),
			createStartsParking(23102, "20"),
			createStartsParking(38044, "1")
		);
		List<VehicleEndsParkingSearch> ends = List.of(
			createEndsParking(22502, "20"),
			createEndsParking(23104, "20"),
			createEndsParking(38046, "1")
		);

		TestEventHandler testEventHandler = new TestEventHandler(starts, ends);

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				addMobsimScopeEventHandlerBinding().to(ParkingOccupancyObserver.class);
				addVehicleHandlerBinding().to(ParkingVehicleHandler.class);
				bind(ParkingOccupancyObservingSearchTimeCalculator.class).in(Singleton.class);
				addParkingSearchTimeCalculatorBinding().to(ParkingOccupancyObservingSearchTimeCalculator.class);
			}
		});
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ParkingOccupancyObserver.class).in(Singleton.class);
				bind(ParkingCapacityInitializer.class).to(ZeroParkingCapacityInitializer.class);
				bind(NetworkKernelFunction.class).to(DefaultKernelFunction.class);
				bind(KernelDistance.class).toInstance(new ConstantKernelDistance(100));
				bind(ParkingSearchTimeFunction.class).toInstance(new BellochePenaltyFunction(2, -6));

				addControlerListenerBinding().to(ParkingOccupancyObserver.class);
				addEventHandlerBinding().toInstance(testEventHandler);
			}
		});

		controller.run();

		checkResult(testEventHandler);
	}

	private void checkResult(TestEventHandler testEventHandler) {
		String eventsFile = "output_events.xml.gz";
		String populationFile = "output_plans.xml.gz";

		ComparisonResult comparisonResult = EventsUtils.compareEventsFiles(utils.getInputDirectory() + eventsFile, utils.getOutputDirectory() + eventsFile);
		Assertions.assertEquals(ComparisonResult.FILES_ARE_EQUAL, comparisonResult);

		PopulationComparison.Result result = PopulationUtils.comparePopulations(utils.getInputDirectory() + populationFile, utils.getOutputDirectory() + populationFile);
		Assertions.assertEquals(PopulationComparison.Result.equal, result);

		testEventHandler.checkAllEventsProcessed();
	}

	private Config getConfig() {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);
		config.replanning().clearStrategySettings();
		return config;
	}

	private static class TestEventHandler implements VehicleStartsParkingSearchEventHandler, VehicleEndsParkingSearchEventHandler {
		private final List<VehicleStartsParkingSearch> expectedStarts;
		private final List<VehicleEndsParkingSearch> expectedEnds;

		private int startCounter = 0;
		private int endCounter = 0;

		public TestEventHandler(List<VehicleStartsParkingSearch> expectedStarts, List<VehicleEndsParkingSearch> expectedEnds) {
			this.expectedStarts = expectedStarts;
			this.expectedEnds = expectedEnds;
		}

		@Override
		public void handleEvent(VehicleStartsParkingSearch event) {
			Assertions.assertEquals(expectedStarts.get(startCounter++), event);
		}

		@Override
		public void handleEvent(VehicleEndsParkingSearch event) {
			Assertions.assertEquals(expectedEnds.get(endCounter++), event);
		}

		public void checkAllEventsProcessed() {
			Assertions.assertEquals(expectedStarts.size(), startCounter);
			Assertions.assertEquals(expectedEnds.size(), endCounter);
		}
	}

	private static VehicleStartsParkingSearch createStartsParking(double time, String linkId) {
		return new VehicleStartsParkingSearch(time, Id.createPersonId("1"), Id.createLinkId(linkId), Id.createVehicleId("1"), "car");
	}

	private static VehicleEndsParkingSearch createEndsParking(double time, String linkId) {
		return new VehicleEndsParkingSearch(time, Id.createPersonId("1"), Id.createLinkId(linkId), Id.createVehicleId("1"), "car");
	}
}
