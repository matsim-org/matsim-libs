package playground.dressler.ea_flow.alt;
/* *********************************************************************** *
 * project: org.matsim.*												   *	
 * FlowEdgeTraversalCalculator.java										   *
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

*//**
 * @author Matthias Rost (rost@mi.fu-berlin.de)
 *
 *//*

public interface FlowEdgeTraversalCalculator {
	
	*//**
	 * 
	 * @param currentFlow
	 * @return how many units can be send over some edge according to the currentFlow
	 *//*
	public int getRemainingForwardCapacityWithThisTravelTime(int currentFlow);
	
	*//**
	 *
	 * @param currentFlow
	 * @return the amount of flow which can be negated according to the currentFlow
	 *//*
	public int getRemainingBackwardCapacityWithThisTravelTime(int currentFlow);
	
	*//**
	 * 
	 * @return the minimal possible travel time over this edge
	 *//*
	public int getMinimalTravelTime();
	
	
	*//**
	 * @return the maximal possible travel time over this edge
	 *//*
	public int getMaximalTravelTime();
	
	
	*//**
	 * @param currentFlow
	 * @return the travel time for additional flow according to the current flow on this edge
	 *//*
	public Integer getTravelTimeForAdditionalFlow(int currentFlow);

	*//**
	 * @param currentFlow
	 * @return the travel time, that the upper bound of the flow induces 
	 *//*
	public Integer getTravelTimeForFlow(int currentFlow);
	
}

*/