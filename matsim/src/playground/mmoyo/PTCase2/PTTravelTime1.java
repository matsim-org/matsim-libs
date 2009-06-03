package playground.mmoyo.PTCase2;

import org.matsim.core.api.network.Link;
import org.matsim.core.router.util.TravelTime;

public class PTTravelTime1 implements TravelTime {
	
	public PTTravelTime1() {

	}
	
	public double getLinkTravelTime(Link link, double time) {
		return 1;
	}

	
}