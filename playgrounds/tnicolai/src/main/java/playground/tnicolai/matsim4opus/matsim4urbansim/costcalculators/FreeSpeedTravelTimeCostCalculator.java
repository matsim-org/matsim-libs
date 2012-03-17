package playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelMinDisutility;

/**
 * This cost calulator is based on freespeed travel times 
 * tnicolai feb'12
 * 
 * @author thomas
 *
 */
public class FreeSpeedTravelTimeCostCalculator implements TravelMinDisutility, TravelDisutility{
	
	@Override
	public double getLinkTravelDisutility(Link link, double time) {
		return link.getLength() / link.getFreespeed();
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return link.getLength() / link.getFreespeed();
	}

}
