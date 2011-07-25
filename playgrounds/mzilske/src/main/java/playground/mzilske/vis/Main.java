package playground.mzilske.vis;

import java.lang.reflect.InvocationTargetException;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.MobsimConfigGroupI;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.vis.otfvis2.OTFVisClient;

public class Main {	

	public static void main(String[] args) throws InterruptedException, InvocationTargetException {
//		 String fileName = "../../detailedEval/Net/network.xml.gz";
//		 String eventsFileName = "../../run950/it.1000/950.1000.events.txt.gz";
//		
//		 String fileName = "../../run749/749.output_network.xml.gz";
//		 String eventsFileName = "../../run749/it.1000/749.1000.events.txt.gz";
//		
		String networkFileName = "../../network-ivtch/ivtch-osm.xml";
		String eventsFileName = "../../run657/it.1000/1000.events.txt.gz";
		
		double snapshotPeriod = 10;
		MobsimConfigGroupI simulationConfigGroup = new SimulationConfigGroup();
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(scenario).readFile(networkFileName);
		
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		
		final BintreeServer server = new BintreeServer(scenario.getNetwork(), events, snapshotPeriod, simulationConfigGroup);
		
		// final EventsCollectingServer server = new EventsCollectingServer(scenario.getNetwork(), events, snapshotPeriod, simulationConfigGroup);
		
		new MatsimEventsReader(events).readFile(eventsFileName);
		
		OTFVisClient client = new OTFVisClient();
		client.setServer(server);
		client.setSwing(false);
		
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
