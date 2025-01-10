package org.matsim.contrib.drt.distributed;

import org.junit.jupiter.api.*;
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
import org.matsim.core.communication.LocalCommunicator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.dsim.Activities;
import org.matsim.dsim.DSimConfigGroup;
import org.matsim.dsim.DistributedSimulationModule;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DrtIntegrationTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	private Scenario createScenario() {

		URL kelheim = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("kelheim"), "config-with-drt.xml");

		DSimConfigGroup dsimConfig = new DSimConfigGroup();
		Config config = ConfigUtils.loadConfig(kelheim, dsimConfig);

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controller().setLastIteration(1);
		config.controller().setWriteEventsInterval(1);
		config.controller().setMobsim(ControllerConfigGroup.MobsimType.dsim.name());

		config.routing().setRoutingRandomness(0);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);

		// copy qsim config from scenario into dsimconfig
		dsimConfig.setLinkDynamics(config.qsim().getLinkDynamics());
		dsimConfig.setTrafficDynamics(config.qsim().getTrafficDynamics());
		dsimConfig.setStuckTime(config.qsim().getStuckTime());
		dsimConfig.setNetworkModes(new HashSet<>(config.qsim().getMainModes()));
		dsimConfig.setStartTime(config.qsim().getStartTime().orElse(0));
		dsimConfig.setEndTime(config.qsim().getEndTime().orElse(86400));
		dsimConfig.setVehicleBehavior(config.qsim().getVehicleBehavior());
		// use bisect to partition scenario
		dsimConfig.setPartitioning(DSimConfigGroup.Partitioning.bisect);

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
	void qsim() {

		Scenario scenario = createScenario();

		scenario.getConfig().controller().setMobsim(ControllerConfigGroup.MobsimType.qsim.name());

		Controler controler = new Controler(scenario);

		prepareController(controler);

		controler.run();
	}

	@Test
	@Order(1)
	void runSingleThread() {

		Scenario scenario = createScenario();

		DistributedSimulationModule module = new DistributedSimulationModule(DSimConfigGroup.ofThreads(1));

		Controler controler = new Controler(scenario, module.getNode());

		prepareController(controler);

		controler.addOverridingModule(module);
		controler.run();

	}

	@Test
	@Order(2)
	void runMultiThreaded() {

		Scenario scenario = createScenario();

		DistributedSimulationModule module = new DistributedSimulationModule(DSimConfigGroup.ofThreads(4));
		Controler controler = new Controler(scenario, module.getNode());

		prepareController(controler);

		controler.addOverridingModule(module);
		controler.run();

	}

	@Test
	@Order(3)
	@Disabled
	void runDistributed() throws IOException, ExecutionException, InterruptedException, TimeoutException {

		int size = 3;
		var pool = Executors.newFixedThreadPool(size);
		var comms = LocalCommunicator.create(size);

		Files.createDirectories(Path.of(utils.getOutputDirectory()));

		var futures = comms.stream()
			.map(comm -> pool.submit(() -> {

				Scenario scenario = createScenario();

				DistributedSimulationModule module = new DistributedSimulationModule(comm, DSimConfigGroup.ofThreads(2));

				Controler controler = new Controler(scenario, module.getNode());
				prepareController(controler);

				controler.addOverridingModule(module);
				controler.run();

				try {
					comm.close();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}))
			.toList();

		for (var f : futures) {
			f.get(2, TimeUnit.MINUTES);
		}

	}
}
