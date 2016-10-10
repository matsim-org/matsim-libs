package playground.sebhoerl.remote_exec.examples;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.events.EventsManagerImpl;
import playground.sebhoerl.remote_exec.*;
import playground.sebhoerl.remote_exec.euler.EulerConfiguration;
import playground.sebhoerl.remote_exec.euler.EulerEnvironment;
import playground.sebhoerl.remote_exec.euler.EulerInterface;

import java.io.*;

public class RunEquilOnEuler {
    static public void main(String[] args) throws JSchException, IOException, InterruptedException {
        // Path to the unpacked standard MATSim package (zip)
        String localPath = "/home/sebastian/Downloads/matsim";

        prepareConfig(localPath); // We have to prepare the config to be compatible with the system

        // In order to work on Euler we need a SSH connection with JSch
        JSch jsch = new JSch();
        jsch.addIdentity("~/.ssh/eth");
        jsch.setKnownHosts("~/.ssh/known_hosts");

        Session session = jsch.getSession("shoerl", "euler", 22);

        try {
            session.connect();
            runMatsim(localPath, session);
        } finally {
            session.disconnect();
        }
    }

    static public void prepareConfig(String localPath) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(localPath + "/examples/equil/config.xml")));
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(localPath + "/examples/equil/updated_config.xml"));

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.replace("examples/equil/", "%scenario/");
            line = line.replace("./output/equil", "%output");
            line = line.replace("4711", "%randomSeed");

            writer.write(line + "\n");
            writer.flush();
        }
    }

    static public void runMatsim(String localPath, Session session) throws IOException, JSchException, InterruptedException {
        // Create a configuration for the Euler environment
        EulerConfiguration config = new EulerConfiguration();
        config.setOutputPath("/cluster/scratch/shoerl/equil_remote"); // Working directory on Euler for simulations
        config.setScenarioPath("/cluster/home/shoerl/equil_remote"); // Storage directory on Euler for scenarios and controllers

        // Create the Euler environment. The EulerInterface is only used internally to handle the communication.
        RemoteEnvironment environment = new EulerEnvironment(new EulerInterface(session, config));

        // First, the scenario has to be set up in the new environment
        //   An unique ID is given, as well as the local path to the scenario (containing the config and all assets)
        RemoteScenario scenario = environment.createScenario("equil", localPath + "/examples/equil");
        scenario.setConfig("updated_config.xml"); // Set the custom config (instead of config.xml)

        // Second, the controller has to be set up by providing:
        // - a unique ID
        // - the local path to the controller (directory containing the necessary files)
        // - the Java CLASSPATH, which is the jar in this case
        // - the Java entry point of the controller
        RemoteController controller = environment.createController("standard", localPath, "matsim-0.8.0.jar", "org.matsim.run.Controler");

        // Third, we can set up an arbitrary number of simulations
        // Here, different car costs are used.
        int[] seeds = { 4001, 4002, 4003, 4004, 4005 };
        String[] ids = { "RS4001", "RS4002", "RS4003", "RS4004", "RS4005" };

        RemoteSimulation[] simulations = new RemoteSimulation[seeds.length];

        for (int i = 0; i < seeds.length; i++) {
            // Create a simulation by providing our scenario and controller
            RemoteSimulation simulation = environment.createSimulation(ids[i], scenario, controller);

            // Set the simulation-specific parameters, which will be overridden in the configuration
            simulation.setParameter("randomSeed", String.valueOf(seeds[i]));

            // Finally, start the simulation
            simulation.start();

            // Track for later use
            simulations[i] = simulation;
        }

        // Now the simulations are in "PENDING" state and will eventually switch to "RUNNING".
        // Just track their progress until they are finished.

        int finished = 0;

        do {
            // Print status information for all simulations
            for (RemoteSimulation simulation : simulations) {
                // Get some information (ID, status, current iteration, parameter)
                String id = simulation.getId();
                RemoteSimulation.Status status = simulation.getStatus();
                long iteration = simulation.getIteration();
                String parameter = simulation.getParameter("randomSeed");

                // Print it
                System.out.println(String.format("Simulation %s [%s] @ iteration %d [param=%s]",
                        id, status.toString(), iteration, parameter));

                if (RemoteUtils.isFinished(status)) {
                    // Check whether the simulation has finished.
                    // Basically check if status == DONE || status == ERROR
                    finished++;
                }
            }

            Thread.sleep(10000); // Repeat checks every 10 seconds
            System.out.println("");
        } while (finished < simulations.length); // Exit when all simulations are finished

        // For convenience, events can be read directly from Euler:
        EventsManager events = new EventsManagerImpl();
        simulations[0].getEvents(events);

        // All the things that have been created here (scenario / controller / simulations) will reside on
        // Euler and can be used later on. So if the program is started again, error messages would be
        // thrown, because the scenario, the controller and the simulations are still available.
        // For brevity, checks with environment.hasSimulation(...) or environment.hasController(...) have been
        // ommitted previously. However, what we can do here is to clean everything up:

        // First the simulations needs to be removed (otherwise trying to remove the scenario or controller will
        // result in exceptions)
        for (RemoteSimulation simulation : simulations) {
            simulation.remove();
        }

        // At least for the Euler implementation, all Simulation etc. objects are proxies, so using an object that has
        // been removed will result in an exception.

        // Now clean up the scenario and the controller
        scenario.remove();
        controller.remove();

        // Now it should be possible to call the script again without any problems.
    }
}
