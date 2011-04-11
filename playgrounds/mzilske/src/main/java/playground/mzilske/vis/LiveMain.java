package playground.mzilske.vis;

import java.lang.reflect.InvocationTargetException;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.MobsimConfigGroupI;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.SnapshotGenerator;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;
import org.matsim.vis.otfvis2.OTFVisClient;
import org.matsim.vis.otfvis2.OTFVisLiveServer;

public class LiveMain {
	
	public static void main(String[] args) throws InterruptedException, InvocationTargetException {

//		 String fileName = "../../run749/749.output_network.xml.gz";
//		 String eventsFileName = "../../run749/it.1000/749.1000.events.txt.gz";
//		
		String networkFileName = "../../matsim/output/example5/output_network.xml.gz";
		String eventsFileName = "../../matsim/output/example5/ITERS/it.10/10.events.xml.gz";
		String populationFileName = "../../matsim/output/example5/wurst.xml";
		
//		String networkFileName = "../../network-ivtch/ivtch-osm.xml";
//		String eventsFileName = "../../run657/it.1000/1000.events.txt.gz";
//		String populationFileName = "../../run657/it.1000/1000.plans.xml.gz";
		
//		String networkFileName = "output/brandenburg/output_network.xml.gz";
//		String eventsFileName = "output/brandenburg/ITERS/it.10/10.events.txt.gz";
//		String populationFileName = "output/brandenburg/output_plans.xml.gz";
		
		double snapshotPeriod = 60;
		MobsimConfigGroupI simulationConfigGroup = new SimulationConfigGroup();
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(scenario).readFile(networkFileName);
		new MatsimPopulationReader(scenario).readFile(populationFileName);
		
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		
		
		final OTFVisLiveServer server = new OTFVisLiveServer(scenario, events);
		SnapshotGenerator snapshotGenerator = new SnapshotGenerator(scenario.getNetwork(), (int) snapshotPeriod, simulationConfigGroup); 
		snapshotGenerator.addSnapshotWriter(server.getSnapshotReceiver());
		events.addHandler(snapshotGenerator);
		server.setSnapshotGenerator(snapshotGenerator);
		
		OTFHostConnectionManager hostConnectionManager = new OTFHostConnectionManager("Wurst", server);
		
		OTFVisClient client = new OTFVisClient();
		client.setHostConnectionManager(hostConnectionManager);
		client.setSwing(false);
		client.run();
		
		System.out.println("Reading...");
		new MatsimEventsReader(events).readFile(eventsFileName);
		snapshotGenerator.finish();
		
	}

}
