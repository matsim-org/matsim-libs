package playground.gregor.sims.shelters.linkpenaltyII;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

public class LinkPenaltyTravelCost implements TravelCost {

	
	private final TravelTime tt;
	private final ShelterInputCounterLinkPenalty lp;

	public LinkPenaltyTravelCost(TravelTime tt, ShelterInputCounterLinkPenalty lp) {
		this.tt = tt;
		this.lp = lp;
	}
	
	public double getLinkTravelCost(Link link, double time) {
			return this.tt.getLinkTravelTime(link, time) + this.lp.getLinkTravelCost(link, time);
	}

}
