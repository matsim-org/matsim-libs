package playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelDisutility;

/**
 * this cost calculator is an attempt to substitute travel distances by travel times
 * 
 * the average walk speed is 5km/h. this speed is independent of the type of road (motorway, sidewalk ...)
 * therefore, walking time can be considered to be linear. it directly correlates with travel distances
 * tnicolai feb'12
 * 
 * @author thomas
 *
 */
public class TravelWalkTimeCostCalculator implements TravelDisutility{
	
	private static final Logger log = Logger.getLogger(TravelWalkTimeCostCalculator.class);
	
	private double meterPerSecWalkSpeed;
	
	public TravelWalkTimeCostCalculator(double meterPerSecWalkSpeed){
		this.meterPerSecWalkSpeed = meterPerSecWalkSpeed;
	}
	
	/**
	 * uses network link lengths * walk speed as costs. 
	 * lengths usually are given in meter and walk speed in meter/sec
	 */
	@Override
	public double getLinkTravelDisutility(final Link link, final double time) {
		if(link != null){
			double secondWalkTime = link.getLength() / meterPerSecWalkSpeed;
			return secondWalkTime;
		}
		log.warn("Link is null. Returned 0 as walk time.");
		return 0.;
	}
}
