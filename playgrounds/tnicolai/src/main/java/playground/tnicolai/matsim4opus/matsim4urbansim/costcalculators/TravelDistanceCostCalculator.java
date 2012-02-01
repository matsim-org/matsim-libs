package playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;

/**
 * cost calculator for travel distances
 * @author thomas
 *
 */
public class TravelDistanceCostCalculator implements TravelCost{
	private static final Logger log = Logger.getLogger(TravelDistanceCostCalculator.class);
	
	/**
	 * uses network link lengths as costs. 
	 * lengths are usually given in meter
	 */
	@Override
	public double getLinkGeneralizedTravelCost(final Link link, final double time) {
		if(link != null)
//			return link.getLength();
			return link.getLength()/1000.; // tnicolai: experimental link lengths as km
		log.warn("Link is null. Returned 0 as link length.");
		return 0.;
	}
}
