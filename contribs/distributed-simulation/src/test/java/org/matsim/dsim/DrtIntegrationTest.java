package org.matsim.dsim;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.optimizer.insertion.parallel.DrtParallelInserterParams;
import org.matsim.contrib.drt.optimizer.insertion.parallel.ParallelRequestInserterModule;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
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

		Config config = ConfigUtils.loadConfig(kelheim);

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controller().setLastIteration(1);
		config.controller().setWriteEventsInterval(1);
		config.controller().setMobsim(ControllerConfigGroup.MobsimType.dsim.name());

		config.routing().setRoutingRandomness(0);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);

		// copy qsim config from scenario into dsimconfig
		config.dsim().setLinkDynamics(config.qsim().getLinkDynamics());
		config.dsim().setTrafficDynamics(config.qsim().getTrafficDynamics());
		config.dsim().setStuckTime(config.qsim().getStuckTime());
		config.dsim().setNetworkModes(new HashSet<>(config.qsim().getMainModes()));
		config.dsim().setStartTime(config.qsim().getStartTime().orElse(0));
		config.dsim().setEndTime(config.qsim().getEndTime().orElse(86400));
		config.dsim().setVehicleBehavior(config.qsim().getVehicleBehavior());
		// use bisect to partition scenario
		config.dsim().setPartitioning(DSimConfigGroup.Partitioning.bisect);

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

		scenario.getConfig().dsim().setThreads(1);
		Controler controler = new Controler(scenario, LocalContext.create(scenario.getConfig()));

		prepareController(controler);

		controler.run();
	}

	@Test
	@Order(2)
	void runMultiThreaded() {

		Scenario scenario = createScenario();

		scenario.getConfig().dsim().setThreads(4);
		Controler controler = new Controler(scenario, LocalContext.create(scenario.getConfig()));

		prepareController(controler);

		controler.run();
	}

	@Test
	@Order(2)
	void runParallelInserter() {

		Scenario scenario = createScenario();

		scenario.getConfig().dsim().setThreads(4);
		Controler controler = new Controler(scenario, LocalContext.create(scenario.getConfig()));

		MultiModeDrtConfigGroup multiModeDrtConfigGroup = MultiModeDrtConfigGroup.get(controler.getConfig());
		DrtConfigGroup drtConfig = multiModeDrtConfigGroup.getModalElements().iterator().next();

		DrtParallelInserterParams inserterParams = new DrtParallelInserterParams();
		inserterParams.setCollectionPeriod(30.0);
		inserterParams.setMaxIterations(5);
		inserterParams.setMaxPartitions(8);
		inserterParams.setInsertionSearchThreadsPerWorker(2);
		inserterParams.setVehiclesPartitioner(DrtParallelInserterParams.VehiclesPartitioner.RoundRobinVehicleEntryPartitioner);
		inserterParams.setRequestsPartitioner(DrtParallelInserterParams.RequestsPartitioner.RoundRobinRequestsPartitioner);

		drtConfig.addParameterSet(inserterParams);

		prepareController(controler);
		controler.addOverridingQSimModule(new ParallelRequestInserterModule(drtConfig));

		controler.run();
	}


	@Test
	@Order(3)
	@Disabled
	void runDistributed() throws IOException, ExecutionException, InterruptedException, TimeoutException {

		int size = 3;
		java.util.List<? extends java.util.concurrent.Future<?>> futures;
		try (var pool = Executors.newFixedThreadPool(size)) {
			var comms = LocalCommunicator.create(size);

			Files.createDirectories(Path.of(utils.getOutputDirectory()));

			futures = comms.stream()
				.map(comm -> pool.submit(() -> {

					Scenario scenario = createScenario();

					scenario.getConfig().dsim().setThreads(2);

					Controler controler = new Controler(scenario, DistributedContext.create(comm, scenario.getConfig()));
					prepareController(controler);

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
}
