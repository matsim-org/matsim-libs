package playground.dressler.ea_flow.alt;
/* *********************************************************************** *
 * project: org.matsim.*												   *	
 * BowTravelTimeCost.java							 					   *
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

//java imports
import java.util.ArrayList;

//matsim imports
import org.matsim.api.core.v01.network.Link;
	
public class BowTravelTimeCost implements FlowEdgeTraversalCalculator
{
	
	protected Link link;
	protected int maxCapacity;
	protected ArrayList<BowEdge> bowEdges = new ArrayList<BowEdge>();
	protected final int bowEdgeSizeMinus1;
	
	protected SimpleEdgeTravelTimeCost sETTC;
	
	

	
	*//**
	 * represents a single BowEdge with a capacity, a traveltime
	 * and an intervall in which flow can be send over this edge.
	 * The bounds of the Intervall are including!
	 * 
	 * @author Matthias Rost (rost@mi.fu-berlin.de)
	 *
	 *//*
	public class BowEdge
	{
		public int capacity;
		public int traveltime;
		public int minFlowToUseThisEdge;
		public int maxFlowToUseThisEdge;
		
		
		public BowEdge(int capacity, int traveltime, int minFlowToUseThisEdge, int maxFlowToUseThisEdge)
		{
			this.capacity = capacity;
			this.traveltime = traveltime;
			this.minFlowToUseThisEdge = minFlowToUseThisEdge;
			this.maxFlowToUseThisEdge = maxFlowToUseThisEdge;
		}
	}
	
	public BowTravelTimeCost(Link link)
	{
		this.link = link;
		maxCapacity = (int)link.getCapacity(1.);
		if(maxCapacity == 1)
		{
			//add only 1 (then simple) edge
			bowEdges.add(new BowEdge(maxCapacity, (int)((double)link.getLength() / (double)link.getFreespeed(1.)), 0, maxCapacity));
		}
		else
		{
			int lowCap = maxCapacity / 2;
			int highCap = maxCapacity - lowCap;
			bowEdges.add(new BowEdge(lowCap, (int)((double)link.getLength() / (double)link.getFreespeed(1.)), 0,lowCap));
			bowEdges.add(new BowEdge(highCap, 2*(int)((double)link.getLength() / (double)link.getFreespeed(1.)),lowCap+1,maxCapacity));
		}
		bowEdgeSizeMinus1 = bowEdges.size()-1;
	}	
	
	
	public Integer getTravelTimeForFlow(int flow)
	{
		BowEdge bEdge = getBowEdge(flow);
		if(bEdge != null)
			return bEdge.traveltime;
		else 
			return null;
	}
	
	//we assume a monotonically increasing function capacity -> traveltime
	public int getMinimalTravelTime()
	{
		return bowEdges.get(0).traveltime;
	}
	
	//we assume a monotonically increasing function capacity -> traveltime
	public int getMaximalTravelTime()
	{
		return bowEdges.get(bowEdgeSizeMinus1).traveltime;
	}
	

	public int getRemainingForwardCapacityWithThisTravelTime(int currentFlow)
	{
		//because we want to augment some flow, we need to get the edge, that is able to
		//transport at least one unit more than the current flow
		BowEdge bEdge = getBowEdge(currentFlow + 1);
		if(bEdge == null)
			return 0;
		else return bEdge.maxFlowToUseThisEdge - currentFlow;
	}
	
	public int getRemainingBackwardCapacityWithThisTravelTime(int currentFlow)
	{
		BowEdge bEdge = getBowEdge(currentFlow-1);
		if(bEdge == null)
			return 0;
		else return currentFlow - bEdge.minFlowToUseThisEdge;
	}
	
	*//**
	 * returns the BowEdge that is used for the amount of "flow" 
	 * 
	 * @param flow the flow
	 * @return the correct BowEdge
	 *//*
	protected BowEdge getBowEdge(int flow)
	{
		if(flow > maxCapacity || flow < 0)
			return null;
		int l = 0; 
		int r = bowEdges.size()-1;
		int m = (l+r)/2;
		BowEdge bEdge;
		do
		{		
			bEdge = bowEdges.get(m);
			if(flow >= bEdge.minFlowToUseThisEdge && flow <= bEdge.maxFlowToUseThisEdge)
				return bEdge;
			else if(flow < bEdge.minFlowToUseThisEdge)
				r = m-1;
			else if(flow > bEdge.maxFlowToUseThisEdge)
				l = m+1;
			m = (l+r)/2;
		}while(l-r >= 0);
		//to make it complete
		return null;	
	}

	public Integer getTravelTimeForAdditionalFlow(int flow) {
		return getTravelTimeForFlow(flow + 1);
	}
}



*/