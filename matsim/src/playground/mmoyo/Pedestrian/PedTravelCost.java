package playground.mmoyo.Pedestrian;

import org.matsim.core.api.network.Link;
import org.matsim.core.router.util.TravelCost;

public class PedTravelCost implements TravelCost {
	
	public PedTravelCost() {
		
	}
	
	public double getLinkTravelCost(Link link, double time) {
 		return 0;
	}
	
}