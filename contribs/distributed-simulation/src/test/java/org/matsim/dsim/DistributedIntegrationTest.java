package org.matsim.dsim;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.communication.LocalCommunicator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.PopulationComparison;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DistributedIntegrationTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	private Config createScenario() {

		URL kelheim = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("kelheim"), "config.xml");

		Config config = ConfigUtils.loadConfig(kelheim);

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controller().setLastIteration(1);
		config.controller().setMobsim(ControllerConfigGroup.MobsimType.dsim.name());
		config.controller().setWriteEventsInterval(1);
		config.controller().setCompressionType(ControllerConfigGroup.CompressionType.none);

		config.routing().setRoutingRandomness(0);

		// Compatibility with many scenarios
		Activities.addScoringParams(config);

		config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);
		config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
		config.qsim().setVehicleBehavior(QSimConfigGroup.VehicleBehavior.teleport);
		config.qsim().setEndTime(36 * 3600);

		// add dsim config
		var dsimConfig = ConfigUtils.addOrGetModule(config, DSimConfigGroup.class);
		dsimConfig.setPartitioning(DSimConfigGroup.Partitioning.bisect);
		dsimConfig.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
		dsimConfig.setStuckTime(30);
		dsimConfig.setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);
		dsimConfig.setVehicleBehavior(QSimConfigGroup.VehicleBehavior.teleport);
		dsimConfig.setNetworkModes(Set.of("car", "freight"));
		dsimConfig.setEndTime(36 * 3600);

		return config;
	}

	private Scenario prepareScenario(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// Need to prepare network for freight
		var carandfreight = Set.of(TransportMode.car, "freight", TransportMode.ride);
		scenario.getNetwork().getLinks().values().parallelStream()
			.filter(l -> l.getAllowedModes().contains(TransportMode.car))
			.forEach(l -> l.setAllowedModes(carandfreight));

		return scenario;
	}

	/**
	 * This test is disabled. The DSim calculates travel times different from the QSim. Therefore events and scores
	 * are not equal and there is no point in comparing it. Keep the test around though, because it is sometimes handy
	 * for comparing with existing features.
	 */
	@Test
	@Order(1)
	@Disabled
	void qsim() {

		Config local = createScenario();

		local.controller().setMobsim(ControllerConfigGroup.MobsimType.qsim.name());

		Scenario scenario = prepareScenario(local);

		Controler controler = new Controler(scenario);

		controler.run();
	}

	@Test
	@Order(2)
	void runLocal() {

		Config local = createScenario();
		local.dsim().setThreads(4);
		Scenario scenario = prepareScenario(local);

		Controler controler = new Controler(scenario, LocalContext.create(local));
		controler.run();
	}

	@Test
	@Order(3)
	void runDistributed() throws ExecutionException, InterruptedException, TimeoutException, IOException {

		int size = 3;
		var comms = LocalCommunicator.create(size);
		Files.createDirectories(Path.of(utils.getOutputDirectory()));

		try (var pool = Executors.newFixedThreadPool(size)) {
			var futures = comms.stream()
				.map(comm -> pool.submit(() -> {

					Config local = createScenario();
					local.dsim().setThreads(2);

					Scenario scenario = prepareScenario(local);

					Controler controler = new Controler(scenario, DistributedContext.create(comm, local));

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

		Path outputPath = Path.of(utils.getOutputDirectory());

		// compare events of distributed and local simulation
		Path expectedEventsPath = outputPath.resolve("..").
			resolve("runLocal").resolve("kelheim-mini.output_events.xml");
		String actualEventsPath = utils.getOutputDirectory() + "kelheim-mini.output_events.xml";
		assertThat(EventsUtils.compareEventsFiles(expectedEventsPath.toString(), actualEventsPath))
			.isEqualTo(ComparisonResult.FILES_ARE_EQUAL);

		// compare populations of distributed simulation and original qsim
		var actualPopulationPath = outputPath.resolve("kelheim-mini.output_plans.xml");
		var expectedPopulationPath = outputPath.resolve("..").resolve("runLocal").resolve("kelheim-mini.output_plans.xml");

		var result = PopulationComparison.compare(
			PopulationUtils.readPopulation(expectedPopulationPath.toString()),
			PopulationUtils.readPopulation(actualPopulationPath.toString()),
			1.
		);

		assertEquals(PopulationComparison.Result.equal, result);
	}
}
