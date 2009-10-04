package playground.mmoyo.PTRouter;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;

/**
 * Contains the information of departures for every station and PTLine
 * @param TimeTableFile path of file with departure information
 */
public class PTTimeTable{
	private Map<Id,Double> linkTravelTimeMap = new TreeMap<Id,Double>();
	private Map<Id,double[]> nodeDeparturesMap = new TreeMap<Id,double[]>();
	private Link lastLink;
	private String type;
	private double lastTime;
	private double lastTravelTime;
	private double walkTime;
	private double waitingTime;
	private double travelTime;
	private PTValues ptValues = new PTValues();
	private double walkSpeed;
	
	public PTTimeTable(){
		walkSpeed = ptValues.getAvgWalkSpeed();
	}
	
	/**
	 * Calculation of travel time for each link type:
	 * 1 Detached transfer And Access: (distance*walk speed) + (veh departure - walk arrival)
	 * 2 Transfer link: (second veh departure - first veh arrival)
	 * 3 Standard link: (toNode arrival- fromNode arrival)
	 * 4 Egress link : (distance * walk speed)
	 */
	public double getLinkTravelTime(final Link link, final double time){
		if (lastLink==link && lastTime==time) return lastTravelTime;
		type = ((LinkImpl) link).getType();
		if (type.equals("DetTransfer") || type.equals("Access")){
			walkTime=link.getLength()* walkSpeed;
			waitingTime= getTransferTime(link.getToNode().getId(), time+walkTime);
			travelTime= walkTime + waitingTime; 
		}else if (type.equals("Transfer")){
			travelTime= getTransferTime(link.getToNode().getId(),time)+ 120; //2 minutes to allow the passenger walk between ptv's 
		}else if (type.equals("Standard")){
			travelTime = linkTravelTimeMap.get(link.getId())*60; // stored in minutes, returned in seconds
			//travelTime= ((StandardLink)link).getTravelTime();
		}else if (type.equals("Egress")){
			travelTime= link.getLength()* walkSpeed;
		}
		
		lastLink= link;
		lastTime = time;
		lastTravelTime = travelTime;
		return travelTime;
	}
	
	/**
	 * Returns the waiting time in a transfer link head node after a given time //minutes */
	public double getTransferTime(Id idNode, double time){
	 	double nextDeparture = nextDepartureB(idNode ,time); 
		double transferTime= 0;
		if (nextDeparture>=time){
			transferTime= nextDeparture-time;
		}else{
			//wait till next day first departure
			transferTime= 86400-time+ nodeDeparturesMap.get(idNode)[0];
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
	public double nextDepartureB(Id idNode,  double time){//,
		double[]arrDep= nodeDeparturesMap.get(idNode);
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

	public void printDepartures(){
		for (Map.Entry <Id,double[]> departuresMap : nodeDeparturesMap.entrySet()){
			System.out.println("\n node:" + departuresMap.getKey());
			double[] departures = departuresMap.getValue();
			for (int x=0; x< departures.length; x++){
				System.out.print(departures[x] + " ");
			}
		}
	}

	public void setLinkTravelTimeMap(Map<Id, Double> linkTravelTimeMap) {
		this.linkTravelTimeMap = linkTravelTimeMap;
	}

	public void setNodeDeparturesMap(Map<Id, double[]> nodeDeparturesMap) {
		this.nodeDeparturesMap = nodeDeparturesMap;
	}
	
		
}
