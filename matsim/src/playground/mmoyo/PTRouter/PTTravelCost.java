package playground.mmoyo.PTRouter;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;

/**
 * Calculates the cost of travel
 */
public class PTTravelCost implements TravelCost {
	PTTimeTable ptTimeTable;
	
	public PTTravelCost(final PTTimeTable ptTimeTable) {
		this.ptTimeTable = ptTimeTable; 
	}

	public double getLinkTravelCost(Link link, double time) {
		double cost;
		
		//////////////////////objective values
		double travelTime = ptTimeTable.getLinkTravelTime(link, time) ;
		double travelDistance = link.getLength();
		
		//////////////////////subjective values
		double timeValue = travelTime * 1;
		double distanceValue = travelDistance * 1;
		double transferValue = travelTime + 60;
		double walkValue = travelTime * 1;
		
		cost = travelTime;     
		return cost;
	}


	
	
	
	
}