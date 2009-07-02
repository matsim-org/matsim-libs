package playground.gregor.sims.run;

import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.evacuation.riskaversion.RiskCostCalculator;
import org.matsim.evacuation.socialcost.SocialCostCalculator;

import playground.gregor.sims.shelters.linkpenaltyII.ShelterInputCounterLinkPenalty;

public class ShelterLinkPenaltyRiskCostTravelCost implements TravelCost {


	private final TravelTimeCalculator ttc;
	private final ShelterInputCounterLinkPenalty slp;
	private final RiskCostCalculator rc;
	private final SocialCostCalculator sc;

	public ShelterLinkPenaltyRiskCostTravelCost(TravelTimeCalculator ttc, ShelterInputCounterLinkPenalty slp, RiskCostCalculator rc, SocialCostCalculator sc) {
		this.ttc = ttc;
		this.slp = slp;
		this.rc = rc;
		this.sc = sc;
	}
	
	public double getLinkTravelCost(LinkImpl link, double time) {
			return this.ttc.getLinkTravelTime(link, time) + this.slp.getLinkTravelCost(link, time) + this.rc.getLinkRisk(link) + this.sc.getSocialCost(link, time);
	}

}
