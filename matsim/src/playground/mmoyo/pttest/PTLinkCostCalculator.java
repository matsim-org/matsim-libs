package playground.mmoyo.pttest;

import org.matsim.network.Link;

/**
* Determines dynamically the weight of each link according to his type
* it is necessary for the router
* *
* @param ptTimeTableInfo Retrieves information of departures at nodes
*  
*/
public class PTLinkCostCalculator{
	private final int TRANSFER_RATE = 1200 ; // This must be changed for each passenger, for each trip distance
	private final int WALKING_RATE = 1; //We add a value  so that the agent avoid indiscriminate walking transfers in a PTSation
	PTTimeTableInfo ptTimeTableInfo;
	
	public PTLinkCostCalculator(PTTimeTableInfo ptTimeTableInfo) {
		this.ptTimeTableInfo = ptTimeTableInfo; 
	}
	
	public int cost(Link l, int time) {
		int cost=0;
		if (l.getType().equals("Standard")){
			cost= ptTimeTableInfo.travelTime(l,time); 
		}
		else if (l.getType().equals("Transfer")){
			//cost= TRANSFER_RATE + (ptTimeTableInfo2.NextDepartureR(l,time)-time);
			//Calculate dinamicaly the travel waiting time
			cost= TRANSFER_RATE; 
		}
		else if (l.getType().equals("Walking")){
				cost=WALKING_RATE;	
		}
		return cost;
	}
}
