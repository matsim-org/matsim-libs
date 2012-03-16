package playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelMinCost;

/**
 * This cost calulator is based on freespeed travel times 
 * tnicolai feb'12
 * 
 * @author thomas
 *
 */
public class FreeSpeedTravelTimeCostCalculator implements TravelMinCost, TravelCost{
	
	@Override
	public double getLinkGeneralizedTravelCost(Link link, double time) {
		return link.getLength() / link.getFreespeed();
	}

	@Override
	public double getLinkMinimumTravelCost(Link link) {
		return link.getLength() / link.getFreespeed();
	}

}
