package org.matsim.contrib.pseudosimulation.distributed;

import java.io.PrintStream;

import org.matsim.core.config.Config;

/** Resolves slave launch settings and applies the legacy slave-specific MATSim config mutations. */
final class SlaveConfigPreparer {

	SlaveConnectionSettings prepareForConnection(Config config, DistributedSimConfigGroup distributedConfig,
			SlaveLaunchArguments arguments, PrintStream standardOut, PrintStream standardError,
			String optionsDescription) {
		config.eventsManager().setSynchronizeOnSimSteps(false);
		int numberOfThreads = resolveThreads(config, distributedConfig, arguments, standardOut, standardError,
				optionsDescription);
		config.global().setNumberOfThreads(numberOfThreads);
		config.eventsManager().setNumberOfThreads(1);

		String hostname = resolveHostname(arguments, standardError);
		int port = resolvePort(distributedConfig, arguments, standardOut, standardError, optionsDescription);
		return new SlaveConnectionSettings(hostname, port, numberOfThreads);
	}

	void prepareForScenario(Config config, int slaveNumber) {
		config.controller().setOutputDirectory(config.controller().getOutputDirectory() + "_" + slaveNumber);
		config.linkStats().setWriteLinkStatsInterval(0);
		config.controller().setCreateGraphsInterval(0);
		config.controller().setWriteEventsInterval(0);
		config.controller().setWritePlansInterval(0);
		config.controller().setWriteSnapshotsInterval(0);
		config.plans().setInputFile(null);
		config.eventsManager().setSynchronizeOnSimSteps(false);
		config.eventsManager().setNumberOfThreads(1);
	}

	void prepareController(Config config, int numberOfPlansOnSlave) {
		config.controller().setDumpDataAtEnd(false);
		config.replanning().setMaxAgentPlanMemorySize(numberOfPlansOnSlave);
	}

	private int resolveThreads(Config config, DistributedSimConfigGroup distributedConfig,
			SlaveLaunchArguments arguments, PrintStream standardOut, PrintStream standardError,
			String optionsDescription) {
		if (arguments.threads() == null) {
			standardError.println("Will use the number of threads in config for simulation.");
			return config.global().getNumberOfThreads();
		}
		try {
			return Integer.parseInt(arguments.threads());
		} catch (NumberFormatException e) {
			standardError.println("Number of threads should be int or it wasn't specced on cmd line. Taking the default of "
					+ distributedConfig.getDefaultNumThreadsOnSlave());
			standardOut.println(optionsDescription);
			return distributedConfig.getDefaultNumThreadsOnSlave();
		}
	}

	private String resolveHostname(SlaveLaunchArguments arguments, PrintStream standardError) {
		if (arguments.hostname() != null) {
			return arguments.hostname();
		}
		standardError.println("No host or IP specified, using default (localhost)");
		return "localhost";
	}

	private int resolvePort(DistributedSimConfigGroup distributedConfig, SlaveLaunchArguments arguments,
			PrintStream standardOut, PrintStream standardError, String optionsDescription) {
		if (arguments.port() == null) {
			standardError.println("Will accept connections on default port number 12345");
			return distributedConfig.getMasterPortNumber();
		}
		try {
			return Integer.parseInt(arguments.port());
		} catch (NumberFormatException e) {
			standardError.println("Port number should be integer. Defaulting to "
					+ distributedConfig.getMasterPortNumber());
			standardOut.println(optionsDescription);
			return distributedConfig.getMasterPortNumber();
		}
	}

	record SlaveConnectionSettings(String hostname, int port, int numberOfThreads) {
	}
}
