package org.matsim.application.options;

import org.matsim.application.ApplicationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import picocli.CommandLine;

import java.nio.file.Path;

/**
 * Command line options which indicates that a process accepts a scenario configuration. This config is always optional.
 */
public class ConfigOptions {

	@CommandLine.Option(names = "--config", description = "Path (or URI) to scenario config", required = false)
	private String configPath;


	/**
	 * Tries to load the config from folder if no specific config has been given via command line.
	 * @see ApplicationUtils#matchInput(String, Path)
	 */
	public Config loadConfig(Path path) {
		if (configPath == null) {
			return ConfigUtils.loadConfig(ApplicationUtils.matchInput("config.xml", path).toAbsolutePath().toString());
		}

		return ConfigUtils.loadConfig(configPath);
	}


}
