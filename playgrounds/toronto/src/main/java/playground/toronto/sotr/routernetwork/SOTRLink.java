package playground.toronto.sotr.routernetwork;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitRoute;

/**
 * I decided against implementing the generic {@link Link} interface, since this class
 * needs to implement the departure and travel time caching from the previous iteration.
 * Otherwise, a whole lookup operation is needed and I need this router to be as fast
 * as possible.
 * 
 * @author pkucirek
 *
 */
public class SOTRLink {
	private TransitRoute route;
	
	protected SOTRNode fromNode;
	protected SOTRNode toNode;
	
	protected final TreeSet<Double> departures;
	//protected TreeSet<Double> defaultDepartures; //TODO figure out what to do with this
	protected TreeMap<Double, Double> travelTimes;
	protected double defaultTravelTime;
	public double length;
	
	public double pendingCost = Double.POSITIVE_INFINITY;
	public double pendingTime = Double.POSITIVE_INFINITY;
	public SOTRLink preceedingLink;
	
	/**
	 * Creates a walk or transfer link.
	 * 
	 * @param fromNode
	 * @param toNode
	 */
	protected SOTRLink(SOTRNode fromNode, SOTRNode toNode){
		this.init(fromNode, toNode);
		
		this.route = null;
		this.departures = null;
		//this.defaultDepartures = null;
		this.travelTimes = null;
	}
	
	/**
	 * Creates an in-vehicle link.
	 * 
	 * @param fromNode
	 * @param toNode
	 * @param route
	 */
	protected SOTRLink(SOTRNode fromNode, SOTRNode toNode, TransitRoute route){
		this.init(fromNode, toNode);
		
		this.route = route;
		this.departures = new TreeSet<Double>();
		//this.defaultDepartures = new TreeSet<Double>();
		this.travelTimes = new TreeMap<Double, Double>();
	}
	
	private void init(SOTRNode fromNode, SOTRNode toNode){
		this.fromNode = fromNode;
		this.toNode = toNode;
		
		this.preceedingLink = null;
	}
	
	public TransitRoute getRoute() { return this.route; }
	public SOTRNode getFromNode(){ return this.fromNode; }
	public SOTRNode getToNode() { return this.toNode; }
	
	//==============================================================================================================
	
	/**
	 * Returns an iterator for incoming turns.
	 * 
	 * @param allowUTurns True if U-Turns are permitted
	 * @return
	 */
	public Iterable<SOTRLink> getIncomingTurns(final boolean allowUTurns) {
		final Iterator<SOTRLink> base = this.fromNode.incomingLinks.iterator();
		
		return new Iterable<SOTRLink>() {
			@Override
			public Iterator<SOTRLink> iterator() {
				return new Iterator<SOTRLink>() {	
					
					SOTRLink nextLink;
					
					@Override
					public void remove() { throw new UnsupportedOperationException(); }
					
					@Override
					public SOTRLink next() { return nextLink; }
					
					@Override
					public boolean hasNext() {
						if (base.hasNext()){
							nextLink = base.next();
							if (allowUTurns) return true; //If u-turns are permitted, no need to skip u-turned links
							
							while (nextLink.fromNode == toNode){
								if (base.hasNext()){nextLink = base.next();}
								else{ return false;}
							}
							return true;
						}
						return false;
					}
				};
			}
		};
	}
	
	/**
	 * Returns an iterator for outgoing turns.
	 * 
	 * @param allowUTurns True if U-Turns are permitted
	 * @return
	 */
	public Iterable<SOTRLink> getOutgoingTurns(final boolean allowUTurns){
		
		final Iterator<SOTRLink> base = this.toNode.outgoingLinks.iterator();
		
		return new Iterable<SOTRLink>() {
			@Override
			public Iterator<SOTRLink> iterator() {
				return new Iterator<SOTRLink>() {	
					
					SOTRLink nextLink;
					
					@Override
					public void remove() { throw new UnsupportedOperationException(); }
					
					@Override
					public SOTRLink next() {
						return nextLink;
					}
					
					@Override
					public boolean hasNext() {
						if (base.hasNext()){
							nextLink = base.next();
							if (allowUTurns) return true; //If u-turns are permitted, no need to skip u-turned links
							
							while (nextLink.toNode == fromNode){
								if (base.hasNext()){nextLink = base.next();}
								else{ return false;}
							}
							return true;
						}
						return false;
					}
				};
			}
		};
	}
	
	//==============================================================================================================
	
	public double getNextDepartureTime(final double now){
		if (this.route == null)
			throw new UnsupportedOperationException("Cannot get departures for a walk or transfer link!");
		
		//TODO: Figure out when to use the default departures. Clearly, if a transit vehicle didn't make it, this
		// should be used, but how to test when this occurs?
		
		Double e = this.departures.ceiling(now);
		return (e == null) ? Double.POSITIVE_INFINITY : e; //Return infinity if no departures found past a given hour.
	}
	
	public double getNextTravelTime(final double now){
		if (this.route == null)
			throw new UnsupportedOperationException("Cannot get travel times for a walk or transfer link!");
		
		Entry<Double, Double> e = this.travelTimes.floorEntry(now);
		return (e == null) ? this.defaultTravelTime : e.getValue();
	}
}
