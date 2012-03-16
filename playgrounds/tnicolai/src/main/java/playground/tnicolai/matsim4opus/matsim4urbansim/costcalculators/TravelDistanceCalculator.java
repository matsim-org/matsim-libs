package playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;

public class TravelDistanceCalculator implements TravelCost{

	private static final Logger log = Logger.getLogger(TravelDistanceCalculator.class);
	
	@Override
	public double getLinkGeneralizedTravelCost(final Link link, final double time) {
		if(link != null)
			return link.getLength();
		log.warn("Link is null. Returned 0 as distance.");
		return 0.;
	}
}
