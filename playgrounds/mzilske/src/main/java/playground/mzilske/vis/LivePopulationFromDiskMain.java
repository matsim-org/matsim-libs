package playground.mzilske.vis;

import java.lang.reflect.InvocationTargetException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.SnapshotGenerator;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;
import org.matsim.vis.otfvis2.OTFVisClient;
import org.matsim.vis.otfvis2.OTFVisLiveServer;

public class LivePopulationFromDiskMain {
	
	public static void main(String[] args) throws InterruptedException, InvocationTargetException {
//		 String fileName = "../../detailedEval/Net/network.xml.gz";
//		 String eventsFileName = "../../run950/it.1000/950.1000.events.txt.gz";
//		
//		 String fileName = "../../run749/749.output_network.xml.gz";
//		 String eventsFileName = "../../run749/it.1000/749.1000.events.txt.gz";
//		
//		String networkFileName = "../../matsim/output/example5/output_network.xml.gz";
//		String eventsFileName = "../../matsim/output/example5/ITERS/it.10/10.events.xml.gz";
//		String populationFileName = "../../matsim/output/example5/output_plans.xml.gz";
		
		String networkFileName = "../../run951/951.output_network.xml.gz";
		String eventsFileName = "../../run951/it.100/951.100.events.txt.gz";
		String populationFileName = "../../run951/951.output_plans.xml.gz";
		
		double snapshotPeriod = 60;
		SimulationConfigGroup simulationConfigGroup = new SimulationConfigGroup();
		ScenarioImpl scenario = new ScenarioImpl();
		
		scenario.setPopulation(new PopulationOnDisk(scenario, DirectoryUtils.createTempDirectory()));
		
		new MatsimNetworkReader(scenario).readFile(networkFileName);
		new MatsimPopulationReader(scenario).readFile(populationFileName);
		
		EventsManagerImpl events = new EventsManagerImpl();
		
		
		final OTFVisLiveServer server = new OTFVisLiveServer(scenario, events);
		SnapshotGenerator snapshotGenerator = new SnapshotGenerator(scenario.getNetwork(), (int) snapshotPeriod, simulationConfigGroup); 
		snapshotGenerator.addSnapshotWriter(server.getSnapshotReceiver());
		events.addHandler(snapshotGenerator);
		
		OTFHostConnectionManager hostConnectionManager = new OTFHostConnectionManager("Wurst", server);
		
		OTFVisClient client = new OTFVisClient();
		client.setHostConnectionManager(hostConnectionManager);
		client.setSwing(false);
		client.run();
		
		System.out.println("Reading...");
		new MatsimEventsReader(events).readFile(eventsFileName);
		
	}

}
