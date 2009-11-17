package playground.mmoyo.PTRouter;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NodeImpl;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitStopFacility;
/**
 * Node with necessary data for the PT simulation
 * These nodes are installed in a different layer in independent paths according to each route
 *
 */
public class PTNode extends NodeImpl {
	private TransitRoute transitRoute ;
	private TransitLine transitLine;
	private TransitRouteStop transitRouteStop;
	private double[]arrDep;  
	private Link inStandardLink;
	private Node plainNode;
	
	public PTNode(final Id id, final Coord coord) {
		super(id, coord, null);
	}

	public TransitRoute getTransitRoute() {
		return transitRoute;
	}

	public void setTransitRoute(TransitRoute transitRoute) {
		this.transitRoute = transitRoute;
	}

	public TransitRouteStop getTransitRouteStop() {
		return transitRouteStop;
	}

	public void setTransitRouteStop(TransitRouteStop transitRouteStop) {
		this.transitRouteStop = transitRouteStop;
	}

	public Link getInStandardLink() {
		return inStandardLink;
	}

	public void setInStandardLink(Link inStandardLink) {
		this.inStandardLink = inStandardLink;
	}

	public Node getPlainNode() {
		return plainNode;
	}

	public void setPlainNode(Node plainNode) {
		this.plainNode = plainNode;
	}

	public TransitStopFacility getTransitStopFacility(){
		System.out.println ("id:" + this.getId());
		return this.transitRouteStop.getStopFacility();
	}

	public double[] getArrDep() {
		return arrDep;
	}

	public void setArrDep(double[] arrDep) {
		this.arrDep = arrDep;
	}
		
	/*
	 * This is an attempt to speed up the calculation of waiting time in a transfer
	 * It must be determined if it is faster to do it here or at timetable class
	 */
	/*
	public double getTransferTime (final double time){//,
		int length = arrDep.length;
		int index =  Arrays.binarySearch(arrDep, time);
		if (index<0){
			index = -index;
			if (index <= length)index--; else index=0;	
		}else{
			if (index < (length-1))index++; else index=0;	
		}
		double nextDeparture = arrDep[index];
		
		double transTime = nextDeparture-time;
		if (transTime<0){//wait till next day first departure
			transTime= 86400-time+ nextDeparture;
		}
		
		return transTime;
	}
	*/
	
	public TransitLine getTransitLine() {
		return transitLine;
	}

	public void setTransitLine(TransitLine transitLine) {
		this.transitLine = transitLine;
	}
		
}