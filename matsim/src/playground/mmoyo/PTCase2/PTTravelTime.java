package playground.mmoyo.PTCase2;

import org.matsim.core.api.network.Link;
import org.matsim.core.router.util.TravelTime;
import playground.mmoyo.Validators.CostValidator;
import playground.mmoyo.Pedestrian.Walk;

/*
 * Calculates the travel time of each link depending on its type
 */
public class PTTravelTime implements TravelTime {
	double walkSpeed =  Walk.walkingSpeed();
	private PTTimeTable2 ptTimeTable = new PTTimeTable2();
	public CostValidator costValidator = new CostValidator();
	double walkTime=0;
	double travelTime=0;
	double waitingTime=0;
	double transTime=0;
	String type = "";
	
	public PTTravelTime(PTTimeTable2 ptTimeTable) {
		this.ptTimeTable = ptTimeTable; 
	}
	
	public double getLinkTravelTime(Link link, double time) {
		type = link.getType();
		if (type.equals("Transfer")){
			travelTime= transferTime(link,time); 
		}else if (type.equals("Walking")){
			travelTime= link.getLength()* walkSpeed;
		
		}else if (type.equals("Standard")){
			travelTime= ptTimeTable.GetTravelTime(link);
		
		}else if (type.equals("DetTransfer")){
			walkTime=link.getLength()* walkSpeed;
			waitingTime= transferTime(link, time+walkTime);
			travelTime= walkTime + waitingTime; 
			//--> decide if the departures will be saved in map or in Node
		}
		return travelTime;
	}

	private double transferTime(Link link, double t){
		transTime= ptTimeTable.GetTransferTime(link, t);
		if (transTime<0) {
			costValidator.pushNegativeValue(link.getId(), t, transTime);
			transTime = 600;
		}
		return transTime;  //->> Transfer factor
	}
}