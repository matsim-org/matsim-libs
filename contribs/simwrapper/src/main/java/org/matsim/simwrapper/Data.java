package org.matsim.simwrapper;

import org.matsim.application.CommandRunner;
import org.matsim.application.MATSimAppCommand;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages and holds data needed for {@link org.matsim.simwrapper.SimWrapper},
 */
public final class Data {

	private Path path;

	/**
	 * Maps context to command runners.
	 */
	private final Map<String, CommandRunner> runners = new HashMap<>();
	private CommandRunner currentContext;

	public Data() {

		// path needed, but not available yet
		currentContext = runners.computeIfAbsent("", CommandRunner::new);
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
	 * @param path don't use path separators, but multiple arguments
	 */
	public String output(String... path) {
		return String.join("/", path);
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
		return this.path.getParent().relativize(path).toString();
	}

	public String subcommand(String command, String file) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public String resource(String path) {
		throw new UnsupportedOperationException("Not implemented yet");
	}


	/**
	 * Switch to a different context, which can hold different arguments and shp options.
	 */
	void setCurrentContext(String name) {
		currentContext = runners.computeIfAbsent(name, CommandRunner::new);
		currentContext.setOutput(path);
	}

	void setPath(Path path) {
		this.path = path;
	}

	Map<String, CommandRunner> getRunners() {
		return runners;
	}

}
