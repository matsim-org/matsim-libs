package playground.staheale.preprocess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkMergeDoubleLinks;
import org.matsim.core.network.algorithms.NetworkMergeDoubleLinks.MergeType;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class AdaptTeleAtlasNetwork {

	private static Logger log = Logger.getLogger(AdaptTeleAtlasNetwork.class);
	private Network network;

	public static void main(String[] args) {
		AdaptTeleAtlasNetwork adaptNetwork = new AdaptTeleAtlasNetwork();
		adaptNetwork.run();
		adaptNetwork.cleanNetwork();
		adaptNetwork.handleDoubleLinks();
		adaptNetwork.writeNetwork();
	}

	public void run() {
		final ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		List<Id> bikeWalkList = new ArrayList<Id>();
		Set<String> carMode = new HashSet<String>(Arrays.asList("car"));
		Set<String> bikeWalkMode = new HashSet<String>(Arrays.asList("bike", "walk"));
		Set<String> carBikeWalkMode = new HashSet<String>(Arrays.asList("car", "bike", "walk"));

		// read network
		log.info("Reading teleatlas network xml file...");
	    new MatsimNetworkReader(sc.getNetwork()).parse("./input/teleatlas2010network.xml.gz");
	    this.network = sc.getNetwork();
		log.info("Reading teleatlas network xml file...done");
		log.info("Initial network contains " +this.network.getLinks().size()+ " links.");
		log.info("Initial network contains " +this.network.getNodes().size()+ " nodes.");
	    
	    // fill bikeWalkList and adapt mode for links with "car, bike, walk" mode
	    for (Link l : this.network.getLinks().values()) {
	    	if (bikeWalkMode.equals(l.getAllowedModes())) {
	    		bikeWalkList.add(l.getId());
	    	}
	    	else if (carBikeWalkMode.equals(l.getAllowedModes())) {
	    		l.setAllowedModes(carMode);
	    	}
	    }
	    
	    log.info("bikeWalkList size is " +bikeWalkList.size());

	    // remove links without car mode
		log.info("Removing links without car mode...");
	    for (int i=0 ; i < bikeWalkList.size() ; i++) {
	    	this.network.removeLink(bikeWalkList.get(i));
	    }
		log.info("Removing links without car mode...done");
		log.info("Resulting network contains " +this.network.getLinks().size()+ " links.");
		
		// remove nodes without links attached to them
		log.info("Removing nodes that have no incoming or outgoing links attached to them...");
		List<Node> toBeRemoved = new ArrayList<Node>();
		for (Node node : this.network.getNodes().values()) {
			if ((node.getInLinks().size() == 0) && (node.getOutLinks().size() == 0)) {
				toBeRemoved.add(node);
			}
		}
		for (Node node : toBeRemoved) {
			this.network.removeNode(node.getId());
		}
		log.info("Removing nodes that have no incoming or outgoing links attached to them...done");
		log.info("Resulting network contains " +this.network.getNodes().size()+ " nodes.");
	}
	
	
	public void cleanNetwork() {
		new NetworkCleaner().run(this.network);
	}
	
	public void handleDoubleLinks() {
		MergeType mt = NetworkMergeDoubleLinks.MergeType.REMOVE;
		new NetworkMergeDoubleLinks(mt).run(this.network);
	}
	
	public void writeNetwork() {
		new NetworkWriter(this.network).write("./output/teleatlas2010networkSingleMode.xml.gz");
	}
	
}
