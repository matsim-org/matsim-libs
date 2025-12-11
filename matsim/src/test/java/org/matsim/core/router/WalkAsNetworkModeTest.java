package org.matsim.core.router;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.Set;

public class WalkAsNetworkModeTest {
	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * This test runs a scenario with walk as network mode and accessEgressModeToLink. There are two persons:
	 * - one with only car legs
	 * - one with only walk legs
	 * <p>
	 * Car trips have the resulting legs: non_network_walk - walk - car - walk - non_network_walk
	 * Walk trips have the resulting legs: non_network_walk - walk - non_network_walk
	 */
	@Test
	void test_accessEgressModeToLink() {
		Config c = getConfig(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
		Scenario s = getScenario(c);
		run(s);

		EventsFileComparator.compare(utils.getOutputDirectory() + "output_events.xml.gz", utils.getInputDirectory() + "output_events.xml.gz");
		PopulationUtils.comparePopulations(utils.getOutputDirectory() + "output_plans.xml.gz", utils.getInputDirectory() + "output_plans.xml.gz");
	}

	// !!! Not using @ParameterizedTest because otherwise the test fails on windows (java.io.UncheckedIOException: java.nio.file.FileSystemException:
	// ...\test_accessEgressModeToLinkPlusTimeConstant\logfile.log: The process cannot access the file because it is being used by another process)
	// not sure why this is the case, probably some issue with the test framework, paul dec'25
	@Test
	void test_accessEgressModeToLinkPlusTimeConstant_ok() {
		test_accessEgressModeToLinkPlusTimeConstant(false);
	}

	@Test
	void test_accessEgressModeToLinkPlusTimeConstant_fail() {
		test_accessEgressModeToLinkPlusTimeConstant(true);
	}

	/**
	 * This test runs a scenario with walk as network mode and accessEgressModeToLinkPlusTimeConstant. There are two persons:
	 * - one with only car legs
	 * - one with only walk legs
	 * <p>
	 * Car trips have the resulting legs: non_network_walk - walk - car - walk - non_network_walk
	 * Walk trips have the resulting legs: non_network_walk - walk - non_network_walk
	 * <p>
	 * All non_network_walk legs have a constant travel time of 10s added.
	 */
	void test_accessEgressModeToLinkPlusTimeConstant(boolean fail) {
		Config c = getConfig(RoutingConfigGroup.AccessEgressType.accessEgressModeToLinkPlusTimeConstant);
		Scenario s = getScenario(c);

		if (!fail) {
			s.getNetwork().getLinks().values().forEach(link -> {
				NetworkUtils.setLinkAccessTime(link, TransportMode.walk, 10.0);
				NetworkUtils.setLinkAccessTime(link, TransportMode.car, 10.0);

				NetworkUtils.setLinkEgressTime(link, TransportMode.walk, 10.0);
				NetworkUtils.setLinkEgressTime(link, TransportMode.car, 10.0);
			});
			run(s);

			EventsFileComparator.compare(utils.getOutputDirectory() + "output_events.xml.gz", utils.getInputDirectory() + "output_events.xml.gz");
			PopulationUtils.comparePopulations(utils.getOutputDirectory() + "output_plans.xml.gz", utils.getInputDirectory() + "output_plans.xml.gz");
		} else {
			// if no access/egress times are set, a runtime exception is expected
			Assertions.assertThrows(RuntimeException.class, () -> run(s));
		}
	}

	// Deliberately not using @ParameterizedTest. See above. 
	@Test
	void test_walkConstantTimeToLink_ok() {
		test_walkConstantTimeToLink(false);
	}

	@Test
	void test_walkConstantTimeToLink_fail() {
		test_walkConstantTimeToLink(true);
	}

	/**
	 * This test runs a scenario with walk as network mode and accessEgressModeToLinkPlusTimeConstant. There are two persons:
	 * - one with only car legs
	 * - one with only walk legs
	 * <p>
	 * Car trips have the resulting legs: non_network_walk - walk - car - walk - non_network_walk
	 * Walk trips have the resulting legs: non_network_walk - walk - non_network_walk
	 * <p>
	 * All non_network_walk legs have a constant travel time of 10s.
	 */
	void test_walkConstantTimeToLink(boolean fail) {
		Config c = getConfig(RoutingConfigGroup.AccessEgressType.walkConstantTimeToLink);
		Scenario s = getScenario(c);

		if (!fail) {
			s.getNetwork().getLinks().values().forEach(link -> {
				NetworkUtils.setLinkAccessTime(link, TransportMode.walk, 10.0);
				NetworkUtils.setLinkAccessTime(link, TransportMode.car, 10.0);

				NetworkUtils.setLinkEgressTime(link, TransportMode.walk, 10.0);
				NetworkUtils.setLinkEgressTime(link, TransportMode.car, 10.0);
			});
			run(s);

			EventsFileComparator.compare(utils.getOutputDirectory() + "output_events.xml.gz", utils.getInputDirectory() + "output_events.xml.gz");
			PopulationUtils.comparePopulations(utils.getOutputDirectory() + "output_plans.xml.gz", utils.getInputDirectory() + "output_plans.xml.gz");
		} else {
			// if no access/egress times are set, a runtime exception is expected
			Assertions.assertThrows(RuntimeException.class, () -> run(s));
		}
	}

	private Config getConfig(RoutingConfigGroup.AccessEgressType accessEgressType) {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));

		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(1);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		config.qsim().setMainModes(Set.of(TransportMode.car, TransportMode.walk));
		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
		config.routing().setNetworkModes(Set.of(TransportMode.car, TransportMode.walk));
		config.routing().setAccessEgressType(accessEgressType);
		config.routing().clearTeleportedModeParams();
		config.routing().addTeleportedModeParams(new RoutingConfigGroup.TeleportedModeParams(TransportMode.non_network_walk).setTeleportedModeSpeed(1.4));

