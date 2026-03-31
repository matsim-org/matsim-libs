package org.matsim.dsim;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import picocli.CommandLine;

import java.net.URL;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "ssim", mixinStandardHelpOptions = true, version = "1.0",
	description = "Run a standard simulation")
public class RunStandardSim implements Callable<Integer> {

	private static final Logger log = LogManager.getLogger(RunStandardSim.class);

	@CommandLine.Option(names = {"-s", "--scenario"}, description = "Scenario to run (from matsim-examples)", defaultValue = "kelheim")
	private String scenario;

	@CommandLine.Option(names = {"-o", "--output"}, description = "Overwrite output path in the config")
	private String output;

	public static void main(String[] args) {
		new CommandLine(new RunStandardSim()).execute(args);
	}

	@Override
	public Integer call() throws Exception {

		URL url = RunDistributedSim.getScenarioURL(scenario);

		Config config = ConfigUtils.loadConfig(url);

		if (output != null)
			config.controller().setOutputDirectory(output);

		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setWriteEventsInterval(1);
		config.controller().setLastIteration(0);

		// Compatibility with many scenarios
		Activities.addScoringParams(config);

		Scenario s = ScenarioUtils.loadScenario(config);

		log.warn("Adding freight and ride as modes to all car links. As we need this in some scenarios. Ideally, this would be encoded in the network already.");
		var carandfreight = Set.of(TransportMode.car, "freight", TransportMode.ride);

		s.getNetwork().getLinks().values().parallelStream()
			.filter(l -> l.getAllowedModes().contains(TransportMode.car))
			.forEach(l -> l.setAllowedModes(carandfreight));

		Controler controler = new Controler(s);
		controler.run();

		return 0;
	}
}
