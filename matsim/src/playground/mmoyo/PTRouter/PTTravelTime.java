package playground.mmoyo.PTRouter;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.util.TravelTime;

import playground.mmoyo.Validators.CostValidator;
/**
 * Calculates the travel time of each link depending on its type
 */
public class PTTravelTime implements TravelTime {
	double walkSpeed =  Walk.getAvgWalkSpeed();
	private PTTimeTable ptTimeTable; 
	public CostValidator costValidator = new CostValidator();
	private String type = "";
	double walkTime=0;
	double travelTime=0;
	double waitingTime=0;
	double transTime=0;
	
	public PTTravelTime(PTTimeTable ptTimeTable) {
		this.ptTimeTable = ptTimeTable; 
	}
	
	/**
	 * Calculation of travel time for each link type:
	 * Standard link: (toNode arrival- fromNode arrival)
	 * Walking link : (distance * walk speed)
	 * Transfer link: (second veh departure - first veh arrival)
	 * Detached transfer: (distance*walk speed) + (veh departure - walk arrival)  
	 */
	public double getLinkTravelTime(final Link link, final double time) {
		type = ((LinkImpl) link).getType();
		if (type.equals("DetTransfer") || type.equals("Access")){
			walkTime=link.getLength()* walkSpeed;
			waitingTime= transferTime(link, time+walkTime);
			travelTime= walkTime + waitingTime; 
			//--> decide if the departures will be saved in map or in Node 
		}else if (type.equals("Transfer")){
			travelTime= transferTime(link,time); 
		}else if (type.equals("Standard")){
			travelTime= ptTimeTable.getTravelTime(link);
		}else if (type.equals("Walking")){
			travelTime= (link.getLength()* walkSpeed);
		}
		return travelTime;
	}
	
	public double transferTime(final Link link, final double time){
		transTime= ptTimeTable.getTransferTime(link, time);
		if (transTime<0) {
			//costValidator.pushNegativeValue(link.getId(), time, transTime);
			//transTime= 86400-time+ transTime;//first departure of next day
			transTime=6000;
			//if (transTime<0) System.out.println("negative value at" + link.getId().toString() + " " + time);
		}
		return transTime;  //-> Transfer factor
	}
}