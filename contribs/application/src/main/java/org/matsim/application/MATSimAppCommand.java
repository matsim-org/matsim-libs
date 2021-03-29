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
     * @return return code, 0 - success. 1 - general failure, 2 -  user/input error
     * @throws Exception
     */
    @Override
    Integer call() throws Exception;

    default void execute(String... args) {
        CommandLine cli = new CommandLine(this);
        int ret = cli.execute(args);

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
                throw new RuntimeException("Application exited with error", e);

        }
    }
}
