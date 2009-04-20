package playground.gregor.sims.shelters.linkpenalty;

import org.matsim.core.api.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.trafficmonitoring.AbstractTravelTimeCalculator;

public class PenaltyLinkCostCalculator implements TravelCost{

	private final AbstractTravelTimeCalculator tc;
	private final ShelterInputCounter sc;
	public PenaltyLinkCostCalculator(AbstractTravelTimeCalculator tc, ShelterInputCounter sc) {
		this.tc = tc;
		this.sc = sc;
	}
	public double getLinkTravelCost(Link link, double time) {
		return this.tc.getLinkTravelTime(link, time) + this.sc.getLinkPenalty(link, time);
	}

}
