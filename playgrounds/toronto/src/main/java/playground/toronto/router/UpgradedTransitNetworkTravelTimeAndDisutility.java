package playground.toronto.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.vehicles.Vehicle;

/**
 * A significantly upgraded version of the base {@link TransitNetworkTravelTimeAndDisutility} calculator for transit, with several differences:
 *  <ul>
 *  	<li>Uses a {@link TransitDataCache} to remember as-simulated transit arrivals/departures, for use in congested transit simulation.</li>
 *  	<li>Changes the disutility calculation to explicitly use agent waiting time to boarding transit. </li>
 *  </ul>
 *  
 *  Right now, in-vehicle times are calculated to include dwell times at stops; which may cause problems with agents egressing from stops
 *  with long dwell times. This was done because there can only be one travel time for each {@link TransitRouterNetworkLink}. 
 * 
 * @author pkucirek
 *
 */
public class UpgradedTransitNetworkTravelTimeAndDisutility implements TravelTime, TravelDisutility{
	
	private static final Logger log = Logger.getLogger(UpgradedTransitNetworkTravelTimeAndDisutility.class);
	
	//inherited properties
	final static double MIDNIGHT = 24.0*3600;
	protected final TransitRouterConfig config;
	private Link previousLink = null;
	private double previousTime = Double.NaN;
	private double cachedWalkTime = Double.NaN;
	private double cachedInVehicleTime = Double.NaN;
	
	private TransitDataCache dataCache;
	
	public UpgradedTransitNetworkTravelTimeAndDisutility(final TransitDataCache cache, final TransitRouterConfig config){
		this.config = config;
		this.dataCache = cache;
	}
	
	
	@Override
	public double getLinkTravelDisutility(Link link, double time,
			Person person, Vehicle vehicle) {
		double cost;
			
		if (((TransitRouterNetworkLink) link).getRoute() == null) {
						
			double waitTime = getWaitTime((TransitRouterNetworkLink)link, time);
			double walkTime = getWalkTime((TransitRouterNetworkLink)link, time) + this.config.additionalTransferTime;
			double fare = getLinkFare((TransitRouterNetworkLink) link, time); //TODO use this in the utility calculation
			
			cost = -walkTime * this.config.getMarginalUtilityOfTravelTimeWalk_utl_s()
				       - waitTime * this.config.getMarginalUtiltityOfWaiting_utl_s()
				       - this.config.getUtilityOfLineSwitch_utl();
			
		}else{
			
			double fare = getLinkFare((TransitRouterNetworkLink) link, time);
			
			cost = - getInVehicleTravelTime((TransitRouterNetworkLink) link, time) * this.config.getMarginalUtilityOfTravelTimePt_utl_s() 
				       - link.getLength() * this.config.getMarginalUtilityOfTravelDistancePt_utl_m();
		}
				
		this.previousLink = link;
		this.previousTime = time;
		
		return cost;
	}

	private double getInVehicleTravelTime(TransitRouterNetworkLink link, double time){	
		if (link == this.previousLink && time == this.previousTime){
			return this.cachedInVehicleTime;
		}
		
		TransitRouteStop fromStop = link.getFromNode().stop;
		TransitRouteStop toStop = link.getToNode().stop;
		if (fromStop == null || toStop == null){
			log.error("Could find stop!");
			return Double.POSITIVE_INFINITY;
		}
		double arrivalTime = this.dataCache.getCurrentTravelTime(fromStop, time) + time; //Current time + DELTA time to next stop
		double departureTime = this.dataCache.getNextDepartureTime(toStop, arrivalTime); //Get the next departure from the end-stop at the time AFTER traveling to
		
		double ttime = departureTime - time;
		if (ttime < 0){
			return 0; //TODO this is a hack for now. I need to properly figure out how to deal with links with no service past a given hour.
		}
		
		this.cachedInVehicleTime = ttime;
		
		return ttime;
	}
	
	private double getWalkTime(TransitRouterNetworkLink link, double time){	
		if (link == this.previousLink && time == this.previousTime){
			return this.cachedWalkTime;
		}
		double distance = link.getLength();
		this.cachedWalkTime =  distance / this.config.getBeelineWalkSpeed();
		return distance / this.config.getBeelineWalkSpeed();
	}
	
	private double getWaitTime(TransitRouterNetworkLink link, double arrivalTime){
		TransitRouteStop s = link.toNode.getStop();
		if (s == null) return 0; //for egress links and/or end terminals
		double departureTime = this.dataCache.getNextDepartureTime(s, arrivalTime);
		
		double ttime = departureTime - arrivalTime;
		if (ttime < 0){
			return 0; //TODO this is a hack for now. I need to properly figure out how to deal with links with no service past a given hour.
		}
				
		return ttime;
	}
	
	private double getLinkFare(TransitRouterNetworkLink link, double time){
		
		//TODO implement fares 
		
		return 0;
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getLinkTravelTime(Link link, double time) {
		
		double ttime = 0;
		
		if (link instanceof TransitRouterNetworkLink){
			TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;

			if (wrapped.getRoute() == null){
				double waitTime = getWaitTime((TransitRouterNetworkLink)link, time);
				double walkTime = getWalkTime((TransitRouterNetworkLink)link, time);
				
				ttime = walkTime + waitTime;
			}else{
				ttime = getInVehicleTravelTime(wrapped, time);
			}
		}else{
			throw new UnsupportedOperationException("Can only get transit travel times for TransitRouterNetworkLinks!");
		}
		
		if (ttime < 0){
			log.error("Travel time was calculated as negative at time " + Time.writeTime(time) + "!");
		}
		
		this.previousLink = link;
		this.previousTime = time;
		
		return ttime;
	}

}
