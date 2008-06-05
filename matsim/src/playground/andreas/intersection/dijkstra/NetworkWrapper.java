package playground.andreas.intersection.dijkstra;

import org.apache.log4j.Logger;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

public class NetworkWrapper {
	
	final private static Logger log = Logger.getLogger(NetworkWrapper.class);	
	
	public static NetworkLayer wrapNetwork(NetworkLayer networkLayer){		
		
		NetworkLayer wrappedNetwork = new NetworkLayer();		
		int numberOfNodesGenerated = 0;
		int numberOfLinksGenerated = 0;
		
		for (Link link : networkLayer.getLinks().values()) {
			// TODO [an] Coordinates and type not set
			wrappedNetwork.createNode(link.getId().toString(), "0", "0", "type");
			numberOfNodesGenerated++;			
		}
		
		for (Node node : networkLayer.getNodes().values()) {
			for (Link inLink : node.getInLinks().values()) {
				for (Link outLink : node.getOutLinks().values()) {
					// TODO [an] FreeSpeed and some other parameters are not set
					wrappedNetwork.createLink(String.valueOf(numberOfLinksGenerated), inLink.getId().toString(), outLink.getId().toString(), String.valueOf((inLink.getLength() + outLink.getLength()) / 2), "100", "1000", "1", String.valueOf(numberOfLinksGenerated), "type");
					numberOfLinksGenerated++;
				}
			}
		}

		log.info("Generated " + numberOfNodesGenerated + " Nodes and " + numberOfLinksGenerated + " Links");
		
		// Debug only
//		NetworkWriter myNetworkWriter = new NetworkWriter(wrappedNetwork, "wrappedNetwork");
//		myNetworkWriter.write();
		
		return wrappedNetwork;		
	}

}
