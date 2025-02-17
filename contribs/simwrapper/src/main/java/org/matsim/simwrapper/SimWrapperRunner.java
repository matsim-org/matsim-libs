package org.matsim.simwrapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@CommandLine.Command(
	name = "simwrapper",
	description = "Run SimWrapper on existing folders and generate dashboard files."
)
public class SimWrapperRunner implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(SimWrapper.class);

	@CommandLine.Parameters(arity = "1..*", description = "Path to folders for which dashboards should be generated.")
	private List<Path> inputPaths;

	@CommandLine.Option(names = "--config", description = "Path to MATSim config that should be used. If not given tries to use output config.", required = false)
	private String configPath;

	@CommandLine.Option(names = "--exclude", split = ",", description = "Exclusion that will be added to the config.")
	private Set<String> exclude;

	@CommandLine.Option(names = "--include", split = ",", description = "Use only the dashboards which classnames match.")
	private Set<String> include;

	public static void main(String[] args) {
		new SimWrapperRunner().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		for (Path input : inputPaths) {
			log.info("Running on {}", input);

			Config config;
			if (configPath != null)
				config = ConfigUtils.loadConfig(configPath);
			else {

				try {
					Path path = ApplicationUtils.matchInput("config.xml", input);
					config = ConfigUtils.loadConfig(path.toString());
				} catch (IllegalArgumentException e) {
					log.warn("No output config found in {}, and no config given via --config", input);
					continue;
				}
			}

			SimWrapperConfigGroup simWrapperConfigGroup = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

			if (exclude != null)
				simWrapperConfigGroup.exclude.addAll(exclude);

			if (include != null)
				simWrapperConfigGroup.include.addAll(include);

			SimWrapperListener listener = new SimWrapperListener(SimWrapper.create(config), config);
			try {
				listener.run(input, configPath);
			} catch (IOException e) {
				log.error("Error creating dashboards on {}", input, e);
			}
		}

		return 0;
	}

}
