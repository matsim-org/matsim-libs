package kid;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkUtils {
	
	private static Logger logger = Logger.getLogger(NetworkUtils.class);
	
	public static Network merge(Network n1, Network n2){
		logger.info("merge networks");
		Config config = new Config();
		config.addCoreModules();
		ScenarioImpl scenario = (ScenarioImpl)ScenarioUtils.createScenario(config);
//		scenario.getNetwork().getLinks().clear();
//		scenario.getNetwork().getNodes().clear();
		NetworkImpl network = scenario.getNetwork();
		logger.info("linkSize="+network.getLinks().values().size());
		logger.info("nodeSize="+network.getNodes().values().size());
		for(Node node : n1.getNodes().values()){
			if(!network.getNodes().containsKey(node.getId())){
				network.createAndAddNode(scenario.createId(node.getId().toString()),node.getCoord());
			}
		}
		for(Link link : n1.getLinks().values()){
			if(!network.getLinks().containsKey(link.getId())){
				Node fromNode = network.getNodes().get(link.getFromNode().getId());
				Node toNode = network.getNodes().get(link.getToNode().getId());
				network.createAndAddLink(scenario.createId(link.getId().toString()), fromNode, toNode, link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
			}
		}
		for(Node node : n2.getNodes().values()){
			if(!network.getNodes().containsKey(node.getId())){
				network.createAndAddNode(scenario.createId(node.getId().toString()),node.getCoord());
			}
		}
		for(Link link : n2.getLinks().values()){
			if(!network.getLinks().containsKey(link.getId())){
				Node fromNode = network.getNodes().get(link.getFromNode().getId());
				Node toNode = network.getNodes().get(link.getToNode().getId());
				network.createAndAddLink(scenario.createId(link.getId().toString()), fromNode, toNode, link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
			}
		}
		return network;
	}

}
