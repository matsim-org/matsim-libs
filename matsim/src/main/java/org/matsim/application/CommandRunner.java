package org.matsim.application;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.matsim.application.options.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Automatically runs commands by using the {@link CommandSpec} and various Options classes.
 */
public final class CommandRunner {

	private static final Logger log = LogManager.getLogger(CommandRunner.class);

	/**
	 * Name of the runner.
	 */
	private final String name;
	private final Map<Class<? extends MATSimAppCommand>, String> shpFiles = new HashMap<>();
	private final Map<Class<? extends MATSimAppCommand>, String[]> args = new HashMap<>();
	private Path output;
	private String defaultShp = null;
	private String defaultCrs = null;
	private Double defaultSampleSize = null;
	private String configPath = null;


	/**
	 * Construct a new runner.
	 */
	public CommandRunner() {
		this("");
	}

	/**
	 * Construct a new runner with a given name.
	 */
	public CommandRunner(String name) {
		this(name, Path.of(".").toAbsolutePath());
	}

	/**
	 * Construct a runner with specific name and path.
	 */
	private CommandRunner(String name, Path output) {
		this.name = name;
		this.output = output;
	}

	/**
	 * Run the specified command. Required input files are searched on {@code input} path.
	 *
	 * @param input search path for input files not defined as output by any command.
	 */
	public void run(Path input) {

		if (!Files.exists(input))
			throw new IllegalArgumentException("Input path does not exists:" + input);

		// Run graph with dependencies
		Graph<Class<? extends MATSimAppCommand>, DefaultEdge> graph = new DirectedAcyclicGraph<>(DefaultEdge.class);

		Set<Class<? extends MATSimAppCommand>> start = new HashSet<>();

		for (Map.Entry<Class<? extends MATSimAppCommand>, String[]> e : args.entrySet()) {
			Class<? extends MATSimAppCommand> clazz = e.getKey();
			graph.addVertex(clazz);
			Dependency[] depends = ApplicationUtils.getSpec(clazz).dependsOn();

			boolean hasDependencies = false;
			for (Dependency d : depends) {
				// Add dependency graph if the dependency is executed as well
				if (args.containsKey(d.value())) {
					graph.addVertex(d.value());
					graph.addEdge(d.value(), clazz);
					hasDependencies = true;
				}
			}
			if (!hasDependencies)
				start.add(clazz);
		}

		BreadthFirstIterator<Class<? extends MATSimAppCommand>, DefaultEdge> it = new BreadthFirstIterator<>(graph, start);
		while (it.hasNext()) {
			Class<? extends MATSimAppCommand> clazz = it.next();
			try {
				// Collect garbage between commands, because they might use quite some memory
				System.gc();
				runCommand(clazz, input);

			} catch (ReflectiveOperationException ex) {
				log.error("Command {} could not be crated.", clazz, ex);
			} catch (RuntimeException e) {
				log.error("Command {} threw an error.", clazz, e);
			}
		}
	}

	/**
	 * Execute the command with configured arguments.
	 */
	private void runCommand(Class<? extends MATSimAppCommand> clazz, Path input) throws ReflectiveOperationException {

		MATSimAppCommand command = clazz.getDeclaredConstructor().newInstance();
		String[] args = this.args.get(clazz);
		args = ArrayUtils.addAll(args, createArgs(clazz, args, input));
		log.info("Running {} with arguments: {}", clazz, String.join(" ", args));

		command.execute(args);
	}

	/**
	 * Build the base path.
	 */
	private Path buildPath(CommandSpec spec, Class<? extends MATSimAppCommand> command) {

		// use the command name if it is present and no other group name given
		String packageName = command.getPackageName();
		if (packageName.contains(".")) {
			packageName = packageName.substring(packageName.lastIndexOf(".") + 1);
		}

		String context = spec.group().isBlank() ? packageName : spec.group();

		if (name != null && !name.isBlank())
			context += "-" + name;

		return output.resolve(context);
	}


