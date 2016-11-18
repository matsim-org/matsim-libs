package playground.sebhoerl.mexec.examples;

import org.apache.commons.lang3.event.EventUtils;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import playground.sebhoerl.mexec.*;
import playground.sebhoerl.mexec.local.LocalEnvironment;
import playground.sebhoerl.mexec.placeholders.PlaceholderUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class RunEquil {
    public static void main(String[] args) throws InterruptedException, IOException {
        String localControllerPath = "/home/sebastian/Downloads/matsim";
        String localScenarioPath = "/home/sebastian/Downloads/matsim/examples/equil";

        Environment environment = new LocalEnvironment("/home/sebastian/mexecenv");

        Scenario scenario;
        if (!environment.hasScenario("equil")) {
            scenario = environment.createScenario("equil", localScenarioPath);
        } else {
            scenario = environment.getScenario("equil");
        }

        Controller controller;
        if (!environment.hasController("standard")) {
            controller = environment.createController("standard", localControllerPath, "matsim-0.8.0.jar", "org.matsim.run.Controler");
        } else {
            controller = environment.getController("standard");
        }

        Simulation simulation = environment.getSimulaiton("sim1");

        EventsManager eventsManager = new EventsManagerImpl();
        eventsManager.addHandler(new PersonDepartureEventHandler() {
            @Override
            public void handleEvent(PersonDepartureEvent event) {
                System.out.println("DEPARTURE");
            }

            @Override
            public void reset(int iteration) {}
        });

        SimulationUtils.processEvents(eventsManager, simulation);

        /*if (environment.hasSimulation("sim1")) {
            Simulation simulation = environment.getSimulaiton("sim1");

            if (simulation.isActive()) {
                simulation.stop();
            }

            while (simulation.isActive()) {
                Thread.sleep(100);
            }

            environment.removeSimulation(simulation);
        }

        Simulation simulation = environment.createSimulation("sim1", scenario, controller);

        Config config = simulation.getConfig();
        config.setParameter("network", "inputNetworkFile", "%{scenario}/network.xml");
        config.setParameter("plans", "inputPlansFile", "%{scenario}/plans100.xml");
        config.setParameter("controler", "outputDirectory", "%{output}");
        config.setParameter("controler", "overwriteFiles", "deleteDirectoryIfExists");
        simulation.save();

        simulation.start();

        while (simulation.isActive()) {
            Thread.sleep(1000);
            Long iteration = simulation.getIteration();

            if (iteration == null) {
                System.out.println("Iteration: unknown");
            } else {
                System.out.println("Iteration: " + iteration);
            }
        }*/
    }
}
