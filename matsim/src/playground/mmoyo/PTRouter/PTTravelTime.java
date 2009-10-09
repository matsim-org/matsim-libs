package playground.mmoyo.PTRouter;

import java.util.Arrays;
import org.matsim.api.core.v01.network.Link;	
import org.matsim.core.router.util.TravelTime;
import org.matsim.api.core.v01.network.Node;

/**
 * Calculates the travel time of each link depending on its type
 */
public class PTTravelTime implements TravelTime {
	private Link lastLink;
	private double lastTime;
	private double lastTravelTime;
	private double walkTime;
	private double waitingTime;
	private double travelTime;
	private PTValues ptValues = new PTValues(); 
	private double walkSpeed = ptValues.getAvgWalkSpeed();
	private byte aliasLink;
	
	//Map <Id, List<Double>> dynTravTimeIndex = new TreeMap <Id, List<Double>>();
	//Map <Id, List<Double>> dynTravTimeValue = new TreeMap <Id, List<Double>>();
	
	public PTTravelTime() {
		
	}
	
	/**
	 * Calculation of travel time for each link type:
	 * 0 Acces Link (distance*walk speed) + (veh departure - walk arrival)
	 * 1 Standard link: (toNode arrival- fromNode arrival)
	 * 2 Transfer link: (second veh departure - first veh arrival)
	 * 3 Detached transfer And Access: (distance*walk speed) + (veh departure - walk arrival)
	 * 4 Egress link : (distance * walk speed)
	 */
	public double getLinkTravelTime(final Link link, final double time) {
		if (lastLink==link && lastTime==time) return lastTravelTime;

		PTLink ptLink = (PTLink)link;
		aliasLink = ptLink.getAliasType();
		
		if (aliasLink == 3 || aliasLink == 0){ // DetTransfer || "Access"
			walkTime=link.getLength()* walkSpeed;
			waitingTime= getTransferTime(link.getToNode(), time+walkTime);
			travelTime= walkTime + waitingTime; 
		}else if (aliasLink == 2){  //  "Transfer"
			travelTime= getTransferTime(link.getToNode(),time); //2 minutes to allow the passenger walk between ptv's!!!!! 
		}else if (aliasLink ==1){   //  "Standard"
			travelTime = ptLink.getTravelTime() * 60; // stored in minutes, returned in seconds
		}else if (aliasLink ==4){  //"Egress" 
			travelTime= link.getLength()* walkSpeed;
		}
		
		lastLink= link;
		lastTime = time;
		lastTravelTime = travelTime;
		return travelTime;
	}

	/* Returns the waiting time in a transfer link head node after a given time //minutes */
	public double getTransferTime(Node node, double time){
	 	double nextDeparture = nextDepartureB(node ,time); 
		double transferTime= 0;
		if (nextDeparture>=time){
			transferTime= nextDeparture-time;
		}else{
			//wait till next day first departure
			transferTime= 86400-time+ ((PTNode)node).getArrDep()[0];
		}
		//in case if a invalid negative value, it should be catched or corrected
		if (transferTime<0) {
			//costValidator.pushNegativeValue(link.getId(), time, transTime);
			//transTime= 86400-time+ transTime;//first departure of next day
			transferTime=6000;
			//if (transTime<0) System.out.println("negative value at" + link.getId().toString() + " " + time);
		}
		return transferTime;
	}

	/**
	*A binary search returns the next departure in a node after a given time 
	*If the time is greater than the last departure, 
	*returns the first departure(of the next day)*/
	public double nextDepartureB(Node node,  double time){//,
		double[]arrDep= ((PTNode)node).getArrDep();
		int length = arrDep.length;
		int index =  Arrays.binarySearch(arrDep, time);
		if (index<0){
			index = -index;
			if (index <= length)index--; else index=0;	
		}else{
			if (index < (length-1))index++; else index=0;	
		}
		return arrDep[index];
	}

}
