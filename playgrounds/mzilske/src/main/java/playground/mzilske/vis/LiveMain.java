package playground.mzilske.vis;

import java.lang.reflect.InvocationTargetException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;

public class LiveMain {
	
	public static void main(String[] args) throws InterruptedException, InvocationTargetException {
//		 String fileName = "../../detailedEval/Net/network.xml.gz";
//		 String eventsFileName = "../../run950/it.1000/950.1000.events.txt.gz";
//		
//		 String fileName = "../../run749/749.output_network.xml.gz";
//		 String eventsFileName = "../../run749/it.1000/749.1000.events.txt.gz";
//		
		String networkFileName = "../../matsim/output/example5/output_network.xml.gz";
		String eventsFileName = "../../matsim/output/example5/ITERS/it.10/10.events.xml.gz";
		String populationFileName = "../../matsim/output/example5/output_plans.xml.gz";
		
		double snapshotPeriod = 60;
		SimulationConfigGroup simulationConfigGroup = new SimulationConfigGroup();
		ScenarioImpl scenario = new ScenarioImpl();
		
		new MatsimNetworkReader(scenario).readFile(networkFileName);
		new MatsimPopulationReader(scenario).readFile(populationFileName);
		
		EventsManagerImpl events = new EventsManagerImpl();
		
		// final BintreeServer server = new BintreeServer(scenario.getNetwork(), events, snapshotPeriod, simulationConfigGroup);
		final EventsCollectingLiveServer server = new EventsCollectingLiveServer(scenario, events, snapshotPeriod, simulationConfigGroup);
				
		OTFHostConnectionManager hostConnectionManager = new OTFHostConnectionManager("Wurst", server);
		
		InjectableOTFClient client = new InjectableOTFClient();
		client.setHostConnectionManager(hostConnectionManager);
		client.run();
		System.out.println("Reading...");
		new MatsimEventsReader(events).readFile(eventsFileName);
		
	}

}
