package playground.mmoyo.PTCase2;

import org.matsim.network.Link;
import org.matsim.router.util.TravelTime;
import org.matsim.utils.misc.Time;

public class PTTravelTime implements TravelTime {
	private final double WALKING_SPEED = 0.75; //   0.75m/s   5 km/h  human speed? 
	private PTTimeTable2 ptTimeTable;
	
	public PTTravelTime(PTTimeTable2 ptTimeTable) {
		this.ptTimeTable = ptTimeTable; 
	}
	
	//minutes
	public double getLinkTravelTime(Link link, double time) {
		double cost=0;
		if (link.getType().equals("Transfer")){
			cost= ptTimeTable.GetTransferTime(link, time);
		}else if (link.getType().equals("Walking")){
			double unit =1; 
			cost=(link.getLength()* unit)/WALKING_SPEED ;	
		}else if (link.getType().equals("Standard")){
			cost= ptTimeTable.GetTravelTime(link);
		}
		return cost;
	}
}