		config.scoring().addModeParams(new ScoringConfigGroup.ModeParams(TransportMode.walk).setConstant(0).setMarginalUtilityOfTraveling(0));
		config.scoring().addModeParams(new ScoringConfigGroup.ModeParams(TransportMode.non_network_walk).setConstant(0).setMarginalUtilityOfTraveling(0));

		config.global().setNumberOfThreads(1);
		return config;
	}

	private Scenario getScenario(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);

		scenario.getNetwork().getLinks().values().forEach(link -> {
			NetworkUtils.addAllowedMode(link, TransportMode.walk);
		});

		scenario.getPopulation().getPersons().values().forEach(person -> {
			person.getPlans().forEach(plan -> {
				PopulationUtils.getLegs(plan).forEach(leg -> {
					leg.setRoute(null);
				});
			});
		});

		addPureWalkPerson(scenario);

		scenario.getVehicles().addVehicleType(VehicleUtils.createVehicleType(Id.create("walk", VehicleType.class), TransportMode.walk).setMaximumVelocity(2.0));
		scenario.getVehicles().addVehicleType(VehicleUtils.createVehicleType(Id.create("car", VehicleType.class), TransportMode.car).setMaximumVelocity(20.0));

		return scenario;
	}

	private void run(Scenario scenario) {
		Controller controller = ControllerUtils.createController(scenario);
		controller.run();
	}

	private static void addPureWalkPerson(Scenario scenario) {
		PopulationFactory factory = scenario.getPopulation().getFactory();
		Person person = factory.createPerson(Id.createPersonId("walk_person"));
		Plan plan = factory.createPlan();

		// copies plan from person "1"
		PopulationUtils.copyFromTo(scenario.getPopulation().getPersons().get(Id.createPersonId("1")).getSelectedPlan(), plan);
		for (Leg leg : TripStructureUtils.getLegs(plan)) {
			leg.setMode(TransportMode.walk);
			leg.setRoute(null);
		}

		Activity activity = (Activity) plan.getPlanElements().getFirst();
		activity.setEndTime(6 * 3600 + 10);
		plan.setPerson(person);
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}
}
