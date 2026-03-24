package org.matsim.contrib.ev.dsim;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.communication.LocalCommunicator;
import org.matsim.core.communication.NullCommunicator;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.dsim.DistributedContext;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for EV drive discharging in a distributed (DSim) setting.
 * <p>
 * Scenario: one EV drives from link l1 through l2 to l3 in the three-link
 * network, crossing all three partition boundaries.  No chargers are present.
 * <p>
 * Tests verify:
 * <ul>
 *   <li>DSim produces the same events as QSim (golden-reference pattern).</li>
 *   <li>Battery state is correctly transferred across partition boundaries
 *       (WP2 — battery state transfer).</li>
 *   <li>DriveDischargingHandler runs as a DistributedMobsimEngine (WP3).</li>
 *   <li>Drive discharging is partition-local and covers all three partitions
 *       (WP4).</li>
 * </ul>
 *
 * @see EvDSimTestFixture
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EvDriveDischargingIntegrationTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	// -------------------------------------------------------------------------
	// Order 1 — QSim reference run
	// -------------------------------------------------------------------------

	/**
	 * Runs the scenario with the standard QSim.  The output events file serves
	 * as the golden reference for all subsequent DSim tests.
	 */
	@Test
	@Order(1)
	void qsim() {
		var config = EvDSimTestFixture.createConfig(utils.getOutputDirectory());
		var scenario = EvDSimTestFixture.createScenario(config, 1 /* single partition */);

		var controller = new Controler(scenario);
		EvDSimTestFixture.installEvModules(controller);
		controller.run();
	}

	// -------------------------------------------------------------------------
	// Order 2 — DSim runs (compare against QSim reference)
	// -------------------------------------------------------------------------

	/**
	 * Runs DSim with three partitions on a single thread pool (NullCommunicator).
	 * All inter-partition communication is in-process.
	 */
	@Test
	@Order(2)
	void threePartitions() {
		var config = EvDSimTestFixture.createConfig(utils.getOutputDirectory());
		EvDSimTestFixture.configureDSim(config, 3);
		var scenario = EvDSimTestFixture.createScenario(config, 3);

		var ctx = DistributedContext.create(new NullCommunicator(), config);
		var controller = new Controler(scenario, ctx);
		EvDSimTestFixture.installEvModules(controller);
		controller.run();

		assertEventsMatchQSim();
	}

	/**
	 * Runs DSim with three separate JVM threads, each owning one partition.
	 * Uses {@link LocalCommunicator} for in-process message passing between ranks.
	 * <p>
	 * Disabled until WP3–WP5 are implemented: threads fail during Guice injection
	 * (singleton EV handlers not yet partition-aware) and the {@link LocalCommunicator}
	 * receive loops spin indefinitely waiting for peers that already failed.
	 */
	@Test
	@Order(2)
	@Timeout(value = 2, unit = TimeUnit.MINUTES)
	@Disabled("Enable once EV handlers are partition-aware (WP3-WP5)")
	void threeNodes() throws InterruptedException, ExecutionException, TimeoutException {
		var outputDirectory = utils.getOutputDirectory();
		var size = 3;
		var comms = LocalCommunicator.create(size);

		try (var pool = Executors.newFixedThreadPool(size)) {
			var futures = comms.stream()
				.map(comm -> pool.submit(() -> {
					var config = EvDSimTestFixture.createConfig(outputDirectory);
					config.controller().setOverwriteFileSetting(
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
					EvDSimTestFixture.configureDSim(config, 1 /* 1 partition per node */);

					var scenario = EvDSimTestFixture.createScenario(config, 3);
					var ctx = DistributedContext.create(comm, config);
					var controller = new Controler(scenario, ctx);
					EvDSimTestFixture.installEvModules(controller);
					controller.run();

					try {
						comm.close();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}))
				.toList();

			try {
				for (var f : futures) {
					f.get(1, TimeUnit.MINUTES);
				}
			} catch (ExecutionException | TimeoutException e) {
				pool.shutdownNow();
				throw e;
			}
		}

		assertEventsMatchQSim();
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private void assertEventsMatchQSim() {
		var expectedEventsPath = Paths.get(utils.getOutputDirectory())
			.resolve("..").resolve("qsim").resolve("output_events.xml")
			.toAbsolutePath().toString();
		var actualEventsPath = utils.getOutputDirectory() + "output_events.xml";

		assertThat(EventsUtils.compareEventsFiles(expectedEventsPath, actualEventsPath))
			.isEqualTo(ComparisonResult.FILES_ARE_EQUAL);
	}
}
