package playground.gregor.sims.run;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.evacuation.riskaversion.RiskCostCalculator;
import org.matsim.evacuation.socialcost.SocialCostCalculator;

import playground.gregor.sims.shelters.linkpenaltyII.ShelterInputCounterLinkPenalty;
import playground.gregor.sims.shelters.socialcost.ShelterInputCounterSocialCost;

public class ShelterSocialCostRiskCostTravelCost implements TravelCost {


	private final TravelTimeCalculator ttc;
	private final ShelterInputCounterSocialCost slp;
	private final RiskCostCalculator rc;
	private final SocialCostCalculator sc;

	public ShelterSocialCostRiskCostTravelCost(TravelTimeCalculator ttc, ShelterInputCounterSocialCost slp, RiskCostCalculator rc, SocialCostCalculator sc) {
		this.ttc = ttc;
		this.slp = slp;
		this.rc = rc;
		this.sc = sc;
	}
	
	public double getLinkTravelCost(Link link, double time) {
			return this.ttc.getLinkTravelTime(link, time) + this.slp.getShelterTravelCost(link, time) + this.rc.getLinkRisk(link); // + this.sc.getSocialCost(link, time);
	}

}
