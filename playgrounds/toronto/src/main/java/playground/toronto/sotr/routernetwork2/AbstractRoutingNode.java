package playground.toronto.sotr.routernetwork2;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public abstract class AbstractRoutingNode {
	
	Set<AbstractRoutingLink> outgoingLinks;
	
	public AbstractRoutingNode(){
		this.outgoingLinks = new HashSet<AbstractRoutingLink>();
	}
	
	public Iterable<AbstractRoutingLink> getOutgoingLinks(){
		return new Iterable<AbstractRoutingLink>() {
			@Override
			public Iterator<AbstractRoutingLink> iterator() { return outgoingLinks.iterator(); }
		};
	}
	
	/**
	 * Gets the stop facility represented by this node, and served by the given transit
	 * route & line. These two arguments are needed for implementations which aggregate
	 * multiple {@link TransitStopFacility}s into one routing node.
	 * @param line
	 * @param route
	 * @return
	 */
	public abstract TransitStopFacility getStopFacility(TransitLine line, TransitRoute route);
	
	public abstract Coord getCoord();
	
	final RoutingNodeCopy getCopy(){
		return new RoutingNodeCopy(this);
	}
	
	final static class RoutingNodeCopy extends AbstractRoutingNode {
		
		private final AbstractRoutingNode base;
		
		RoutingNodeCopy(AbstractRoutingNode original){
			this.base = original;
		}
		
		@Override
		public TransitStopFacility getStopFacility(TransitLine line,
				TransitRoute route) {
			return base.getStopFacility(line, route);
		}

		@Override
		public Coord getCoord() {
			return base.getCoord();
		}
		
	}
}
