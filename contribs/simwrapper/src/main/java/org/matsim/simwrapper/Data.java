package org.matsim.simwrapper;

import org.matsim.application.MATSimAppCommand;

import java.nio.file.Path;
import java.util.List;

/**
 * Manages and holds data needed for {@link org.matsim.simwrapper.SimWrapper},
 */
public final class Data {

	private Path path;

	private String currentContext;

	/**
	 * Set the default args that will be used for a specific command.
	 */
	public Data args(Class<? extends MATSimAppCommand> command, String... args) {
		return this;
	}

	public Data shp(Class<? extends MATSimAppCommand> command, String path) {
		return this;
	}

	/**
	 * Reference to a file within the runs output directory.
	 *
	 * @param path don't use path separators, but multiple arguments
	 */
	public String output(String... path) {
		return "";
	}

	/**
	 * Uses a command to construct the require output.
	 *
	 * @param command the command to be executed
	 * @param file    name of the produced output file
	 */
	public String compute(Class<? extends MATSimAppCommand> command, String file) {
		return "";
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
	}

	void setPath(Path path) {
		this.path = path;
	}

	List<MATSimAppCommand> getCommands() {

		// TODO: order by dependencies
		// link input output files

		// TODO: use the command runner

		return null;
	}


	// TODO: shp file options, multiple shape options
	// TODO: same command / file with different options / shapes

}
