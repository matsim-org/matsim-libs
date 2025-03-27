package org.matsim.application.options;

import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import picocli.CommandLine;

import java.nio.file.Path;

/**
 * High-level options to include configure a scenario to be given via command line.
 */
public class ScenarioOptions {

	@CommandLine.Option(names = "--config", description = "Path (or URI) to scenario config", required = true)
	private String configPath;

	@CommandLine.Option(names = "--scenario", description = "Full qualified classname of the MATSim application scenario class. The IMC modules must be specified there.", required = true)
	private Class<? extends MATSimApplication> scenario;

	@CommandLine.Option(names = "--args", description = "Arguments passed to the scenario")
	private String scenarioArgs;

	@CommandLine.Option(names = "--population", description = "Path to input population")
	private String populationPath;

	private Config config;

	/**
	 * Loads and stores the config for the scenario.
	 */
	public Config getConfig() {
		if (config == null) {
			config = ConfigUtils.loadConfig(configPath);

			if (populationPath != null)
				config.plans().setInputFile(populationPath);
		}

		return config;
	}


	/**
	 * Create a controler to run the scenario.
	 *
	 * @return
	 */
	public Controler createControler() {
		if (scenarioArgs == null || scenarioArgs.isBlank())
			return MATSimApplication.prepare(scenario, getConfig());
		else
			return MATSimApplication.prepare(scenario, getConfig(), scenarioArgs.split(" "));
	}

}
