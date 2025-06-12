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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
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

import static org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingUtils.LINK_OFF_STREET_SPOTS;
import static org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingUtils.LINK_ON_STREET_SPOTS;

public class ParkingTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testNoParking() {
		Config config = getConfig(1);
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
		Config config = getConfig(1);
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
		Config config = getConfig(1);
		config.eventsManager().setNumberOfThreads(4);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		for (Link value : scenario.getNetwork().getLinks().values()) {
			value.getAttributes().putAttribute(LINK_ON_STREET_SPOTS, 1);
			value.getAttributes().putAttribute(LINK_OFF_STREET_SPOTS, 0);
		}

		Controller controller = ControllerUtils.createController(scenario);

		//all always two seconds parking search time, because there is only one agents and for each parking request, the Occ is 0, thus the result of the Belloche function is always alpha (=2)
		/*
		agent 1:
		- 22500 - 22502: parking search time 2*exp(0)=2 (0 parking cars)
		- 23102 - 23909: parking search time 2*exp(6)=807 (1 parking car! - this is because there is an empty route and consequently the vehicle
		leave and parking search start happen in the same time step. Parking observer returns the occupancy of the last time step, where the vehicle
		 was registered as parking.)
		- 38849 - 38851: parking search time 2*exp(0)=2 (0 parking cars)
		 */
		List<VehicleStartsParkingSearch> starts = List.of(
			createStartsParking(22500, "20"),
			createStartsParking(23102, "20"),
			createStartsParking(38849, "1")
		);
		List<VehicleEndsParkingSearch> ends = List.of(
			createEndsParking(22502, "20"),
			createEndsParking(23909, "20"),
			createEndsParking(38851, "1")
		);

		TestEventHandler testEventHandler = new TestEventHandler(starts, ends);

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				addQSimComponentBinding("ParkingOccupancyOberserver").to(ParkingOccupancyObserver.class);
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
				addMobsimListenerBinding().to(ParkingOccupancyObserver.class);
				addEventHandlerBinding().toInstance(testEventHandler);
				addControlerListenerBinding().to(DumpParkingDataAtEnd.class);
			}
		});

		controller.run();

		checkResult(testEventHandler);
	}


	@Test
	void testParking_BellocheTime_MultipleAgents() {
		Config config = getConfig(2);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		for (Link value : scenario.getNetwork().getLinks().values()) {
			value.getAttributes().putAttribute(LINK_ON_STREET_SPOTS, 1);
			value.getAttributes().putAttribute(LINK_OFF_STREET_SPOTS, 0);
		}

		//shift first activity end of person 2 to 6:10, such that it is always behind the first activity end of person 1
		Plan plan = scenario.getPopulation().getPersons().get(Id.createPersonId("2")).getPlans().getFirst();
		((Activity) plan.getPlanElements().get(0)).setEndTime(6 * 3600 + 10);

		Controller controller = ControllerUtils.createController(scenario);

		/*
		agent 1:
		- 22500 - 22502: parking search time 2*exp(0)=2 (0 parking cars)
		- 23102 - 23115: parking search time 2*exp(10*2/11)=13 (2 parking cars, agent 1 & 2!! - see explanation above)
		- 38055 - 38057: parking search time 2*exp(0)=2 (0 parking cars)

		agent 2:
		- 22510 - 22515: parking search time 2*exp(10*1/11)=5 (1 parking car, agent 1)
		- 23115 - 23120: parking search time 2*exp(10*1/11)=5 (1 parking car, agent 1)
		- 38060 - 38065: parking search time 2*exp(10*1/11)=5 (1 parking car, agent 1)
		 */
		List<VehicleStartsParkingSearch> starts = List.of(
			createStartsParking(22500, "20", "1"),
			createStartsParking(22510, "20", "2"),
			createStartsParking(23102, "20", "1"),
			createStartsParking(23115, "20", "2"),
			createStartsParking(38055, "1", "1"),
			createStartsParking(38060, "1", "2")
		);
		List<VehicleEndsParkingSearch> ends = List.of(
			createEndsParking(22502, "20", "1"),
			createEndsParking(22515, "20", "2"),
			createEndsParking(23115, "20", "1"),
			createEndsParking(23120, "20", "2"),
			createEndsParking(38057, "1", "1"),
			createEndsParking(38065, "1", "2")
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
				//changed to 1000
				bind(KernelDistance.class).toInstance(new ConstantKernelDistance(3000));
				//changed to beta=-10
				bind(ParkingSearchTimeFunction.class).toInstance(new BellochePenaltyFunction(2, -10));

				addControlerListenerBinding().to(ParkingOccupancyObserver.class);
				addMobsimListenerBinding().to(ParkingOccupancyObserver.class);
				addEventHandlerBinding().toInstance(testEventHandler);
				addControlerListenerBinding().to(DumpParkingDataAtEnd.class);
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

	private Config getConfig(int suffix) {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans" + suffix + ".xml"));
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

	private static VehicleStartsParkingSearch createStartsParking(double time, String linkId, String personId) {
		return new VehicleStartsParkingSearch(time, Id.createPersonId(personId), Id.createLinkId(linkId), Id.createVehicleId(personId), "car");
	}

	private static VehicleEndsParkingSearch createEndsParking(double time, String linkId, String personId) {
		return new VehicleEndsParkingSearch(time, Id.createPersonId(personId), Id.createLinkId(linkId), Id.createVehicleId(personId), "car");
	}

	private static VehicleStartsParkingSearch createStartsParking(double time, String linkId) {
		return createStartsParking(time, linkId, "1");
	}

	private static VehicleEndsParkingSearch createEndsParking(double time, String linkId) {
		return createEndsParking(time, linkId, "1");
	}
}
