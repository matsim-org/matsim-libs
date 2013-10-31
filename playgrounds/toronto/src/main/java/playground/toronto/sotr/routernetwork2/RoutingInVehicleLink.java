package playground.toronto.sotr.routernetwork2;

import java.util.Map.Entry;

import org.matsim.pt.transitSchedule.api.TransitRoute;

/**
 * Routing link for in-vehicle links.
 * 
 * @author pkucirek
 *
 */
public class RoutingInVehicleLink extends AbstractRoutingLink {
	
	protected final InVehicleLinkData data;
	
	public RoutingInVehicleLink(AbstractRoutingNode fromNode, AbstractRoutingNode toNode, InVehicleLinkData data){
		super(fromNode, toNode);
		this.data = data;
	}
	
	public TransitRoute getRoute(){ return this.data.getRoute();}
	
	public double getNextDepartureTime(final double now){		
		//TODO: Figure out when to use the default departures. Clearly, if a transit vehicle didn't make it, this
		// should be used, but how to test when this occurs?
		
		Double e = this.data.getTravelTimes().ceilingKey(now);
		return (e == null) ? Double.POSITIVE_INFINITY : e; //Return infinity if no departures found past a given hour.
	}
	
	public double getNextTravelTime(final double now){		
		Entry<Double, Double> e = this.data.getTravelTimes().floorEntry(now);
		return (e == null) ? this.data.getDefaultTravelTime() : e.getValue();
	}
}
