package playground.kai.usecases.mexec;

import java.io.File;

import org.apache.commons.lang3.SystemUtils;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import playground.sebhoerl.mexec.Config;
import playground.sebhoerl.mexec.Controller;
import playground.sebhoerl.mexec.Environment;
import playground.sebhoerl.mexec.Scenario;
import playground.sebhoerl.mexec.Simulation;
import playground.sebhoerl.mexec.SimulationUtils;
import playground.sebhoerl.mexec.local.LocalEnvironment;
import playground.sebhoerl.mexec.local.os.LinuxDriver;
import playground.sebhoerl.mexec.local.os.OSDriver;
import playground.sebhoerl.mexec.local.os.WindowsDriver;
import playground.sebhoerl.mexec.ssh.SSHEnvironment;
import playground.sebhoerl.mexec.ssh.utils.SSHUtils;

public class RunEquilLocally {
	public static void main(String[] args) {
		runLocally();
		//runRemotely();
	}

	private static void runLocally() {
		OSDriver driver = SystemUtils.IS_OS_WINDOWS ? new WindowsDriver() : new LinuxDriver();
		File environmentPath = new File(SystemUtils.getUserHome(), "mexecenv");
		Environment environment = new LocalEnvironment(environmentPath.toString(), driver);

		String localControllerPath = "/home/sebastian/Downloads/matsim";
		String localScenarioPath = "/home/sebastian/Downloads/matsim/examples/equil";

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

		Simulation simulation = null;

		if (environment.hasSimulation("sim1")) {
			simulation = environment.getSimulation("sim1");

			if (!simulation.isActive()) {
				environment.removeSimulation(environment.getSimulation("sim1"));
				simulation = null;
			}
		}

		if (simulation == null) {
			simulation = environment.createSimulation("sim1", scenario, controller);

			Config config = simulation.getConfig();
			config.setParameter("network", "inputNetworkFile", "%{scenario}/network.xml");
			config.setParameter("plans", "inputPlansFile", "%{scenario}/plans100.xml");
			config.setParameter("controler", "outputDirectory", "%{output}");
			config.setParameter("controler", "overwriteFiles", "deleteDirectoryIfExists");

			simulation.save();
			simulation.start();
		}

		while (simulation.isActive()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}

			Long iteration = simulation.getIteration();

			if (iteration == null) {
				System.out.println("Iteration: unknown");
			} else {
				System.out.println("Iteration: " + iteration);
			}
		}

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
	}

	private static void runRemotely() {
		try {
			JSch jsch = new JSch();
			jsch.addIdentity("~/.ssh/eth");

			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");


			Session session = jsch.getSession("shoerl", "pikelot.ivt.ethz.ch", 22);
			session.setConfig(config);

			try {
				session.connect();
				SSHUtils ssh = new SSHUtils(session);

				runInEnvironment(new SSHEnvironment("/nas/shoerl/mexec", ssh));
			} finally {
				session.disconnect();
			}
		} catch (JSchException e) {
			throw new RuntimeException("SSH initialization error");
		}
	}

	public static void runInEnvironment(Environment environment) {
		String localControllerPath = "/home/sebastian/Downloads/matsim";
		String localScenarioPath = "/home/sebastian/Downloads/matsim/examples/equil";

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

		Simulation simulation = null;

		if (environment.hasSimulation("sim1")) {
			simulation = environment.getSimulation("sim1");

			if (!simulation.isActive()) {
				environment.removeSimulation(environment.getSimulation("sim1"));
				simulation = null;
			}
		}

		if (simulation == null) {
			simulation = environment.createSimulation("sim1", scenario, controller);

			Config config = simulation.getConfig();
			config.setParameter("network", "inputNetworkFile", "%{scenario}/network.xml");
			config.setParameter("plans", "inputPlansFile", "%{scenario}/plans100.xml");
			config.setParameter("controler", "outputDirectory", "%{output}");
			config.setParameter("controler", "overwriteFiles", "deleteDirectoryIfExists");

			simulation.save();
			simulation.start();
		}

		while (simulation.isActive()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}

			Long iteration = simulation.getIteration();

			if (iteration == null) {
				System.out.println("Iteration: unknown");
			} else {
				System.out.println("Iteration: " + iteration);
			}
		}

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
	}
}
