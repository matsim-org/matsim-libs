package org.matsim.application;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "run", description = "Run the scenario")
class RunScenario implements Callable<Integer> {

    @CommandLine.ParentCommand
    private MATSimApplication app;

    @Override
    public Integer call() throws Exception {
        return app.call();
    }

}
