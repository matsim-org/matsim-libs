package playground.mmoyo.Pedestrian;

import org.matsim.core.api.network.Link;
import org.matsim.core.router.util.TravelTime;

public class PedTravelTime implements TravelTime {
	double walkSpeed =  Walk.walkingSpeed();
	
	public PedTravelTime() {
		 
	}

	public double getLinkTravelTime(Link link, double time) {
		double travelTime=0;
		travelTime= link.getLength()* walkSpeed;
		return travelTime;
	}

}