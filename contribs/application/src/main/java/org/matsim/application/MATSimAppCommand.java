package org.matsim.application;


import picocli.CommandLine;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Interface for {@link MATSimApplication} commands. This is just a subtype of {@link Callable<Integer>}.
 * <p>
 * The developer needs to override {@link #call()} to implement the commands functionality.
 */
public interface MATSimAppCommand extends Callable<Integer> {

	/**
	 * Run the command logic.
	 *
	 * @return return code, 0 - success. 1 - general failure, 2 - user/input error
	 */
	@Override
	Integer call() throws Exception;

	/**
	 * Execute the command with given arguments.
	 *
	 * @param args arguments passed to the command
	 * @implNote This method is for convenience and does not need to be overwritten
	 */
	default void execute(String... args) {
		CommandLine cli = new CommandLine(this);
		AtomicReference<Exception> exc = new AtomicReference<>();
		cli.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
			exc.set(ex);
			return 2;
		});

		int code = cli.execute(args);

		if (code > 0) {
			Exception e = exc.get();
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException("Command exited with error", e);

		}
	}

	/**
	 * Apply the given command line arguments to this instance and return it.
	 */
	default MATSimAppCommand withArgs(String... args) {
		CommandLine cli = new CommandLine(this);
		CommandLine.ParseResult parseResult = cli.parseArgs(args);

		if (!parseResult.errors().isEmpty())
			throw new IllegalStateException("Error parsing arguments", parseResult.errors().get(0));

		return cli.getCommand();
	}

}
