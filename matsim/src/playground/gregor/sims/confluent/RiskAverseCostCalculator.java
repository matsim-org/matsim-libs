package playground.gregor.sims.confluent;

import org.matsim.core.api.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.evacuation.riskaversion.RiskCostCalculator;


public class RiskAverseCostCalculator implements TravelCost{

	private RiskCostCalculator rc;
	private LinkPenalty lpc;
	private TravelTime tt;

	public RiskAverseCostCalculator(TravelTime tt, LinkPenalty lpc, RiskCostCalculator rc){
		this.tt = tt;
		this.lpc = lpc;
		this.rc = rc;
	}
	
	public double getLinkTravelCost(Link link, double time) {
		return this.rc.getLinkRisk(link, time) + this.lpc.getLinkCost(link) + this.tt.getLinkTravelTime(link, time);
	}

}
