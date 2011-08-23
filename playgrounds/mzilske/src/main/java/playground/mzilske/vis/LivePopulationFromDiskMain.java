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
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.snapshotconsumingserver.SnapshotConsumingOTFServer;

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
		MobsimConfigGroupI simulationConfigGroup = new SimulationConfigGroup();
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		scenario.setPopulation(new PopulationOnDisk(scenario, DirectoryUtils.createTempDirectory()));
		
		new MatsimNetworkReader(scenario).readFile(networkFileName);
		new MatsimPopulationReader(scenario).readFile(populationFileName);
		
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		
		
		final SnapshotConsumingOTFServer server = new SnapshotConsumingOTFServer(scenario, events);
		SnapshotGenerator snapshotGenerator = new SnapshotGenerator(scenario.getNetwork(), (int) snapshotPeriod, simulationConfigGroup); 
		snapshotGenerator.addSnapshotWriter(server.getSnapshotReceiver());
		events.addHandler(snapshotGenerator);
		
		OTFClientLive.run(scenario.getConfig(), server);
		
		System.out.println("Reading...");
		new MatsimEventsReader(events).readFile(eventsFileName);
		
	}

}
