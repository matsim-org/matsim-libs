package playground.mmoyo.PTCase2;

import org.matsim.core.api.network.Link;
import org.matsim.core.router.util.TravelCost;

public class PTTravelCost implements TravelCost {
	PTTimeTable2 ptTimeTable;
	PTTravelTime ptTravelTime;
	
	public PTTravelCost(PTTimeTable2 ptTimeTable) {
		this.ptTimeTable = ptTimeTable; 
		this.ptTravelTime= new PTTravelTime(ptTimeTable);
	}

	
	public double getLinkTravelCost(Link link, double time) {
		//return ptTravelTime.getLinkTravelTime(link, time);
		return link.getLength()*5;
	}
	
}