package playground.mzilske.vis;

import java.lang.reflect.InvocationTargetException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;

public class Main {
	
	public static void main(String[] args) throws InterruptedException, InvocationTargetException {
//		 String fileName = "../../detailedEval/Net/network.xml.gz";
//		 String eventsFileName = "../../run950/it.1000/950.1000.events.txt.gz";
//		
//		 String fileName = "../../run749/749.output_network.xml.gz";
//		 String eventsFileName = "../../run749/it.1000/749.1000.events.txt.gz";
		
		String fileName = "../../matsim/output/example5/output_network.xml.gz";
		String eventsFileName = "../../matsim/output/example5/ITERS/it.10/10.events.xml.gz";
		
		double snapshotPeriod = 60;
		SimulationConfigGroup simulationConfigGroup = new SimulationConfigGroup();
		ScenarioImpl scenario = new ScenarioImpl();
		
		new MatsimNetworkReader(scenario).readFile(fileName);
		
		EventsManagerImpl events = new EventsManagerImpl();
		
		final EventsCollectingServer server = new EventsCollectingServer(scenario.getNetwork(), events, snapshotPeriod, simulationConfigGroup);
		
		new MatsimEventsReader(events).readFile(eventsFileName);
		
		OTFHostConnectionManager hostConnectionManager = new OTFHostConnectionManager("Wurst", server);
		
		InjectableOTFClient client = new InjectableOTFClient();
		client.setHostConnectionManager(hostConnectionManager);
		
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				System.out.println("Closing server...");
				server.close();
			}
			
		});
		
		client.run();
		
	}

}