	private String[] createArgs(Class<? extends MATSimAppCommand> command, String[] existingArgs, Path input) {

		List<String> args = new ArrayList<>();

		CommandSpec spec = ApplicationUtils.getSpec(command);

		for (String require : spec.requires()) {

			// Whether this file is produced by a dependency
			String arg = "--input-" + InputOptions.argName(require);

			boolean present = ArrayUtils.contains(existingArgs, arg);
			if (present)
				continue;

			// Look for this file on the input
			String path = ApplicationUtils.matchInput(require, input).toString();
			args.add(arg);
			args.add(path);
		}

		for (Dependency depend : spec.dependsOn()) {
			for (String file : depend.files()) {
				String arg = "--input-" + InputOptions.argName(file);
				if (!ArrayUtils.contains(existingArgs, arg)) {
					String path = getPath(depend.value(), file);
					args.add(arg);
					args.add(path);
				}
			}
		}

		if (spec.requireEvents() && !ArrayUtils.contains(existingArgs, "--events")) {
			args.add("--events");
			args.add(ApplicationUtils.matchInput("events.xml", input).toString());
		}
		if (spec.requirePopulation() && !ArrayUtils.contains(existingArgs, "--population")) {
			args.add("--population");
			args.add(ApplicationUtils.matchInput("plans.xml", input).toString());
		}
		if (spec.requireNetwork() && !ArrayUtils.contains(existingArgs, "--network")) {
			args.add("--network");
			args.add(ApplicationUtils.matchInput("network.xml", input).toString());
		}
		if (spec.requireCounts() && !ArrayUtils.contains(existingArgs, "--counts")) {
			args.add("--counts");
			args.add(ApplicationUtils.matchInput("counts.xml", input).toString());
		}
		if (spec.requireRunDirectory() && !ArrayUtils.contains(existingArgs, "--run-directory")) {
			args.add("--run-directory");
			args.add(input.toString());
		}

		if (ApplicationUtils.acceptsOptions(command, ShpOptions.class) && !ArrayUtils.contains(existingArgs, "--shp")) {
			if (shpFiles.containsKey(command)) {
				args.add("--shp");
				args.add(shpFiles.get(command));
			} else if (defaultShp != null) {
				args.add("--shp");
				args.add(defaultShp);
			}
		}

		if (ApplicationUtils.acceptsOptions(command, CrsOptions.class) && !ArrayUtils.contains(existingArgs, "--input-crs")) {
			if (defaultCrs != null) {
				args.add("--input-crs");
				args.add(defaultCrs);
			}
		}

		if (ApplicationUtils.acceptsOptions(command, SampleOptions.class) && !ArrayUtils.contains(existingArgs, "--sample-size")) {
			if (defaultSampleSize != null) {
				args.add("--sample-size");
				args.add(String.valueOf(defaultSampleSize));
			}
		}

		if (ApplicationUtils.acceptsOptions(command, ConfigOptions.class) && !ArrayUtils.contains(existingArgs, "--config")) {
			if (configPath != null) {
				args.add("--config");
				args.add(configPath);
			}
		}

		// Adds output arguments for this class
		for (String produce : spec.produces()) {
			String arg = "--output-" + InputOptions.argName(produce);
			String path = getPath(command, produce);
			args.add(arg);
			args.add(path);
		}

		return args.toArray(new String[0]);
	}


	/**
	 * Get the path for a certain file produced by a command.
	 */
	private String getPath(Class<? extends MATSimAppCommand> command, String file) {
		CommandSpec spec = ApplicationUtils.getSpec(command);
		return buildPath(spec, command).resolve(file).toString();
	}

	/**
	 * Returns the output path of a command. Will throw an exception if this command does not declare it as an output.
	 */
	public Path getRequiredPath(Class<? extends MATSimAppCommand> command, String file) {
		CommandSpec spec = ApplicationUtils.getSpec(command);
		if (!ArrayUtils.contains(spec.produces(), file))
			throw new IllegalArgumentException(String.format("Command %s does not declare output %s", command, file));

		return buildPath(spec, command).resolve(file);
	}

	/**
	 * Return the output of a command with a placeholder.
	 * @param file file name, which must contain a %s, which will be replaced by the placeholder
	 */
	public Path getRequiredPath(Class<? extends MATSimAppCommand> command, String file, String placeholder) {
		CommandSpec spec = ApplicationUtils.getSpec(command);
		if (!ArrayUtils.contains(spec.produces(), file))
			throw new IllegalArgumentException(String.format("Command %s does not declare output %s", command, file));
		if (!file.contains("%s"))
			throw new IllegalArgumentException(String.format("File %s does not contain placeholder %%s", file));

		file = String.format(file, placeholder);

		return buildPath(spec, command).resolve(file);
	}

	/**
	 * Base path for the runner.
	 */
	public Path getOutput() {
		return output;
	}

	/**
	 * Set output folder.
	 *
	 * @return same instance
	 */
	public CommandRunner setOutput(Path path) {
		this.output = path;
		return this;
	}

	/**
	 * Name of the runner.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Add a command with certain arguments to the runner.
	 */
	public void add(Class<? extends MATSimAppCommand> command, String... args) {

		if (args.length != 0) {
			String[] existing = this.args.get(command);
			if (existing != null && existing.length > 0 && !Arrays.equals(existing, args)) {
				throw new IllegalArgumentException(String.format("Command %s already registered with args %s, can not define different args as %s (name '%s').",
					command.toString(), Arrays.toString(existing), Arrays.toString(args), name));
			}
		}

		ApplicationUtils.checkCommand(command);

		if (!this.args.containsKey(command) || this.args.get(command).length == 0)
			this.args.put(command, args);

		CommandSpec spec = ApplicationUtils.getSpec(command);

		// Add dependent classes
		for (Dependency depends : spec.dependsOn()) {
			if (depends.required() && !this.args.containsKey(depends.value()))
				add(depends.value());
		}
	}

	/**
	 * Insert args for an already existing command. If the command was not added, this does nothing.
	 */
	public void insertArgs(Class<? extends MATSimAppCommand> command, String... args) {

		if (!this.args.containsKey(command))
			return;

		String[] existing = this.args.get(command);
		String[] newArgs = new String[existing.length + args.length];
		System.arraycopy(args, 0, newArgs, 0, args.length);
		System.arraycopy(existing, 0, newArgs, args.length, existing.length);

		this.args.put(command, newArgs);
	}

	/**
	 * Set specific shape file for certain command.
	 */
	public void setShp(Class<? extends MATSimAppCommand> command, String path) {
		shpFiles.put(command, path);
	}

	/**
	 * Set the default shp file for all commands.
	 */
	public void setShp(String path) {
		defaultShp = path;
	}

	/**
	 * Set the default CRS passed to input.
	 */
	public void setCRS(String crs) {
		defaultCrs = crs;
	}

	/**
	 * Set a config path, which is passed to command using {@link ConfigOptions}.
	 */
	public void setConfigPath(String configPath) {
		this.configPath = configPath;
	}

	/**
	 * Set the default sample size that is passed as input to commands.
	 */
	public void setSampleSize(double sampleSize) {
		this.defaultSampleSize = sampleSize;
	}
}
