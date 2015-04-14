package playground.tobiqui.master;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class NetworkLinkRename {

	public static void main(String[] args) {
		String networkFileName = "E:/MA/workspace.bak/matsim/examples/siouxfalls-2014/Siouxfalls_network_PT.xml";
		String transitScheduleFileName = "E:/MA/workspace.bak/matsim/examples/siouxfalls-2014/Siouxfalls_transitSchedule.xml";
		String configFileName = "E:/MA/workspace.bak/matsim/examples/siouxfalls-2014/config_default.xml";
		String replacement = "-";
		String newNetworkFileName = "E:/MA/workspace.bak/matsim/examples/siouxfalls-2014/Siouxfalls_network_PT_renamed.xml";
		String newTransitScheduleFileName = "E:/MA/workspace.bak/matsim/examples/siouxfalls-2014/Siouxfalls_transitSchedule_renamed.xml";
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFileName));
		Scenario newScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFileName);
		Network network = scenario.getNetwork();
		Network newNetwork = newScenario.getNetwork();
		
		for (Node node : network.getNodes().values()){
			Id<Node> newNodeId = Id.createNodeId(node.getId().toString().replace("_", replacement));
			newNetwork.addNode(newNetwork.getFactory().createNode(newNodeId, node.getCoord()));
		}
		
		for (Link link : network.getLinks().values()){
			Id<Link> newLinkId = Id.createLinkId(link.getId().toString().replace("_", replacement));
			Id<Node> newFromNodeId = Id.createNodeId(link.getFromNode().getId().toString().replace("_", replacement));
			Id<Node> newToNodeId = Id.createNodeId(link.getToNode().getId().toString().replace("_", replacement));
			
			LinkFactoryImpl linkFactory = new LinkFactoryImpl();
			Node from = newNetwork.getNodes().get(newFromNodeId);
			Node to = newNetwork.getNodes().get(newToNodeId);
			
			Link newLink = linkFactory.createLink(newLinkId, from, to, newNetwork, link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
			newLink.setAllowedModes(link.getAllowedModes());
			newNetwork.addLink(newLink);
		}
		
		new NetworkCleaner().run(newNetwork);
	    new NetworkWriter(newNetwork).write(newNetworkFileName);
	    
	    
	    new TransitScheduleReader(scenario).readFile(transitScheduleFileName);
	    TransitSchedule transitSchedule = scenario.getTransitSchedule();
	    
	    for (TransitStopFacility transitStopFacility : transitSchedule.getFacilities().values()){
	    	Id<Link> newLinkId = Id.createLinkId(transitStopFacility.getLinkId().toString().replace("_", replacement));
	    	transitStopFacility.setLinkId(newLinkId);
	    }
	    
	    List<Id<Link>> newLinkIds = new ArrayList<Id<Link>>();
	    
	    for (TransitLine transitLine : transitSchedule.getTransitLines().values()){
	    	for (TransitRoute transitRoute : transitLine.getRoutes().values()){
	    		newLinkIds.clear();
	    		Iterator<Id<Link>> it = transitRoute.getRoute().getLinkIds().iterator();
	    		while(it.hasNext()){
	    			Id<Link> newLinkId = Id.createLinkId(it.next().toString().replace("_", replacement));
	    			newLinkIds.add(newLinkId);
	    		}
	    		Id<Link> newStartLinkId = Id.createLinkId(transitRoute.getRoute().getStartLinkId().toString().replace("_", replacement));
	    		Id<Link> newEndLinkId = Id.createLinkId(transitRoute.getRoute().getEndLinkId().toString().replace("_", replacement));
	    		
	    		transitRoute.getRoute().setLinkIds(newStartLinkId, newLinkIds, newEndLinkId);
	    	}
	    }
	    
	    new TransitScheduleWriter(transitSchedule).writeFile(newTransitScheduleFileName);
	}

}
