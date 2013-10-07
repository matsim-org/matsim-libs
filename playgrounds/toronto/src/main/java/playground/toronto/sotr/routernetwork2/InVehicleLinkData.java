package playground.toronto.sotr.routernetwork2;

import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.pt.transitSchedule.api.TransitRoute;

/**
 * A (hopefully) thread-safe container of in-vehicle link data (departure times, travel times) used in
 * routing. This way, parallel copies of this link can just point to this one holder for the travel
 * time data to save memory.
 * 
 * @author pkucirek
 *
 */
public class InVehicleLinkData {
	
	private final StopGroupFacility fromStop;
	private final StopGroupFacility toStop;
	private final TransitRoute route;
	private final TreeSet<Double> departures;
	private final TreeMap<Double, Double> travelTimes;
	private final double defaultTravelTime; //Scheduled travel time. Should never change for the life of this object.
	
	public InVehicleLinkData(final TransitRoute route, final StopGroupFacility fromStop, final StopGroupFacility toStop,
			final double defaultTravelTime){
		this.route = route;
		this.fromStop = fromStop;
		this.toStop = toStop;
		
		this.departures = new TreeSet<Double>();
		this.travelTimes = new TreeMap<Double, Double>();
		this.defaultTravelTime = defaultTravelTime;
	}
	
	public StopGroupFacility getFromStop(){ return this.fromStop;}
	public StopGroupFacility getToStop() { return this.toStop;}
	public TransitRoute getRoute() { return this.route; }
	public TreeSet<Double> getDepartures() { return this.departures; }
	public TreeMap<Double, Double> getTravelTimes() {return this.travelTimes; }
	public double getDefaultTravelTime() { return this.defaultTravelTime; }
	
}
 