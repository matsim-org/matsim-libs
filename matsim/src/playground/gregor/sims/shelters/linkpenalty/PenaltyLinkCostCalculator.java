package playground.gregor.sims.shelters.linkpenalty;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;


public class PenaltyLinkCostCalculator implements TravelCost{

	private final TravelTimeCalculator tc;
	private final ShelterInputCounter sc;
	public PenaltyLinkCostCalculator(TravelTimeCalculator tc, ShelterInputCounter sc) {
		this.tc = tc;
		this.sc = sc;
	}
	public double getLinkTravelCost(Link link, double time) {
		return this.tc.getLinkTravelTime(link, time) + this.sc.getLinkPenalty(link, time);
	}
	

}
