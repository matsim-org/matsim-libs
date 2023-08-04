package org.matsim.application;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Optional;
import java.util.stream.Stream;

public class ApplicationUtils {

	private static final Logger log = LogManager.getLogger(ApplicationUtils.class);

	private ApplicationUtils() {
	}

	/**
	 * Extends a context (usually config location) with an relative filename.
	 * If the results is a local file, the path will be returned. Otherwise, it will be an url.
	 * The results can be used as input for command line parameter or {@link IOUtils#resolveFileOrResource(String)}.
	 * @return string with path or URL
	 */
	public static String resolve(URL context, String filename) {

		URL refURL = IOUtils.extendUrl(context, filename);
		String refData;
		try {
			refData = new File(refURL.toURI()).getAbsolutePath();
		} catch (URISyntaxException e) {
			refData = refURL.toString();
		}
		return refData;

	}

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

	/**
	 * Check if a command can be used with {@link CommandRunner}.
	 *
	 * @throws IllegalArgumentException if a command is not suitable.
	 */
	public static void checkCommand(Class<? extends MATSimAppCommand> command) {

		if (command.getAnnotation(CommandSpec.class) == null) {
			throw new IllegalArgumentException(String.format("The command %s has no @CommandSpec annotation.", command));
		}

		Field[] fields = command.getDeclaredFields();

		boolean input = false;
		boolean output = false;
		for (Field field : fields) {
			if (field.getType().equals(InputOptions.class))
				input = true;

			if (field.getType().equals(OutputOptions.class))
				output = true;
		}

		if (!input) {
			throw new IllegalArgumentException(String.format("The command %s has no field with InputOptions.", command));
		}

		if (!output) {
			throw new IllegalArgumentException(String.format("The command %s has no field with OutputOptions.", command));
		}
	}


	/**
	 * Whether this command accepts a specific class as options.
	 */
	public static boolean acceptsOptions(Class<? extends MATSimAppCommand> command, Class<?> options) {
		for (Field field : command.getDeclaredFields()) {
			if (field.getType().equals(options) && field.getAnnotation(CommandLine.Mixin.class) != null)
				return true;
		}

		return false;
	}

	/**
	 * Tries to match input file {@code name} with files from the input directory {@code dir}.
	 * Please check docs in the code for the conventions. Different naming conventions are tried in a specific order.
	 *
	 * @throws IllegalArgumentException if no file could be found.
	 */
	public static Path matchInput(String name, Path dir) {

		Path possibility = dir.resolve(name);
		if (Files.exists(possibility))
			return possibility;

		possibility = dir.resolve(name + ".gz");
		if (Files.exists(possibility))
			return possibility;

		// Match files that could be matsim output files
		Optional<Path> path = matchSuffix("output_" + name, dir);
		if (path.isPresent())
			return path.get();

		path = matchSuffix("output_" + name + ".gz", dir);
		if (path.isPresent())
			return path.get();

		path = matchSuffix(name, dir);
		if (path.isPresent())
			return path.get();

		// Match more general pattern at last
		path = matchPattern( ".+\\.[a-zA-Z0-9]*_" + name + "\\..+", dir);
		if (path.isPresent())
			return path.get();

		throw new IllegalArgumentException("Could not match input file: " + name);
	}

	private static Optional<Path> matchSuffix(String suffix, Path dir) {
		try (Stream<Path> stream = Files.list(dir)) {
			return stream.filter(p -> p.getFileName().toString().endsWith(suffix)).findFirst();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static Optional<Path> matchPattern(String pattern, Path dir) {
		try (Stream<Path> stream = Files.list(dir)) {
			return stream.filter(p -> p.getFileName().toString().matches(pattern)).findFirst();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Get the {@link CommandSpec} of a {@link MATSimAppCommand}.
	 */
	public static CommandSpec getSpec(Class<? extends MATSimAppCommand> command) {
		return command.getAnnotation(CommandSpec.class);
	}

	/**
	 * Get the {@link CommandLine.Command} of a {@link MATSimAppCommand}.
	 */
	public static CommandLine.Command getCommand(Class<? extends MATSimAppCommand> command) {
		return command.getAnnotation(CommandLine.Command.class);
	}

}
