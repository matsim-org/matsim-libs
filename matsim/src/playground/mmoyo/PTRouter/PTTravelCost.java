package playground.mmoyo.PTRouter;

import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

/**
 * Calculates the cost of travel in different travel zones of the city
 */
public class PTTravelCost implements TravelCost {
	PTTimeTable2 ptTimeTable;
	TravelTime ptTravelTime;
	
	public PTTravelCost(final PTTimeTable2 ptTimeTable) {
		this.ptTimeTable = ptTimeTable; 
		this.ptTravelTime= new PTTravelTime(ptTimeTable);
	}

	public double getLinkTravelCost(LinkImpl link, double time) {
		//return 1;
		//return link.getLength();
		return ptTravelTime.getLinkTravelTime(link, time);
		//--> Compare fare zone from toNode and fromNode. If They are different then a extra cost is to be charged 
	}
	
}