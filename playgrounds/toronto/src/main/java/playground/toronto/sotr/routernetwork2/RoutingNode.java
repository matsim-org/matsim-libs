package playground.toronto.sotr.routernetwork2;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Represents a node in the routing graph. Each <code>RoutingNode</code> is a meeting
 * of multiple stops at a single {@link Node}, allowing for transfers.
 * 
 * @author pkucirek
 *
 */
public class RoutingNode {
	
	protected List<RoutingLink> outgoingLinks;
	protected Coord coord;
	
	protected RoutingNode(Coord coord){
		this.coord = coord;
	}
	
	public Iterable<RoutingLink> getOutgoingLinks(){
		return this.outgoingLinks;
	}
	
}
