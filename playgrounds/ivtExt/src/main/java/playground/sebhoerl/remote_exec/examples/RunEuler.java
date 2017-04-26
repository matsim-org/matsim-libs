package playground.sebhoerl.remote_exec.examples;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import playground.sebhoerl.remote_exec.RemoteSimulation;
import playground.sebhoerl.remote_exec.euler.*;

import java.io.*;

public class RunEuler {
    public static void main(String[] args) throws JSchException, IOException, InterruptedException {
        EulerConfiguration config = new EulerConfiguration();
        config.setOutputPath("/cluster/scratch/shoerl/equil_remote"); // Working directory on Euler for simulations
        config.setScenarioPath("/cluster/home/shoerl/equil_remote"); // Storage directory on Euler for scenarios and controllers

        JSch jsch = new JSch();
        jsch.addIdentity("~/.ssh/eth");
        jsch.setKnownHosts("~/.ssh/known_hosts");

        Session session = jsch.getSession("shoerl", "euler", 22);
        session.connect();

        EulerEnvironment environment = new EulerEnvironment(new EulerInterface(session, config));
        EulerSimulation simulation = environment.getSimulation("RS4001");

        EventsManager events = new EventsManagerImpl();
        simulation.getEvents(events);

        //ByteArrayOutputStream output = new ByteArrayOutputStream();
        //simulation.getOutputFile("output_events.xml.gz", output);

        //System.out.println(new String(output.toByteArray()));

        session.disconnect();

        System.err.println("THE END");
    }
}
