package org.matsim.simwrapper;

import org.matsim.application.MATSimAppCommand;

/**
 * Manages and holds data needed for {@link org.matsim.simwrapper.SimWrapper},
 */
public final class Data {

	/**
	 * Set the default args that will be used for a specific command.
	 */
	public Data defaultArgs(Class<? extends MATSimAppCommand> command, String... args) {
		return this;
	}

	/**
	 * Reference to a file within the runs output directory.
	 *
	 * @param path don't use path separators, but multiple arguments
	 * @return
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

	public String resource(String path) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

}
