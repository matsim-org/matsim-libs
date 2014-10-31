package matsimConnector.utility;

import matsimConnector.agents.Pedestrian;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class IdUtility {
	private static String nodeIdPrefix = "HybridNode_";
	
	public static Id<Link> createLinkId(Id<Node> fromId, Id<Node> toId) {
		return Id.create(fromId.toString() + "-->"+toId.toString(),Link.class);
	}
	
	public static Id<Node> createNodeId(int CANodeId) {
		return Id.create(nodeIdPrefix+CANodeId,Node.class);
	}
	
	public static int nodeIdToDestinationId(Id<Node> nodeId){
		return Integer.parseInt(nodeId.toString().substring(nodeIdPrefix.length()));
	}
	
	public static int linkIdToDestinationId(Id<Link> linkId){
		int beginIndex = linkId.toString().indexOf('>')+nodeIdPrefix.length()+1;
		return Integer.parseInt(linkId.toString().substring(beginIndex));
	}
	
	public static Id<Pedestrian> createPedestrianId(int pedestrianId){
		return Id.create(""+pedestrianId, Pedestrian.class);
	}
	
}
