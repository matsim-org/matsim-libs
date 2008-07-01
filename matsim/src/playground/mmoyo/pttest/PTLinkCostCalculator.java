package playground.mmoyo.pttest;

import org.matsim.network.Link;

public class PTLinkCostCalculator{
	private final int TRANSFER_RATE = 1200 ; // This must be changed for each passenger, for each trip distance
	private final int WALKING_RATE = 1; //We add a value  so that the agent avoid indiscriminate walking transfers in a PTSation
	PTTimeTableInfo ptTimeTableInfo;
	
	public PTLinkCostCalculator(PTTimeTableInfo ptTimeTableInfo) {
		this.ptTimeTableInfo = ptTimeTableInfo; 
	}
	
	public int Cost(Link l, int time, boolean isExtreme) {
		int cost=0;
		if (l.getType().equals("Standard")){
			cost= ptTimeTableInfo.TravelTime(l,time); 
		}
		else if (l.getType().equals("Transfer")){
			//cost= TRANSFER_RATE + (ptTimeTableInfo2.NextDepartureR(l,time)-time);
			//Calculate dinamically the travel waiting time
			cost= TRANSFER_RATE; 
		}
		else if (l.getType().equals("Walking")){
			//when the link is the start or the end of the trip, is set to 0
			if (!isExtreme) {
				cost=Integer.MAX_VALUE;
			}else{
				cost=WALKING_RATE;	
			}
		}
		return cost;
	}
}
