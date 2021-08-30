package org.matsim.application;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.options.CrsOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Optional;

public class ApplicationUtils {

	private static final Logger log = LogManager.getLogger(ApplicationUtils.class);

	/**
	 * Helper function to glob for a required file.
	 *
	 * @throws IllegalStateException if no file was matched
	 */
	public static Path globFile(Path path, String pattern) {

		PathMatcher m = path.getFileSystem().getPathMatcher("glob:" + pattern);

		try {
			return Files.list(path)
					.filter(p -> m.matches(p.getFileName()))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("No " + pattern + " file found."));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Glob pattern from path, if not found tries to go into the parent directory.
	 */
	public static Optional<Path> globWithParent(Path path, String pattern) {
		PathMatcher m = path.getFileSystem().getPathMatcher("glob:" + pattern);
		try {
			Optional<Path> match = Files.list(path).filter(p -> m.matches(p.getFileName())).findFirst();
			// Look one directory higher for required file
			if (match.isEmpty())
				return Files.list(path.getParent()).filter(p -> m.matches(p.getFileName())).findFirst();

			return match;
		} catch (IOException e) {
			log.warn(e);
		}

		return Optional.empty();
	}

	/**
	 * Helper function to glob for a required file.
	 *
	 * @throws IllegalStateException if no file was matched
	 */
	public static String globFile(Path path, String runId, String name) {

		String file = globWithParent(path, runId + ".*" + name + ".*").orElseThrow(() -> new IllegalStateException("No " + name + "file found.")).toString();

		log.info("Using {} file: {}", name, file);

		return file;
	}

	/**
	 * Load scenario from a directory using globed patterns.
	 *
	 * @param runId        run id pattern
	 * @param runDirectory path to run directory
	 * @param crs          crs of the scenario
	 */
	public static Scenario loadScenario(String runId, Path runDirectory, CrsOptions crs) {
		log.info("Loading scenario...");

		Path populationFile = globWithParent(runDirectory, runId + ".*plans.*").orElseThrow(() -> new IllegalStateException("No plans file found."));
		int index = populationFile.getFileName().toString().indexOf(".");
		if (index == -1)
			index = 0;

		String resolvedRunId = populationFile.getFileName().toString().substring(0, index);
		log.info("Using population {} with run id {}", populationFile, resolvedRunId);

		Path networkFile = globWithParent(runDirectory, runId + ".*network.*").orElseThrow(() -> new IllegalStateException("No network file found."));
		log.info("Using network {}", networkFile);

		String facilitiesFile = globWithParent(runDirectory, runId + ".*facilities.*").map(Path::toString).orElse(null);
		log.info("Using facilities {}", facilitiesFile);

		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(crs.getInputCRS());
		config.controler().setOutputDirectory(runDirectory.toString());
		config.controler().setRunId(resolvedRunId);

		config.plans().setInputFile(populationFile.toString());
		config.network().setInputFile(networkFile.toString());
		config.facilities().setInputFile(facilitiesFile);

		return ScenarioUtils.loadScenario(config);
	}
}
