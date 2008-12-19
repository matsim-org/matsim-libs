package playground.mmoyo.PTCase2;

import org.matsim.router.util.TravelCost;
import org.matsim.network.Link;

public class PTTravelCost implements TravelCost {
	PTTimeTable2 ptTimeTable;
	PTTravelTime ptTravelTime;
	
	public PTTravelCost(PTTimeTable2 ptTimeTable) {
		this.ptTimeTable = ptTimeTable; 
		this.ptTravelTime= new PTTravelTime(ptTimeTable);
	}

	
	public double getLinkTravelCost(Link link, double time) {
		return ptTravelTime.getLinkTravelTime(link, time);
		
		/*
		//For the time being the route considers only travel, transfer and walking Time: 
		double cost =0;
		if (link.getType().equals("Transfer")){
			cost= ptTimeTable.GetTransferTime(link, time);
			//Borrar
			cost= link.getFromNode().getCoord().calcDistance(link.getToNode().getCoord());
		}else if (link.getType().equals("Walking")){
			cost= link.getLength()* WALKING_SPEED ;	
		}else if (link.getType().equals("Standard")){
			cost= ptTimeTable.GetTravelTime(link);
		}
		if (cost<0) {
			cost=0.1;
		}  //TODO: lok for negative values
		return link.getFromNode().getCoord().calcDistance(link.getToNode().getCoord());
		 */
	}
	
}