package playground.toronto.sotr.routernetwork;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * Router network for Second Order Transit Router. Merges in TransitDataCache functionality,
 * losing the extra map/dictionary lookup by appending the departure and travel time data
 * directly to the links/turns
 * 
 * I decided to forego the regular Network interfaces for this one, as I intend to use special 
 * functionality not normally built into the network API. I also don't really care for the ID.
 * 
 * I'm going to try to save on memory by not storing nodes or links in redundant maps.
 * Hopefully I never have to iterate through them.
 * 
 * 
 * 
 * @author pkucirek
 *
 */
public class SOTRNetwork {
	
	private final static Logger log = Logger.getLogger(SOTRNetwork.class);
	
	private QuadTree<SOTRNode> quadTree;
	
	
	public SOTRNode createNode(TransitStopFacility stop){
		return new SOTRNode(stop);
	}
	
	public SOTRLink createWalkLink(SOTRNode fromNode, SOTRNode toNode){
		SOTRLink link = new SOTRLink(fromNode, toNode);
		fromNode.outgoingLinks.add(link);
		toNode.incomingLinks.add(link);
		return link;
	}
	
	public SOTRLink createInVehicleLink(SOTRNode fromNode, SOTRNode toNode, TransitRoute route){
		SOTRLink link = new SOTRLink(fromNode, toNode, route);
		fromNode.outgoingLinks.add(link);
		toNode.incomingLinks.add(link);
		return link;
	}
	
	public Collection<SOTRNode> getNearestNodes(final Coord coord, final double distance) {
		return this.quadTree.get(coord.getX(), coord.getY(), distance);
	}

	public SOTRNode getNearestNode(final Coord coord) {
		return this.quadTree.get(coord.getX(), coord.getY());
	}
		
	/**
	 * Creates a special virtual link connecting the origin or destination
	 * of a routing request to the various access and egress nodes. These special links do
	 * NOT get attached to this Network's links & nodes so as not to alter the Network 
	 * topology. In other words, the connectors exist inside the router only.
	 * 
	 * This is done for thread safety, since there can only be one network for all routing
	 * requests.
	 * 
	 * @param fromNode
	 * @param toNode
	 * @return
	 */
	public SOTRLink createVirtualConnectorLink(SOTRNode fromNode, SOTRNode toNode){
		return new SOTRLink(fromNode, toNode);
	}
	
}
