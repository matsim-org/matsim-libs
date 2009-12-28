package playground.gregor.sims.confluent;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

public class CostCalculator implements TravelCost {

	private TravelTime tt;
	private LinkPenalty lpc;

	public CostCalculator(TravelTime tt, LinkPenalty lpc) {
		this.tt = tt;
		this.lpc = lpc;
	}
	
	public double getLinkTravelCost(Link link, double time) {
		double tc = tt.getLinkTravelTime(link, time);
		double penalty = this.lpc.getLinkCost(link);
		return  tc + penalty; 
	}

}
