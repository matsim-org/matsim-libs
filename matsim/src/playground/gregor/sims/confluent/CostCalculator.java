package playground.gregor.sims.confluent;

import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

public class CostCalculator implements TravelCost {

	private TravelTime tt;
	private LinkPenalty lpc;

	public CostCalculator(TravelTime tt, LinkPenalty lpc) {
		this.tt = tt;
		this.lpc = lpc;
	}
	
	public double getLinkTravelCost(LinkImpl link, double time) {
		double tc = tt.getLinkTravelTime(link, time);
		double penalty = this.lpc.getLinkCost(link);
		return  tc + penalty; 
	}

}
