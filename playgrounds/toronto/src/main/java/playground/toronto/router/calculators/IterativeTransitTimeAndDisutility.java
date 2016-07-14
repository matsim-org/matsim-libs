package playground.toronto.router.calculators;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.vehicles.Vehicle;

import playground.toronto.transitfares.deprecated.FareCalculator;
import playground.toronto.transitfares.deprecated.NullFareCalculator;

/**
 * <p>Iterative {@link TransitRouterNetworkTravelTimeCost} calculator for transit, which utilizes the {@link TransitDataCache} to
 * calculate times as-executed in the previous iteration. Also includes the effects of fares through the use of the {@link FareCalculator}
 * (which by default is set to the <code>NullFareCalculator</code> which always returns '0'.</p>
 * 
 * <p>Note that this latest version (2) has been modified significantly to reflect the fact that the {@link TransitRouterImpl} implicitly
 * assumes that transit waiting times occur on in-vehicle links are are indistinguishable from dwell times at stops. </p>
 * 
 * @author pkucirek
 * @version Updated on 22.08.2013
 *
 */
public class IterativeTransitTimeAndDisutility implements TravelTime, TransitTravelDisutility {
	
	private static final Logger log = Logger.getLogger(IterativeTransitTimeAndDisutility.class);
	
	private final static double MIDNIGHT = 24.0*3600;
	protected final TransitRouterConfig config;
	private final FareCalculator fareCalc;
	private final TransitDataCache dataCache;
	
	//Cache since the MND requests time, then cost
	private Link cachedLink;
	private double cachedNow;
	private double cachedCost;
	
	public IterativeTransitTimeAndDisutility(final TransitDataCache cache, final TransitRouterConfig config){
		this.config = config;
		this.dataCache = cache;
		this.fareCalc = new NullFareCalculator();
	}
	
	public IterativeTransitTimeAndDisutility(final TransitDataCache cache, final TransitRouterConfig config, final FareCalculator fareCalculator){
		this.config = config;
		this.dataCache = cache;
		this.fareCalc = fareCalculator;
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle){
		
		this.cachedLink = link;
		this.cachedNow = time;
			
		TransitRouterNetworkLink trnLink = (TransitRouterNetworkLink) link;
		
		if (trnLink.getRoute() == null){
			//Transfer link
			double walkTime = this.getWalkTime(trnLink, time);
			double fareCost = this.fareCalc.getDisutilityOfTransferFare(person, vehicle, (TransitRouterNetworkLink)link, time);
				
			this.cachedCost = -walkTime * this.config.getMarginalUtilityOfTravelTimeWalk_utl_s()
								- this.config.getUtilityOfLineSwitch_utl()
								- fareCost;
				
			return walkTime;
		}else{
			//In-vehicle link
			double departureTime = this.getDepartureTime(trnLink, time);
			double waitTime = departureTime - time;
			double inVehicleTime = this.getTravelTime(trnLink, departureTime);
			double fareCost = this.fareCalc.getDisutilityOfInVehicleFare(person, vehicle, (TransitRouterNetworkLink)link, time);
				
			this.cachedCost = - waitTime * this.config.getMarginalUtilityOfWaitingPt_utl_s()
							- inVehicleTime * this.config.getMarginalUtilityOfTravelTimePt_utl_s()
							- link.getLength() * this.config.getMarginalUtilityOfTravelDistancePt_utl_m()
							- fareCost;
				
			return waitTime + inVehicleTime;
		}
	}
		
	@Override
	public double getLinkTravelDisutility(Link link, double time,
			Person person, Vehicle vehicle, final CustomDataManager dataManager) {
		
		if (link == this.cachedLink){
			if (time == this.cachedNow)
				return this.cachedCost;
		}
		
		this.getLinkTravelTime(link, time, person, vehicle);
		return this.cachedCost;
	}
	
	//----------------------------------------------------------------------
	
	@Override
	public double getTravelDisutility(Person person, Coord coord, Coord toCoord) {
		//  getMarginalUtilityOfTravelTimeWalk INCLUDES the opportunity cost of time.  kai, dec'12
		double initialCost = - (getTravelTime(person, coord, toCoord) * config.getMarginalUtilityOfTravelTimeWalk_utl_s());
		return initialCost;
	}
	
	@Override
	public double getTravelTime(Person person, Coord coord, Coord toCoord) {
		double distance = CoordUtils.calcEuclideanDistance(coord, toCoord);
		double initialTime = distance / config.getBeelineWalkSpeed();
		return initialTime;
	}
	
	//----------------------------------------------------------------------
	
	private double getTravelTime(TransitRouterNetworkLink link, double departureTime){
		return this.dataCache.getCurrentTravelTime(link.fromNode.stop, departureTime);
	}
	
	private double getWalkTime(TransitRouterNetworkLink link, double arrivalTime){	
		return link.getLength() / this.config.getBeelineWalkSpeed();
	}
	
	private double getDepartureTime(TransitRouterNetworkLink link, double arrivalTime){
		return this.dataCache.getNextDepartureTime(link.fromNode.stop, arrivalTime);
	}
	
	
	
	
}
