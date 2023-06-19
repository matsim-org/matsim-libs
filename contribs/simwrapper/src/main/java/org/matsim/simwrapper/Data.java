package org.matsim.simwrapper;

import org.apache.commons.io.FilenameUtils;
import org.matsim.application.CommandRunner;
import org.matsim.application.MATSimAppCommand;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages and holds data needed for {@link org.matsim.simwrapper.SimWrapper}.
 */
public final class Data {

	/**
	 * Configured Simwrapper config.
	 */
	private final SimWrapperConfigGroup config;

	/**
	 * Maps context to command runners.
	 */
	private final Map<String, CommandRunner> runners = new HashMap<>();

	/**
	 * Resources that needs to be copied.
	 */
	private final Map<Path, URL> resources = new HashMap<>();

	/**
	 * The output directory.
	 */
	private Path path;
	private CommandRunner currentContext;
	private SimWrapperConfigGroup.ContextParams context;

	Data(SimWrapperConfigGroup config) {
		// path needed, but not available yet
		this.currentContext = runners.computeIfAbsent("", CommandRunner::new);
		this.config = config;
	}

	/**
	 * Returns the config that was given to {@link SimWrapper}.
	 */
	public SimWrapperConfigGroup config() {
		return config;
	}

	/**
	 * The current active context configuration.
	 */
	public SimWrapperConfigGroup.ContextParams context() {
		return context;
	}

	/**
	 * Set the default args that will be used for a specific command.
	 */
	public Data args(Class<? extends MATSimAppCommand> command, String... args) {
		currentContext.add(command, args);
		return this;
	}

	/**
	 * Set shp file for specific command, otherwise default shp will be used.
	 */
	public Data shp(Class<? extends MATSimAppCommand> command, String path) {
		currentContext.setShp(command, path);
		return this;
	}

	/**
	 * Reference to a file within the runs output directory.
	 *
	 * @param first name of the file or first part of the path
	 * @param path  don't use path separators, but multiple arguments
	 */
	public String output(String first, String... path) {

		// Must use unix separators and not use Path because * might be used as part of the path
		StringBuilder p = new StringBuilder(first);
		for (String s : path) {
			p.append("/");
			p.append(s);
		}

		return p.toString();
	}

	/**
	 * Uses a command to construct the required output.
	 *
	 * @param command the command to be executed
	 * @param file    name of the produced output file
	 */
	public String compute(Class<? extends MATSimAppCommand> command, String file, String... args) {
		currentContext.add(command, args);
		Path path = currentContext.getRequiredPath(command, file);

		// Relative path from the simulation output
		return this.getUnixPath(this.path.getParent().relativize(path));
	}

	public String subcommand(String command, String file) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	/**
	 * Copies and references file bundled in the classpath.
	 *
	 * @param name name of the resource
	 */
	public String resource(String name) {

		URL resource = this.getClass().getResource(name);

		if (resource == null) {
			// Try to prefix / automatically
			resource = this.getClass().getResource("/" + name);
			if (resource == null)
				throw new IllegalArgumentException("Resource '" + name + "' not found!");
		}

		String baseName = FilenameUtils.getName(resource.getPath());

		Path baseDir;
		if (currentContext.getName().isBlank())
			baseDir = this.path.resolve("resources");
		else
			baseDir = this.path.resolve("resources-" + currentContext.getName());

		// Final path where resource should be copied
		Path resolved = baseDir.resolve(baseName);

		try {
			if (resources.containsKey(resolved) && !resources.get(resolved).toURI().equals(resource.toURI()))
				throw new IllegalArgumentException(String.format("Resource '%s' was already mapped to resource '%s'. ", name, resources.get(resolved)));

		} catch (URISyntaxException e) {
			throw new RuntimeException("Illegal URL", e);
		}

		resources.put(resolved, resource);
		return this.getUnixPath(this.path.getParent().relativize(resolved));
	}

	/**
	 * Returns the unix path. Otherwise, paths might be Windows paths, which are not compatible to simwrapper.
	 */
	private String getUnixPath(Path p) {
		return FilenameUtils.separatorsToUnix(p.toString());
	}

	/**
	 * Switch to a different context, which can hold different arguments and shp options.
	 */
	void setCurrentContext(String name) {
		currentContext = runners.computeIfAbsent(name, CommandRunner::new);
		currentContext.setOutput(path);
		context = config.get(name);
	}

	void setPath(Path path) {
		this.path = path;
	}

	Map<String, CommandRunner> getRunners() {
		return runners;
	}

	Map<Path, URL> getResources() {
		return resources;
	}
}
