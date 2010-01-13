package playground.mmoyo.PTRouter;

import java.util.Arrays;
import org.matsim.api.core.v01.network.Link;	
import org.matsim.core.router.util.TravelTime;

/** Calculates the travel time of each link depending on its type */
public class PTTravelTime implements TravelTime {
	private Link lastLink;
	private double lastTime;
	private double lastTravelTime;
	private double waitingTime;
	private double travelTime;
	
	public PTTravelTime() {
	
	}
	
	/**Calculation of travel time for each link type:*/
	public double getLinkTravelTime(final Link link, final double time) {
		if (lastLink==link && lastTime==time) return lastTravelTime;
		PTLink ptLink = (PTLink)link;
		
		switch (ptLink.getAliasType()){
		case 4:    // DetTransfer
			waitingTime= getTransferTime((Station)link.getToNode(), time + ptLink.getWalkTime());
			travelTime= ptLink.getWalkTime() + waitingTime; 
			break;
		case 3:  //"Transfer"
			travelTime= getTransferTime((Station)link.getToNode(),time); //2 minutes to allow the passenger walk between ptv's!!!!! 
			break;
		case 2: //  "Standard"
			travelTime = ptLink.getTravelTime(); // it is stored in seconds
			break;
		case 1:   //access
			waitingTime= getTransferTime((Station)link.getToNode(), time + ptLink.getWalkTime());
			travelTime= ptLink.getWalkTime() + waitingTime; 
			break;
		case 5: //"Egress" 
			travelTime= ptLink.getWalkTime();
			break;
		default:
			 throw new NullPointerException("The link does not have a defined type" + link.getId());
		}

		lastLink = link;
		lastTime = time;
		lastTravelTime = travelTime;

		return travelTime;
	}

	/* Returns the waiting time in a transfer link head node after a given time //minutes */
	public double getTransferTime(Station node, double time){
	 	double nextDeparture = nextDepartureB(node, time); 
		double transferTime= 0;
		if (nextDeparture>=time){
			transferTime= nextDeparture-time;
		}else{
			//wait till next day first departure
			transferTime= 86400-time+ (node).getArrDep()[0];
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
	public double nextDepartureB(Station node,  double time){//,
		double[]arrDep= node.getArrDep();
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
