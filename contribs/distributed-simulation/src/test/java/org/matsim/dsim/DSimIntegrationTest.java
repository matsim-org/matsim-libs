package org.matsim.dsim;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.communication.LocalCommunicator;
import org.matsim.core.communication.NullCommunicator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DSimIntegrationTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	@Order(1)
	void runLocal() {

		Config local = createScenario();
		var outputPath = Paths.get(utils.getOutputDirectory());
		local.controller().setOutputDirectory(outputPath.resolve("prerun").toString());
		// do a pre run, because we want to use the same plans for local and distributed.
		// When using the unplanned plans file for the local and the planned one from the local run for the distributed run, we end up with slightly
		// different travel times, due to Double-rounding errors. This would cause varying orders of agent leaving the Activity engine for example.
		// Therefore, conduct a pre run, then do a local run using the output plans file from that run for the local run and for the distributed run,
		// which are both compared at the end of runDistributed.
		var preRun = new DistributedController(new NullCommunicator(), local, 1);
		preRun.run();
		var plans = outputPath.resolve("prerun/kelheim-mini.output_plans.xml").toAbsolutePath();
		local.plans().setInputFile(plans.toString());
		local.controller().setOutputDirectory(outputPath.toString());

		DistributedController controller = new DistributedController(new NullCommunicator(), local, 1);
		controller.run();

		assertThat(Path.of(utils.getOutputDirectory()))
			.isNotEmptyDirectory()
			.isDirectoryContaining("glob:**kelheim-mini.output_events.xml");
	}

	@Test
	@Order(2)
	void runDistributed() throws IOException, InterruptedException, ExecutionException, TimeoutException {

		Path output = Path.of(utils.getOutputDirectory());
		Path plansPath = output.resolve("..").resolve("runLocal/prerun").resolve("kelheim-mini.output_plans.xml").toAbsolutePath();
		Files.createDirectories(output);

		// start three instances each containing one partition
		var size = 3;
		var comms = LocalCommunicator.create(size);
		try (var pool = Executors.newFixedThreadPool(size)) {
			var futures = comms.stream()
				.map(comm -> pool.submit(() -> {
					Config config = createScenario();
					config.plans().setInputFile(plansPath.toString());
					DistributedController c = new DistributedController(comm, config, 2);
					c.run();
					try {
						comm.close();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}))
				.toList();

			for (var f : futures) {
				f.get(1, TimeUnit.MINUTES);
				//f.get();
			}
		}

		Path distOutput = output.resolve("kelheim-mini.output_events.xml");
		Path localOutput = output.resolve("..").resolve("runLocal/kelheim-mini.output_events.xml").toAbsolutePath();
		assertThat(EventsUtils.compareEventsFiles(localOutput.toString(), distOutput.toString()))
			.isEqualTo(ComparisonResult.FILES_ARE_EQUAL);
	}

	private Config createScenario() {

		URL kelheim = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("kelheim"), "config.xml");

		Config config = ConfigUtils.loadConfig(kelheim);

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controller().setMobsim("dsim");
		config.controller().setWriteEventsInterval(1);
		config.controller().setCompressionType(ControllerConfigGroup.CompressionType.none);

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
		// rely on flow capacity factor until we introduce global scaling factor
		config.qsim().setFlowCapFactor(1.);

		// Randomness will lead to different results from the baseline
		config.routing().setRoutingRandomness(0);

		// Compatibility with many scenarios
		Activities.addScoringParams(config);

		return config;
	}
}
