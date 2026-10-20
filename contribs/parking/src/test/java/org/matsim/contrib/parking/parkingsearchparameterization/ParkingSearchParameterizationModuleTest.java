package org.matsim.contrib.parking.parkingsearchparameterization;

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
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.kernel.ConstantKernelDistance;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.matsim.contrib.parking.parkingsearchparameterization.ParkingUtils.LINK_ON_STREET_SPOTS;

class ParkingSearchParameterizationModuleTest {
	private static final double ALPHA = 2;

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * The occupancy observer is used for the whole run. With unchanged plans, every iteration must therefore produce the same
	 * parking search times, which requires the observer to receive the events and to be re-initialized before each mobsim.
	 */
	@Test
	void parkingSearchTimesAreReproducedInEveryIteration() {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans2.xml"));
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(1);
		config.replanning().clearStrategySettings();
		config.replanning().addStrategySettings(new ReplanningConfigGroup.StrategySettings()
			.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected)
			.setWeight(1));
		//The plans don't contain access/egress legs.
		config.routing().setAccessEgressConsistencyCheck(RoutingConfigGroup.AccessEgressConsistencyCheck.disable);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.getAttributes().putAttribute(LINK_ON_STREET_SPOTS, 1);
		}

		ParkingSearchTimeRecorder recorder = new ParkingSearchTimeRecorder();
		Controller controller = ControllerUtils.createController(scenario);
		controller.addOverridingModule(new ParkingSearchParameterizationModule(new BellochePenaltyFunction(ALPHA, -10), new ConstantKernelDistance(3000))
			.setParkingCapacityInitializer(ZeroParkingCapacityInitializer.class));
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(recorder);
			}
		});
		controller.run();

		List<Double> firstIteration = recorder.searchTimesPerIteration.get(0);
		assertEquals(6, firstIteration.size());
		// Without occupancy, the search time is alpha. Longer search times show that the observer counts the parked vehicles.
		assertTrue(firstIteration.stream().anyMatch(searchTime -> searchTime > ALPHA));
		assertEquals(firstIteration, recorder.searchTimesPerIteration.get(1));
	}

	private static class ParkingSearchTimeRecorder implements VehicleStartsParkingSearchEventHandler, VehicleEndsParkingSearchEventHandler {
		private final Map<Integer, List<Double>> searchTimesPerIteration = new HashMap<>();
		private final Map<Id<Vehicle>, Double> searchStarts = new HashMap<>();
		private List<Double> searchTimes;

		@Override
		public void reset(int iteration) {
			searchStarts.clear();
			searchTimes = new ArrayList<>();
			searchTimesPerIteration.put(iteration, searchTimes);
		}

		@Override
		public void handleEvent(VehicleStartsParkingSearch event) {
			searchStarts.put(event.getVehicleId(), event.getTime());
		}

		@Override
		public void handleEvent(VehicleEndsParkingSearch event) {
			searchTimes.add(event.getTime() - searchStarts.remove(event.getVehicleId()));
		}
	}
}
