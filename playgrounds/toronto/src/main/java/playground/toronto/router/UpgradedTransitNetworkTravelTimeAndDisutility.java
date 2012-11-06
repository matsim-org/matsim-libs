package playground.toronto.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.vehicles.Vehicle;

import playground.toronto.transitfares.FareCalculator;
import playground.toronto.transitfares.NullFareCalculator;

/**
 * <p>A significantly upgraded version of the base {@link TransitNetworkTravelTimeAndDisutility} calculator for transit, with several differences:
 *  <ul>
 *  	<li>Uses a {@link TransitDataCache} to remember as-simulated transit arrivals/departures, for use in congested transit simulation.</li>
 *  	<li>Changes the disutility calculation to explicitly use agent waiting time to boarding transit.</li>
 *  	<li>Includes the effects of transit fare disutility through the {@link FareCalculator} interface (will default to 0). </li>
 *  </ul></p>
 *  <p>Right now, in-vehicle times are calculated to include dwell times at stops; which may cause problems with agents egressing from stops
 *  with long dwell times. This was done because there can only be one travel time for each {@link TransitRouterNetworkLink}.</p> 
 * 
 * @author pkucirek
 *
 */
public class UpgradedTransitNetworkTravelTimeAndDisutility implements TravelTime, TransitTravelDisutility{
	
	private static final Logger log = Logger.getLogger(UpgradedTransitNetworkTravelTimeAndDisutility.class);
	
	//inherited properties
	final static double MIDNIGHT = 24.0*3600;
	protected final TransitRouterConfig config;
	private final FareCalculator fareCalc;
	private Link previousLink = null;
	private double previousTime = Double.NaN;
	private double cachedWalkTime = Double.NaN;
	private double cachedInVehicleTime = Double.NaN;
	
	private TransitDataCache dataCache;
	
	public UpgradedTransitNetworkTravelTimeAndDisutility(final TransitDataCache cache, final TransitRouterConfig config){
		this.config = config;
		this.dataCache = cache;
		this.fareCalc = new NullFareCalculator();
	}
	
	public UpgradedTransitNetworkTravelTimeAndDisutility(final TransitDataCache cache, final TransitRouterConfig config, final FareCalculator fareCalculator){
		this.config = config;
		this.dataCache = cache;
		this.fareCalc = fareCalculator;
	}
	
	
	@Override
	public double getLinkTravelDisutility(Link link, double time,
			Person person, Vehicle vehicle, final CustomDataManager dataManager) {
		double cost;
			
		if (((TransitRouterNetworkLink) link).getRoute() == null) {
				
			//TODO: This should be done dynamically, i.e. the waiting time should be based on time + walkTime
			//TODO: In this case, where does the additional transfer time get applied? -pkucirek Oct '12 
			double waitTime = getWaitTime((TransitRouterNetworkLink)link, time);
			double walkTime = getWalkTime((TransitRouterNetworkLink)link, time) + this.config.additionalTransferTime;
			double fareCost = this.fareCalc.getDisutilityOfTransferFare(person, vehicle, (TransitRouterNetworkLink)link, time);
			
			//TODO: Figure out how to use the CustomDataManager to store transit fares.
			
			cost = -walkTime * this.config.getMarginalUtilityOfTravelTimeWalk_utl_s()
				       - waitTime * this.config.getMarginalUtilityOfWaitingPt_utl_s()
				       - this.config.getUtilityOfLineSwitch_utl()
				       - fareCost;
			
		}else{
			
			double fareCost = this.fareCalc.getDisutilityOfInVehicleFare(person, vehicle, (TransitRouterNetworkLink)link, time);
			
			cost = - getInVehicleTravelTime((TransitRouterNetworkLink) link, time) * this.config.getMarginalUtilityOfTravelTimePt_utl_s() 
				       - link.getLength() * this.config.getMarginalUtilityOfTravelDistancePt_utl_m()
				       - fareCost;
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
	
	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		
		double ttime = 0;
		
		if (link instanceof TransitRouterNetworkLink){
			TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;

			if (wrapped.getRoute() == null){
				
				//TODO: This should be done dynamically, i.e. the waiting time should be based on time + walkTime @pkucirek Oct '12
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
