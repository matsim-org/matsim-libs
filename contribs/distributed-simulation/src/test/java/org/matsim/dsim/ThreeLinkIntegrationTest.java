package org.matsim.dsim;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.communication.LocalCommunicator;
import org.matsim.core.communication.NullCommunicator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.DistributedExecution;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ThreeLinkIntegrationTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	@Order(1)
	void qsim() {
		var configPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links-config.xml";
		var config = ConfigUtils.loadConfig(configPath);
		config.controller().setCompressionType(ControllerConfigGroup.CompressionType.none);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setMobsim("qsim");
		var scenario = ScenarioUtils.loadScenario(config);
		var controller = new Controler(scenario);
		controller.run();
	}

	@Test
	@Order(2)
	void oneAgentOneThread() throws URISyntaxException {

		var configPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links-config.xml";
		var config = ConfigUtils.loadConfig(configPath);
		var outputDir = Paths.get(utils.getOutputDirectory());
		config.controller().setCompressionType(ControllerConfigGroup.CompressionType.none);

		// remove the partition information from the network
		var netInputPath = Paths.get(config.getContext().toURI()).getParent().resolve(config.network().getInputFile());
		var net = NetworkUtils.readNetwork(netInputPath.toString());
		for (var link : net.getLinks().values()) {
			link.getAttributes().putAttribute(NetworkDecomposition.PARTITION_ATTR_KEY, 0);
		}
		for (var node : net.getNodes().values()) {
			node.getAttributes().putAttribute(NetworkDecomposition.PARTITION_ATTR_KEY, 0);
		}
		var netPath = outputDir.resolve("single-partition-network.xml").toAbsolutePath();
		NetworkUtils.writeNetwork(net, netPath.toString());

		config.controller().setOutputDirectory(outputDir.resolve("output").toString());
		config.network().setInputFile(netPath.toString());
		var controller = new DistributedController(new NullCommunicator(), config, 1);
		controller.run();

		var expectedEventsPath = outputDir.resolve("..").resolve("qsim").resolve("three-links.output_events.xml");
		var actualEventsPath = utils.getOutputDirectory() + "output/three-links.output_events.xml";

		assertThat(EventsUtils.compareEventsFiles(expectedEventsPath.toString(), actualEventsPath))
			.isEqualTo(ComparisonResult.FILES_ARE_EQUAL);
	}

	/**
	 * This tests the propagation of a vehicle through the network. It was helpful to debug the timing of message passing, including when processes
	 * should synchronize and when simulation times should be updated.
	 */
	@Test
	@Order(2)
	void oneAgentThreeThreads() {

		var configPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links-config.xml";
		var config = ConfigUtils.loadConfig(configPath);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		var controller = new DistributedController(new NullCommunicator(), config, 3);
		controller.run();

		var outputDir = Paths.get(utils.getOutputDirectory());
		var expectedEventsPath = outputDir.resolve("..").resolve("qsim").resolve("three-links.output_events.xml");
		var actualEventsPath = utils.getOutputDirectory() + "three-links.output_events.xml";

		assertThat(EventsUtils.compareEventsFiles(expectedEventsPath.toString(), actualEventsPath))
			.isEqualTo(ComparisonResult.FILES_ARE_EQUAL);
	}

	/**
	 * This tests mainly tests, that storage capacities block following agents. The scenario contains two agents, one with a slow vehicle and one with
	 * a fast vehicle. The one with the slow vehicle blocks the last link for 100s. This test was helpful for debugging, the consuming and releasing
	 * logic of links. Also, this tests that storage capacities are propagated between partitions
	 */
	@Test
	@Order(2)
	void twoAgentsThreeThreads() {

		var configPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links-config.xml";
		var config = ConfigUtils.loadConfig(configPath);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.plans().setInputFile("three-links-plans-2.xml");
		var controller = new DistributedController(new NullCommunicator(), config, 3);
		controller.run();

		var expectedEventsPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links.expected-events-2-plans.xml";
		var actualEventsPath = utils.getOutputDirectory() + "three-links.output_events.xml";

		assertThat(EventsUtils.compareEventsFiles(expectedEventsPath, actualEventsPath))
			.isEqualTo(ComparisonResult.FILES_ARE_EQUAL);
	}

	@Test
	@Order(2)
	@org.matsim.testcases.DisabledOnGitHubWindowsCI
	void oneAgentThreeNodes() {

		var configPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links-config.xml";
		var outputDirectory = utils.getOutputDirectory(); // this also creats the directory

		// start three instances each containing one partition
		var size = 3;
		var comms = LocalCommunicator.create(size);

		DistributedExecution.execute(comms, 120, comm -> {
			Config config = ConfigUtils.loadConfig(configPath);
			config.controller().setOutputDirectory(outputDirectory);
			config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
			DistributedController c = new DistributedController(comm, config, 1);
			c.run();
			try {
				comm.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		var outputDir = Paths.get(utils.getOutputDirectory());
		var expectedEventsPath = outputDir.resolve("..").resolve("qsim").resolve("three-links.output_events.xml");
		var actualEventsPath = utils.getOutputDirectory() + "three-links.output_events.xml";

		assertThat(EventsUtils.compareEventsFiles(expectedEventsPath.toString(), actualEventsPath))
			.isEqualTo(ComparisonResult.FILES_ARE_EQUAL);
	}

	@Test
	@Order(2)
	@org.matsim.testcases.DisabledOnGitHubWindowsCI
	void oneAgentThreeNodesTwoIterations() {
		var configPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links-config.xml";
		var outputDirectory = utils.getOutputDirectory(); // this also creats the directory
		var size = 3;
		var comms = LocalCommunicator.create(size);

		DistributedExecution.execute(comms, 120, comm -> {
			Config local = ConfigUtils.loadConfig(configPath);
			local.dsim().setThreads(1);
			local.controller().setFirstIteration(0);
			local.controller().setLastIteration(1);
			local.controller().setOutputDirectory(outputDirectory);
			local.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

			Scenario scenario = ScenarioUtils.loadScenario(local);

			Controler controler = new Controler(scenario, DistributedContext.create(comm, local));

			controler.run();

			try {
				comm.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			assertEquals(1, scenario.getPopulation().getPersons().size());
			var person = scenario.getPopulation().getPersons().values().iterator().next();

			if (comm.getRank() == 0) {
				assertNotNull(person.getSelectedPlan().getScore());
			} else {
				assertNull(person.getSelectedPlan().getScore());
			}
		});
	}

	@Test
	@Order(2)
	@Timeout(value = 2, unit = TimeUnit.MINUTES)
	void storageCapacityThreeNodes() throws URISyntaxException {

		var configPath = utils.getPackageInputDirectory() + "three-links-scenario/three-links-config.xml";
		var config = ConfigUtils.loadConfig(configPath);
		config.plans().setInputFile("three-links-plans-storage-cap.xml");
		var outputDir = utils.getOutputDirectory();

		// remove the partition information from the network
		var netInputPath = Paths.get(config.getContext().toURI()).getParent().resolve(config.network().getInputFile());
		var net = NetworkUtils.readNetwork(netInputPath.toString());
		for (var link : net.getLinks().values()) {
			link.getAttributes().putAttribute(NetworkDecomposition.PARTITION_ATTR_KEY, 0);
		}
		for (var node : net.getNodes().values()) {
			node.getAttributes().putAttribute(NetworkDecomposition.PARTITION_ATTR_KEY, 0);
		}
		var netPath = Paths.get(outputDir + "single-partition-network.xml").toAbsolutePath().toString();
		NetworkUtils.writeNetwork(net, netPath);

		config.controller().setOutputDirectory(utils.getOutputDirectory() + "output");
		config.network().setInputFile(netPath);
		var controller = new DistributedController(new NullCommunicator(), config, 1);
		controller.run();

	}
}
