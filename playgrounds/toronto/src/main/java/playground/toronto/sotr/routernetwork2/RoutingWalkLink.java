package playground.toronto.sotr.routernetwork2;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * 
 * 
 * @author pkucirek
 *
 */
public class RoutingWalkLink extends RoutingLink {
	
	/**
	 * Enumeration of walk link types for convenience in cost calculators.
	 * 
	 * <ol>
	 * <li>ACCESS: Connects to the origin node</li>
	 * <li>EGRESS: Connects to the destination node</li>
	 * <li>TRANSFER: Connects two stops with a manual transfer (must be coded as 'TRANSFER'
	 * in the {@link Network}).</li>
	 * </ol>
	 * 
	 * @author pkucirek
	 *
	 */
	public static enum WalkType { 
		ACCESS, TRANSFER, EGRESS
	}
	
	private double length;
	private WalkType type;
	
	public RoutingWalkLink(RoutingNode fromNode, RoutingNode toNode, double length, WalkType type){
		super(fromNode, toNode);
		this.type = type;
		this.length = length;
	}
	
	public RoutingWalkLink(RoutingNode fromNode, RoutingNode toNode, WalkType type){
		super(fromNode, toNode);
		this.type = type;
		this.length = CoordUtils.calcDistance(this.fromNode.coord, this.toNode.coord);
	}
	
	public double getLength() { return this.length; }
	public WalkType getType() { return this.type; }
}
