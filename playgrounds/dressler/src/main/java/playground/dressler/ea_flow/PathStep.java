/* *********************************************************************** *
 * project: org.matsim.*
 * PathStep.java
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
 * *********************************************************************** */

package playground.dressler.ea_flow;

import playground.dressler.control.FlowCalculationSettings;


public interface PathStep {	

	/**
	 * Returns a shifted copy of a PathStep.
	 * The time the flow enters the PathStep is changed to newStart
	 * and the ArrivalTime is changed accordingly. 
	 * @param newStart
	 * @return a new PathStep
	 */
	PathStep copyShiftedToStart(int newStart);	
	
	/**
	 * Returns a shifted copy of a PathStep.
	 * The time the flow leaves the PathStep is changed to newArrival
	 * and the StartTime is changed accordingly. 
	 * @param newArrival
	 * @return a new PathStep
	 */
	PathStep copyShiftedToArrival(int newArrival);	
	
	/**
	 * Returns a shifted copy of a PathStep.
	 * All times (where applicable) are shifted by shift 
	 * @param shift
	 * @return a new PathStep
	 */
	PathStep copyShifted(int shift);
	
	/**
	 * Is the the forward or residual version of the step?
	 * @return true iff forward
	 */
	boolean getForward();

	/**
	 * Returns the node that the flow uses first.
	 * This is adjusted according to isForward().
	 * @return The first visited node.
	 */
	VirtualNode getStartNode();
	
	/**
	 * Returns the node that the flow uses second.
	 * This is adjusted according to isForward().
	 * @return The second visited node.
	 */
	VirtualNode getArrivalNode();

	/**
	 * Returns the time point when the flow enters.
	 * This is adjusted according to isForward().
	 * @return The second visited node.
	 */
	int getStartTime();

	/**
	 * Returns the time point when the flow leaves.
	 * This is adjusted according to isForward().
	 * @return The second visited node.
	 */
	int getArrivalTime();

	/**
	 * Checks if two PathEdges are identical in all fields and of the same type.
	 * @param other another PathStep 
	 * @return true iff identical
	 */
	boolean equals(PathStep other);
	
	/**
	 * Checks if two PathEdges are "identical" up to direction
	 * @param other another PathStep 
	 * @return true iff identical up 
	 */
	boolean equalsNoCheckForward(PathStep other);
	
	/**
	 * Checks if this is the residual of other.
	 * In particular, this must be a residual step and other a forward step for this to return true.
	 * @param other a forward PathEdge 
	 * @return true iff this is the Residual of other
	 */
	boolean isResidualVersionOf(PathStep other);
	
	/**
	 * Returns the cost of the step, assuming costs on the edges and 0 cost going into a sink.
	 * It accounts for primal/residual steps.
	 */
	int getCost();
	
	/**
	 * Checks if this is the other leave the same time/node-pair in the full (virtual) time-expanded network
	 * @param other a PathEdge 
	 * @return true iff they really leave the same time/node
	 */
	//boolean haveSameStart(PathStep other);
	
	String toString();
	
	/**
	 * String representation for saving a flow to a file
	 * @return Strin rep
	 */
	String print();
	
	/**
	 * Checks if this and other could be combined into one step, assuming the times match!
	 * @param other a PathEdge 
	 * @return true iff they describe the same pathstep. Times are not checked.
	 */
	boolean continuedBy(PathStep other);
	
	boolean isHoldover();
}