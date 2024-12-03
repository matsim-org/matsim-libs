package org.matsim.dsim;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.communication.LocalCommunicator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

import java.net.URL;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

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

		config.routing().setRoutingRandomness(0);

		// Compatibility with many scenarios
		Activities.addScoringParams(config);

		return config;
	}

	@Test
	@Order(1)
	void qsim() {

		Config local = createScenario();

		local.controller().setMobsim(ControllerConfigGroup.MobsimType.qsim.name());

		Scenario scenario = ScenarioUtils.loadScenario(local);

		// Need to prepare network for freight
		var carandfreight = Set.of(TransportMode.car, "freight", TransportMode.ride);
		scenario.getNetwork().getLinks().values().parallelStream()
			.filter(l -> l.getAllowedModes().contains(TransportMode.car))
			.forEach(l -> l.setAllowedModes(carandfreight));

		Controler controler = new Controler(scenario);

		controler.run();
	}

	@Test
	@Order(2)
	void runLocal() {

		Config local = createScenario();
		Scenario scenario = ScenarioUtils.loadScenario(local);

		// Need to prepare network for freight
		var carandfreight = Set.of(TransportMode.car, "freight", TransportMode.ride);
		scenario.getNetwork().getLinks().values().parallelStream()
			.filter(l -> l.getAllowedModes().contains(TransportMode.car))
			.forEach(l -> l.setAllowedModes(carandfreight));

		Controler controler = new Controler(scenario);

		// TODO: single node module run script / controller is different from multi node setup

		controler.addOverridingModule(new DistributedSimulationModule(4));
		controler.run();

		Path outputPath = Path.of(utils.getOutputDirectory());

		var expectedEventsPath = outputPath.resolve("..").
			resolve("qsim").resolve("kelheim-mini.output_events.xml");
		var actualEventsPath = utils.getOutputDirectory() + "kelheim-mini.output_events.xml";

		assertThat(EventsUtils.compareEventsFiles(expectedEventsPath.toString(), actualEventsPath))
			.isEqualTo(ComparisonResult.FILES_ARE_EQUAL);

	}

	@Test
	@Order(2)
	@Disabled
	void runDistributed() throws ExecutionException, InterruptedException, TimeoutException {

		//Config local = createScenario();
		//Scenario scenario = ScenarioUtils.loadScenario(local);

		// Need to prepare network for freight
		//var carandfreight = Set.of(TransportMode.car, "freight", TransportMode.ride);
		//scenario.getNetwork().getLinks().values().parallelStream()
		//	.filter(l -> l.getAllowedModes().contains(TransportMode.car))
		//	.forEach(l -> l.setAllowedModes(carandfreight));

		var size = 4;
		var pool = Executors.newFixedThreadPool(size);
		var comms = LocalCommunicator.create(size);
		var outputDir = utils.getOutputDirectory();

		var futures = comms.stream()
			.map(comm -> pool.submit(() -> {
				Config config = ConfigUtils.createConfig();
				config.controller().setOutputDirectory(outputDir);
				config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
				config.controller().setLastIteration(2);
				config.controller().setMobsim("dsim");
				Activities.addScoringParams(config);
				DistributedController c = new DistributedController(comm, config, 1, 1);
				c.run();
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


		/*
		try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
			List<Communicator> comms = LocalCommunicator.create(4);
			for (Communicator comm : comms) {
				pool.submit(() -> {
					//Controler controler = new Controler(scenario);
					//controler.addOverridingModule(new DistributedSimulationModule(comm, 2, 1.0));
					//controler.run();
					var distributedController = new DistributedController(comm, local, 1, 1);
					distributedController.run();
				});
			}
		}

		 */

	}
}
