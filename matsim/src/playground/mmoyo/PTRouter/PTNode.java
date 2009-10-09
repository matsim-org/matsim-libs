package playground.mmoyo.PTRouter;

import java.util.Arrays;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.network.NodeImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.transitSchedule.api.TransitRouteStop;
/**
 * Node with necessary data for the PT simulation
 * These nodes are installed in a different layer in independent paths according to each PtLine route
 *
 * @param idFather the "real" node from which is copied
 * @param idPTLine the PT line that exclusively travels through the node
 */
public class PTNode extends NodeImpl {
	//private TransitStopFacility transitStopFacility;
	private TransitRoute transitRoute ;
	private TransitRouteStop transitRouteStop;
	private int minutesAfterDeparture;
	private double[]arrDep;  
	private Link inStandardLink;
	private NodeImpl plainNode;
	
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

	public int getMinutesAfterDeparture() {
		return minutesAfterDeparture;
	}

	public void setMinutesAfterDeparture(int minutesAfterDeparture) {
		this.minutesAfterDeparture = minutesAfterDeparture;
	}

	public Link getInStandardLink() {
		return inStandardLink;
	}

	public void setInStandardLink(Link inStandardLink) {
		this.inStandardLink = inStandardLink;
	}

	public NodeImpl getPlainNode() {
		return plainNode;
	}

	public void setPlainNode(NodeImpl plainNode) {
		this.plainNode = plainNode;
	}

	public double[] getArrDep() {
		return arrDep;
	}

	public void setArrDep(double[] arrDep) {
		this.arrDep = arrDep;
	}
		
	/*
	 * This is an attempt to speed up the calculaton of waiting time in a transfer
	 * It must be determined if it is faster to do it here or at timetable class
	 */
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
		
}