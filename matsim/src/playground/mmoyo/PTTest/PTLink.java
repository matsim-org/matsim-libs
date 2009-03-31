package playground.mmoyo.PTTest;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;

public class PTLink extends LinkImpl {
	private String idPTLine;
	private int nextDepature;
	private double travelTime;
	private double walkTime;

	public PTLink(Id id, Node from, Node to, NetworkLayer network,double length, double freespeed, double capacity, double permlanes) {
		super(id, from, to, network, length, freespeed, capacity, permlanes);
	}

	public void setTravelTime(double travelTime) {
		this.travelTime = travelTime;
	}

	public void setWalkTime(double walkTime) {
		this.walkTime = walkTime;
	}

	public String getIdPTLine() {
		return idPTLine;
	}

	public void setIdPTLine(String idPTLine) {
		this.idPTLine = idPTLine;
	}

	public int getNextDepature() {
		return nextDepature;
	}

	public void setNextDepature(int nextDepature) {
		this.nextDepature = nextDepature;
	}
	
	public double getTravelTime(){
		return this.travelTime;
	}
	
	public double getWalkTime(){
		return this.walkTime;
	}
	
	/*
	public double getTransferTime(double time){
		/*
		time= time + 600;  //add 2 minutes of tolerance to reach the next PTV
		double transTime= ptTimeTable.GetTransferTime(this, time);
		if (transTime<0)
			//costValidator.pushNegativeValue(link.getId(), t, transTime);
		return transTime;
		
		return 0;
	}	
	
	public double getDetTransferTime (double time){
		double walkTime= this.getWalkTime();
		double waitingTime = this.getTransferTime(time);
		return walkTime + waitingTime;  
	}
	
	*/

	
	
	
}// class
