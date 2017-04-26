package playground.sebhoerl.remote_exec.examples;

import playground.sebhoerl.remote_exec.*;
import playground.sebhoerl.remote_exec.local.*;

import java.io.IOException;

public class RunLocal {
    public static void main(String[] args) throws InterruptedException, IOException {
        LocalConfiguration config = new LocalConfiguration();
        config.setOutputPath("/home/sebastian/calibration/test_env/output"); // Working directory on Euler for simulations
        config.setScenarioPath("/home/sebastian/calibration/test_env/scenario"); // Storage directory on Euler for scenarios and controllers

        RemoteEnvironment environment = new LocalEnvironment(new LocalInterface(config));

        RemoteController controller;
        RemoteScenario scenario;
        RemoteSimulation simulation;

        if (!environment.hasController("standard")) {
            controller = environment.createController("standard", "/home/sebastian/calibration/controller", "matsim-0.8.0.jar", "org.matsim.run.Controler");
        } else {
            controller = environment.getController("standard");
        }

        if (!environment.hasScenario("sioux2016")) {
            scenario = environment.createScenario("sioux2016", "/home/sebastian/calibration/scenario");
        } else {
            scenario = environment.getScenario("sioux2016");
        }

        if (!environment.hasSimulation("sim1")) {
            simulation = environment.createSimulation("sim1", scenario, controller);
        } else {
            simulation = environment.getSimulation("sim1");
        }

        if (!(simulation.getStatus() == RemoteSimulation.Status.RUNNING || simulation.getStatus() == RemoteSimulation.Status.DONE)) {
            simulation.start();
        }

        RemoteSimulation.Status status;

        do {
            status = simulation.getStatus();
            long iteration = simulation.getIteration();

            System.out.println(String.format("Simulation %s at iteration %d [%s]",
                    simulation.getId(), iteration, status.toString()));

            Thread.sleep(1000);
        } while (!RemoteUtils.isFinished(status));
    }
}
