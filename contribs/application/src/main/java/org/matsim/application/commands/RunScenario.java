package org.matsim.application.commands;

import java.util.concurrent.Callable;
import org.matsim.application.MATSimApplication;
import picocli.CommandLine;

@CommandLine.Command(name = "run", description = "Run the scenario")
public class RunScenario implements Callable<Integer> {

  @CommandLine.ParentCommand private MATSimApplication app;

  @Override
  public Integer call() throws Exception {
    return app.call();
  }
}
