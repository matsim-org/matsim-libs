package playground.dressler.ea_flow.alt;
/* *********************************************************************** *
 * project: org.matsim.*												   *	
 * SimpleEdgeTravelTimeCost.java									   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** 

package playground.dressler.ea_flow;

//matsim imports
import org.matsim.api.core.v01.network.Link;

public class SimpleEdgeTravelTimeCost implements FlowEdgeTraversalCalculator {

	protected Link link;
	protected final int capacity;	
	protected final int traveltime;
	
	public SimpleEdgeTravelTimeCost(Link link){
		if(link ==null){
			this.link=null;
			capacity= 1;
			traveltime=1;
		}else{
			this.link = link;
			capacity = (int)link.getCapacity(1.);
			traveltime = (int)((double)link.getLength() / (double)link.getFreespeed(1.));
		}	
	}
	
	
	public int getMaximalTravelTime() {
		return traveltime;
	}

	public int getMinimalTravelTime() {
		return traveltime;
	}

	public int getRemainingBackwardCapacityWithThisTravelTime(int currentFlow) {
		return currentFlow;
	}

	public int getRemainingForwardCapacityWithThisTravelTime(int currentFlow) {
		return capacity - currentFlow;
	}

	public Integer getTravelTimeForAdditionalFlow(int currentFlow) {
		if(currentFlow == capacity)
			return null;
		else return traveltime;
	}

	public Integer getTravelTimeForFlow(int currentFlow) {
		return traveltime;
	}

}*/