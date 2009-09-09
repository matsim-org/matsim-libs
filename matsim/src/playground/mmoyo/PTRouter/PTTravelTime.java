package playground.mmoyo.PTRouter;

import org.matsim.api.core.v01.network.Link;	
import org.matsim.core.router.util.TravelTime;

/**
 * Calculates the travel time of each link depending on its type
 */
public class PTTravelTime implements TravelTime {
	private PTTimeTable ptTimeTable; 
	
	//Map <Id, List<Double>> dynTravTimeIndex = new TreeMap <Id, List<Double>>();
	//Map <Id, List<Double>> dynTravTimeValue = new TreeMap <Id, List<Double>>();
	
	public PTTravelTime(PTTimeTable ptTimeTable) {
		this.ptTimeTable = ptTimeTable; 
	}
	
	public double getLinkTravelTime(final Link link, final double time) {
		return this.ptTimeTable.getLinkTravelTime(link, time);
	}
}