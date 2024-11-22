package org.matsim.dsim;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpModeLimitedMaxSpeedTravelTimeModule;
import org.matsim.core.communication.Communicator;
import org.matsim.core.communication.LocalCommunicator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DrtIntegrationTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	private Scenario createScenario() {

		URL kelheim = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("kelheim"), "config-with-drt.xml");

		Config config = ConfigUtils.loadConfig(kelheim);

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controller().setLastIteration(2);
		config.controller().setMobsim(ControllerConfigGroup.MobsimType.dsim.name());

		config.routing().setRoutingRandomness(0);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);

		// Compatibility with many scenarios
		Activities.addScoringParams(config);

		MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);

		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.scoring(), config.routing());

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// Need to prepare network for freight
		scenario.getNetwork().getLinks().values().parallelStream()
			.filter(l -> l.getAllowedModes().contains(TransportMode.car))
			.forEach(l -> l.setAllowedModes(Stream.concat(l.getAllowedModes().stream(), Stream.of("freight")).collect(Collectors.toSet())));

		scenario.getPopulation()
			.getFactory()
			.getRouteFactories()
			.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

		return scenario;
	}

	private void prepareController(Controler controler) {

		Config config = controler.getScenario().getConfig();
		MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));

		// Add speed limit to av vehicle
		double maxSpeed = controler.getScenario()
			.getVehicles()
			.getVehicleTypes()
			.get(Id.create("autonomous_vehicle", VehicleType.class))
			.getMaximumVelocity();

		controler.getScenario().getVehicles().getVehicleTypes().values().forEach(vt -> vt.setNetworkMode(TransportMode.car));

		controler.addOverridingModule(
			new DvrpModeLimitedMaxSpeedTravelTimeModule("av", config.qsim().getTimeStepSize(),
				maxSpeed));

	}

	@Test
	@Order(1)
	void runSingleThread() {

		Scenario scenario = createScenario();

		Controler controler = new Controler(scenario);

		prepareController(controler);

		controler.addOverridingModule(new DistributedSimulationModule(1));
		controler.run();

	}

	@Test
	@Order(2)
	@Disabled
	void runMultiThreaded() {

		Scenario scenario = createScenario();

		Controler controler = new Controler(scenario);

		prepareController(controler);

		controler.addOverridingModule(new DistributedSimulationModule(4));
		controler.run();

	}

	@Test
	@Order(3)
	@Disabled
	void runDistributed() {

		try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
			List<Communicator> comms = LocalCommunicator.create(4);
			for (Communicator comm : comms) {
				pool.submit(() -> {
					Controler controler = new Controler(createScenario());

					prepareController(controler);

					controler.addOverridingModule(new DistributedSimulationModule(comm, 2, 1.0));
					controler.run();
				});
			}
		}
	}
}
