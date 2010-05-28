package playground.mzilske.vis;

import java.lang.reflect.InvocationTargetException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.mobsim.queuesim.QueueSimulationFactory;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;

public class ReallyLiveMain {
	
	public static void main(String[] args) throws InterruptedException, InvocationTargetException {

		String configFileName = "./examples/tutorial/config/example5-config.xml";
		
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(configFileName);
		loader.loadScenario();
		
		Scenario scenario = loader.getScenario();
		EventsManager events = new EventsManagerImpl();
		
		final EventsCollectingLiveServer server = new EventsCollectingLiveServer(scenario, events);
		
		QueueSimulation queueSimulation = (QueueSimulation) new QueueSimulationFactory().createMobsim(scenario, events);
		queueSimulation.addSnapshotWriter(server.getSnapshotReceiver());
				
		OTFHostConnectionManager hostConnectionManager = new OTFHostConnectionManager("Wurst", server);
		InjectableOTFClient client = new InjectableOTFClient();
		client.setHostConnectionManager(hostConnectionManager);
		//client.setSwing(true);
		client.run();
		
		queueSimulation.run();
		
	}

}
