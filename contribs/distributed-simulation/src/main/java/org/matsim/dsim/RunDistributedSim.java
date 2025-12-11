package org.matsim.dsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.communication.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.UnmaterializedConfigGroupChecker;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "dsim", mixinStandardHelpOptions = true, version = "1.0",
	description = "Run a distributed simulation")
public class RunDistributedSim implements Callable<Integer> {

	@CommandLine.Option(names = {"-r", "--rank"}, description = "Rank of this node", defaultValue = "0")
	private int rank;

	@CommandLine.Option(names = {"-t", "--total"}, description = "Total number of nodes", defaultValue = "1")
	private int total;

	@CommandLine.Option(names = {"--threads"}, description = "Number of threads on a node", defaultValue = "4")
	private int threads;

	@CommandLine.Option(names = {"--write-events"}, description = "Write events to the output directory", defaultValue = "false")
	private boolean writeEvents;

	@CommandLine.Option(names = {"--nodes"}, description = "List of all nodes", defaultValue = "")
	private String nodes;

	@CommandLine.Option(names = {"--address"}, description = "Address of this node", defaultValue = "")
	private String address;

	@CommandLine.Option(names = {"-c", "--communicator"}, description = "Type of communicator", defaultValue = "LOCAL")
	private Type communicator;

	@CommandLine.Option(names = {"-s", "--scenario"}, description = "Scenario to run (from matsim-examples)", defaultValue = "kelheim")
	private String scenario;

	@CommandLine.Option(names = {"-o", "--output"}, description = "Overwrite output path in the config")
	private String output;

	public static void main(String[] args) {
		new CommandLine(new RunDistributedSim()).execute(args);
	}

	public static URL getScenarioURL(String scenario) throws MalformedURLException, URISyntaxException {
		if (Files.exists(Path.of(scenario)))
			return Path.of(scenario).toUri().toURL();
		else if (!scenario.startsWith("http")) {
			final URL resource = RunDistributedSim.class.getResource("/test/scenarios/" + scenario + "/");
			return IOUtils.extendUrl(resource, "config.xml");
		} else
			return new URI(scenario).toURL();
	}

	@Override
	public Integer call() throws Exception {

		if (rank >= total)
			throw new IllegalArgumentException("Rank must be less than total. Rank index starts at 0.");

		Communicator comm = total == 1 ? new NullCommunicator() : switch (communicator) {
			case AERON -> new AeronCommunicator(rank, total, false, address);
			case AERON_IPC -> new AeronCommunicator(rank, total, true, address);
			case HAZELCAST -> new HazelcastCommunicator(rank, total, Communicator.parseNodeList(address, nodes));
			case SHM -> new SharedMemoryCommunicator(rank, total);
			case LOCAL -> new NullCommunicator();
		};

		// Some test scenarios:

		// https://github.com/matsim-scenarios/matsim-kelheim/raw/master/input/v3.1/kelheim-v3.1-config.xml
		// https://github.com/matsim-scenarios/matsim-leipzig/raw/main/input/v1.3/leipzig-v1.3.1-10pct.config.xml
		// https://github.com/matsim-scenarios/matsim-berlin/raw/main/input/v6.3/berlin-v6.3.config.xml

		URL url = getScenarioURL(scenario);
		Config config = ConfigUtils.loadConfig(url);

		if (output != null)
			config.controller().setOutputDirectory(output);

		// TODO: Deleting the directory causes problem with the existing controller if two simulations use the same directory
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		// Compatibility with many scenarios
		Activities.addScoringParams(config);
		config.controller().setMobsim(ControllerConfigGroup.MobsimType.dsim.name());
		config.controller().setWriteEventsInterval(writeEvents ? 1 : 0);
		config.controller().setLastIteration(0);

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

		// Randomness might cause differences on different nodes
		config.routing().setRoutingRandomness(0);

		config.dsim().setThreads(threads);

		Scenario s = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(s, DistributedContext.create(comm, config));

		controler.addOverridingModule(new DistributedSimulationModule());

		controler.getInjector();
		// Removes check after injector has been created, just a workaround to avoid exceptions
		controler.getConfig().removeConfigConsistencyChecker(UnmaterializedConfigGroupChecker.class);

		controler.run();

		comm.close();

		return 0;
	}

	public enum Type {
		AERON,
		AERON_IPC,
		HAZELCAST,
		SHM,
		LOCAL
	}

}
