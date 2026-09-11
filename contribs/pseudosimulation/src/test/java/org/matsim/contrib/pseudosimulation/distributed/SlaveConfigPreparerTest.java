package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

class SlaveConfigPreparerTest {

	private final SlaveConfigPreparer preparer = new SlaveConfigPreparer();

	@Test
	void usesConfigAndDistributedDefaultsWhenOverridesAreAbsent() {
		Config config = ConfigUtils.createConfig();
		config.global().setNumberOfThreads(6);
		DistributedSimConfigGroup distributedConfig = new DistributedSimConfigGroup();
		distributedConfig.setMasterPortNumber(34567);
		CapturedOutput output = new CapturedOutput();

		SlaveConfigPreparer.SlaveConnectionSettings settings = preparer.prepareForConnection(
				config, distributedConfig, new SlaveLaunchArguments("config.xml", null, null, null),
				output.standardOut(), output.standardError(), "options");

		assertEquals("localhost", settings.hostname());
		assertEquals(34567, settings.port());
		assertEquals(6, settings.numberOfThreads());
		assertEquals(6, config.global().getNumberOfThreads());
		assertEquals(1, config.eventsManager().getNumberOfThreads());
		assertFalse(config.eventsManager().getSynchronizeOnSimSteps());
		assertEquals("", output.outText());
		assertEquals("Will use the number of threads in config for simulation.\n"
				+ "No host or IP specified, using default (localhost)\n"
				+ "Will accept connections on default port number 12345\n", output.errText());
	}

	@Test
	void appliesExplicitConnectionOverrides() {
		Config config = ConfigUtils.createConfig();
		CapturedOutput output = new CapturedOutput();

		SlaveConfigPreparer.SlaveConnectionSettings settings = preparer.prepareForConnection(
				config, new DistributedSimConfigGroup(),
				new SlaveLaunchArguments("config.xml", "worker", "45678", "12"),
				output.standardOut(), output.standardError(), "options");

		assertEquals("worker", settings.hostname());
		assertEquals(45678, settings.port());
		assertEquals(12, settings.numberOfThreads());
		assertEquals(12, config.global().getNumberOfThreads());
		assertEquals("", output.outText());
		assertEquals("", output.errText());
	}

	@Test
	void invalidNumericOverridesUseDistributedDefaultsAndPrintOptions() {
		Config config = ConfigUtils.createConfig();
		DistributedSimConfigGroup distributedConfig = new DistributedSimConfigGroup();
		distributedConfig.setDefaultNumThreadsOnSlave(3);
		distributedConfig.setMasterPortNumber(32123);
		CapturedOutput output = new CapturedOutput();

		SlaveConfigPreparer.SlaveConnectionSettings settings = preparer.prepareForConnection(
				config, distributedConfig,
				new SlaveLaunchArguments("config.xml", "worker", "bad-port", "bad-threads"),
				output.standardOut(), output.standardError(), "legacy options");

		assertEquals(3, settings.numberOfThreads());
		assertEquals(32123, settings.port());
		assertEquals("legacy options\nlegacy options\n", output.outText());
		assertEquals("Number of threads should be int or it wasn't specced on cmd line. Taking the default of 3\n"
				+ "Port number should be integer. Defaulting to 32123\n", output.errText());
	}

	@Test
	void preparesConfigForScenarioLoading() {
		Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory("output");
		config.linkStats().setWriteLinkStatsInterval(5);
		config.controller().setCreateGraphsInterval(2);
		config.controller().setWriteEventsInterval(3);
		config.controller().setWritePlansInterval(4);
		config.controller().setWriteSnapshotsInterval(6);
		config.plans().setInputFile("plans.xml");
		config.eventsManager().setSynchronizeOnSimSteps(true);
		config.eventsManager().setNumberOfThreads(7);

		preparer.prepareForScenario(config, 9);

		assertEquals("output_9", config.controller().getOutputDirectory());
		assertEquals(0, config.linkStats().getWriteLinkStatsInterval());
		assertEquals(0, config.controller().getCreateGraphsInterval());
		assertEquals(0, config.controller().getWriteEventsInterval());
		assertEquals(0, config.controller().getWritePlansInterval());
		assertEquals(0, config.controller().getWriteSnapshotsInterval());
		assertNull(config.plans().getInputFile());
		assertFalse(config.eventsManager().getSynchronizeOnSimSteps());
		assertEquals(1, config.eventsManager().getNumberOfThreads());
	}

	@Test
	void preparesFinalControllerLimits() {
		Config config = ConfigUtils.createConfig();
		config.controller().setDumpDataAtEnd(true);
		config.replanning().setMaxAgentPlanMemorySize(5);

		preparer.prepareController(config, 11);

		assertFalse(config.controller().getDumpDataAtEnd());
		assertEquals(11, config.replanning().getMaxAgentPlanMemorySize());
	}

	private static final class CapturedOutput {
		private final ByteArrayOutputStream out = new ByteArrayOutputStream();
		private final ByteArrayOutputStream err = new ByteArrayOutputStream();

		PrintStream standardOut() {
			return new PrintStream(out, true, StandardCharsets.UTF_8);
		}

		PrintStream standardError() {
			return new PrintStream(err, true, StandardCharsets.UTF_8);
		}

		String outText() {
			return out.toString(StandardCharsets.UTF_8);
		}

		String errText() {
			return err.toString(StandardCharsets.UTF_8);
		}
	}
}
