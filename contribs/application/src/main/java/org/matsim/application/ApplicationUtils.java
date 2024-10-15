package org.matsim.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigAliases;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ApplicationUtils {

	private static final Logger log = LogManager.getLogger(ApplicationUtils.class);

	/**
	 * This encoding indicates command line was used to start the jar.
	 */
	private static final String WIN_CLI_ENCODING = "cp850";

	private ApplicationUtils() {
	}


	/**
	 * Merge given arguments with custom ones.
	 *
	 * @param args        given args, usually input from command line / main method
	 * @param defaultArgs default arguments that will be added to existing ones.
	 */
	public static String[] mergeArgs(String[] args, String... defaultArgs) {
		String[] mergedArgs = new String[args.length + defaultArgs.length];
		System.arraycopy(args, 0, mergedArgs, 0, args.length);
		System.arraycopy(defaultArgs, 0, mergedArgs, args.length, defaultArgs.length);
		return mergedArgs;
	}

	/**
	 * Utility method to check if a jar might be run from the desktop (using double-click).
	 */
	public static boolean isRunFromDesktop() {

		// check if gui was explicitly enabled
		String env = System.getenv().getOrDefault("RUN_GUI", "undefined");
		if (env.equalsIgnoreCase("true") || env.equals("1"))
			return true;
		else if (env.equalsIgnoreCase("false") || env.equals("0"))
			return false;

		String property = System.getProperty("RUN_GUI", "undefined");
		if (property.equalsIgnoreCase("true") || property.equals("1"))
			return true;
		else if (property.equalsIgnoreCase("false") || property.equals("0"))
			return false;

		String macIdentifier = System.getenv().getOrDefault("__CFBundleIdentifier", "none");

		if (macIdentifier.equals("com.apple.java.JarLauncher") || macIdentifier.equals("com.apple.JavaLauncher"))
			return true;

		String os = System.getProperty("os.name");

		if (os.toLowerCase().startsWith("windows")) {

			// presence of the prompt variable indicates that the jar was run from the command line
			boolean hasPrompt = System.getenv().containsKey("PROMPT");

			// this prompt is not set in PowerShell, so we need another check
			if (hasPrompt)
				return false;

			// stdout.encoding from CLI are usually cp850
			String encoding = System.getProperty("stdout.encoding", "none");
			String sunEncoding = System.getProperty("sun.stdout.encoding", "none");

			if (encoding.equals(WIN_CLI_ENCODING) || sunEncoding.equals(WIN_CLI_ENCODING))
				return false;

			// Run from intelij, will not start the gui by default
			if (System.getenv().containsKey("IDEA_INITIAL_DIRECTORY"))
				return false;
			// also file.encoding=UTF-8, seems to be set by default in IntelliJ

			// if no other cli indicators are present, we have to assume that the jar was run from the desktop
			return true;
		}

		return false;

	}

	/**
	 * Apply run configuration in yaml format.
	 */
	public static void applyConfigUpdate(Config config, Path yaml) {

		if (!Files.exists(yaml)) {
			throw new IllegalArgumentException("Given config yaml does not exist: " + yaml);
		}

		ObjectMapper mapper = new ObjectMapper(new YAMLFactory()
			.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));

		ConfigAliases aliases = new ConfigAliases();
		Deque<String> emptyStack = new ArrayDeque<>();

		try (BufferedReader reader = Files.newBufferedReader(yaml)) {

			JsonNode node = mapper.readTree(reader);

			Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> field = fields.next();
				String configGroupName = aliases.resolveAlias(field.getKey(), emptyStack);
				ConfigGroup group = config.getModules().get(configGroupName);
				if (group == null) {
					group = new ConfigGroup(configGroupName);
					config.addModule(group);
				}

				applyNodeToConfigGroup(field.getValue(), group);
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	/**
	 * Sets the json config into
	 */
	private static void applyNodeToConfigGroup(JsonNode node, ConfigGroup group) {

		Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> field = fields.next();

			if (isParameterSet(field.getValue())) {

				// store the current parameters sets, newly added sets are not merged with each other
				List<? extends ConfigGroup> params = new ArrayList<>(group.getParameterSets(field.getKey()));

				for (JsonNode item : field.getValue()) {


					// Special case of parameter sets that have only one entry
					if (field.getValue().size() == 1 && params.size() == 1 && field.getValue().get(0).isObject()) {

						applyNodeToConfigGroup(field.getValue().get(0), params.get(0));

					} else {

						applyNodeAsParameterSet(field.getKey(), item, group, params);
					}
				}
			} else {

				if (field.getValue().isTextual())
					group.addParam(field.getKey(), field.getValue().textValue());
				else if (field.getValue().isArray()) {
					// arrays are joined using ","
					Stream<JsonNode> stream = StreamSupport.stream(field.getValue().spliterator(), false);
					String string = stream.map(n -> n.isTextual() ? n.textValue() : n.toString()).collect(Collectors.joining(","));
					group.addParam(field.getKey(), string);
				} else
					group.addParam(field.getKey(), field.getValue().toString());
			}
		}
	}

	/**
	 * Any array of complex object can be considered a config group.
	 */
	private static boolean isParameterSet(JsonNode node) {

		if (!node.isArray())
			return false;

		// any object can be assigned as parameter set
		for (JsonNode el : node) {
			if (!el.isObject())
				return false;
		}

		return true;
	}

	/**
	 * Handle possible update and creation of parameter sets within a config group.
	 */
	private static void applyNodeAsParameterSet(String groupName, JsonNode item, ConfigGroup group, List<? extends ConfigGroup> params) {

		Iterator<Map.Entry<String, JsonNode>> it = item.fields();

		// There was at least one matching group
		boolean matched = false;

		while (!params.isEmpty() && it.hasNext()) {

			Map.Entry<String, JsonNode> attr = it.next();
			List<? extends ConfigGroup> candidates = params.stream()
				.filter(p -> p.getParams().containsKey(attr.getKey()))
				.filter(p -> p.getParams().get(attr.getKey())
					.equals(attr.getValue().isTextual() ? attr.getValue().textValue() : attr.getValue().toString()))
				.toList();

			if (candidates.isEmpty())
				break;

			matched = true;
			params = candidates;
		}

		if (params.size() > 1) {
			throw new IllegalArgumentException("Ambiguous parameter set: " + item);
		} else if (params.size() == 1 && matched) {
			applyNodeToConfigGroup(item, params.get(0));
		} else {
			ConfigGroup newGroup = group.createParameterSet(groupName);
			group.addParameterSet(newGroup);
			applyNodeToConfigGroup(item, newGroup);
		}
	}

	/**
	 * Extends a context (usually config location) with a relative filename.
	 * If the results is a local file, the path will be returned. Otherwise, it will be an url.
	 * The results can be used as input for command line parameter or {@link IOUtils#resolveFileOrResource(String)}.
	 *
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
			try (Stream<Path> list = Files.list(path)) {
				return list.filter(p -> m.matches(p.getFileName()))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("No " + pattern + " file found."));
			}
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
		config.controller().setOutputDirectory(runDirectory.toString());
		config.controller().setRunId(resolvedRunId);

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
			if (field.getType().equals(InputOptions.class)) {
				input = true;
				CommandLine.Mixin mixin = field.getAnnotation(CommandLine.Mixin.class);
				if (mixin == null)
					throw new IllegalArgumentException(String.format("The command %s has no @Mixin annotation for InputOptions %s.", command, field.getName()));
			}

			if (field.getType().equals(OutputOptions.class)) {
				output = true;
				CommandLine.Mixin mixin = field.getAnnotation(CommandLine.Mixin.class);
				if (mixin == null)
					throw new IllegalArgumentException(String.format("The command %s has no @Mixin annotation for OutputOptions %s.", command, field.getName()));
			}
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

		// Match more general pattern
		path = matchPattern(".+\\.[a-zA-Z0-9\\-]*_" + name + "\\..+", dir);
		if (path.isPresent())
			return path.get();

		// Even more permissive pattern
		path = matchPattern(".+[a-zA-Z0-9_.\\-]*(_|\\.)" + name + ".+", dir);
		if (path.isPresent())
			return path.get();

		throw new IllegalArgumentException("Could not match input file: %s (in %s)".formatted(name, dir));
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
