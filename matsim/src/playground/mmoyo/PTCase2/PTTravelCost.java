package playground.mmoyo.PTCase2;

import org.matsim.router.util.TravelCost;
import org.matsim.network.Link;

public class PTTravelCost implements TravelCost {
	private final double TRANSFER_RATE = 1.0 ; //  TODO: Transfer function
	PTTimeTable2 ptTimeTable;
	
	public PTTravelCost(PTTimeTable2 ptTimeTable) {
		this.ptTimeTable = ptTimeTable; 
	}

	public double getLinkTravelCost(Link link, double time) {
		double cost =0;
		if (link.getType().equals("Transfer")){
			cost= ptTimeTable.GetTransferTime(link, time);
			//cost=1;
		}else if (link.getType().equals("Walking")){
			cost=0;
		}else if (link.getType().equals("Standard")){
			cost= ptTimeTable.GetTravelTime(link);
		}
		return cost;
	}
}