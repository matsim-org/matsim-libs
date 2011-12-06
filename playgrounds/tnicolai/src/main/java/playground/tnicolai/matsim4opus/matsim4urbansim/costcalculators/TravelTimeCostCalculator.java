package playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

public class TravelTimeCostCalculator implements TravelCost {

	protected final TravelTime timeCalculator;
	
	public TravelTimeCostCalculator(final TravelTime timeCalculator){
		this.timeCalculator = timeCalculator;
	}
	
	@Override
	public double getLinkGeneralizedTravelCost(Link link, double time) {
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time);
		return travelTime;
	}

}
