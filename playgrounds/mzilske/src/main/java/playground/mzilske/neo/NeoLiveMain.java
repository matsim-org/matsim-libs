package playground.mzilske.neo;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.neo4j.graphdb.Transaction;

public class NeoLiveMain {

	public static void main(String[] args) throws InterruptedException, InvocationTargetException {

		String directory =  "output/neo";
		Map<String,String> config = new HashMap<String,String>();
		config.put("neostore.nodestore.db.mapped_memory","80M");
		config.put("neostore.relationshipstore.db.mapped_memory","750M");
		config.put("neostore.propertystore.db.mapped_memory","0M");
		config.put("neostore.propertystore.db.strings.mapped_memory","0M");
		config.put("neostore.propertystore.db.arrays.mapped_memory","0M");

		String eventsFileName = "../../matsim/output/example5/ITERS/it.10/10.events.xml.gz";

		double snapshotPeriod = 60;
		SimulationConfigGroup simulationConfigGroup = new SimulationConfigGroup();
		NeoScenario scenario = new NeoScenario(directory, config);
		try {
			Transaction tx = scenario.beginTx();
			try {
				EventsManagerImpl events = new EventsManagerImpl();


//				final OTFVisLiveServer server = new OTFVisLiveServer(scenario, events);
//				SnapshotGenerator snapshotGenerator = new SnapshotGenerator(scenario.getNetwork(), (int) snapshotPeriod, simulationConfigGroup); 
//				snapshotGenerator.addSnapshotWriter(server.getSnapshotReceiver());
//				events.addHandler(snapshotGenerator);
//				server.setSnapshotGenerator(snapshotGenerator);
//
//				OTFHostConnectionManager hostConnectionManager = new OTFHostConnectionManager("Wurst", new NeoOTFLiveServerTransactionWrapper(server, scenario));
//
//				OTFVisClient client = new OTFVisClient();
//				client.setHostConnectionManager(hostConnectionManager);
//				client.setSwing(false);
//				client.run();
//
//				System.out.println("Reading...");
//				new MatsimEventsReader(events).readFile(eventsFileName);
				
				int i=0;
				for (Link link : scenario.getNetwork().getLinks().values()) {
					i++;
					Node fromNode = link.getFromNode();
				//	Id fromId = fromNode.getId();
					Node toNode = link.getToNode();
			//		Id toId = toNode.getId();
//					Coord from = link.getFromNode().getCoord();
//					Coord to = link.getToNode().getCoord();
					if (i % 1000 == 0)
						System.out.println(
								""
							//	+ from 
							//	+ fromId
								+ fromNode
								+ " " 
							//	+ to 
							//	+ toId
								+ toNode
								+ " " 
								+ i);
						
				}
				
				tx.success();
			} finally {
				tx.finish();
			}
		} finally {
			scenario.shutdown();
		}
	}

}
