/* *********************************************************************** *
 * project: org.matsim.*
 * BreadCrumbResidualEdge.java
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

import playground.dressler.control.Debug;
import playground.dressler.control.FlowCalculationSettings;
import playground.dressler.network.IndexedLinkI;


public class BreadCrumbEdgeBackwards implements BreadCrumb {	
	
	/**
	 * Edge in a path
	 */
	private final IndexedLinkI edge;
	
	BreadCrumbEdgeBackwards(IndexedLinkI edge) {
		this.edge = edge;
	}
	
	@Override
	public String toString() {
		return edge.getFromNode().getId().toString()+"<-- " + edge.getToNode().getId().toString() + " backwards";
	}

	@Override
	public PathStep createPathStepForward(VirtualNode arrival,
			FlowCalculationSettings settings) {
		if (Debug.GLOBAL && Debug.STEP_CHECKS) {
			if (!(arrival instanceof VirtualNormalNode)) {
				throw new RuntimeException("Can only use edges with normal nodes!"); 
			}
		}
		
		// We are at a normal node and using this edge backwards we come to where we used to be ...
		// So this is a forward step.
		
		int starttime = arrival.getRealTime() - settings.getLength(edge);			
		return new StepEdge(edge, starttime, arrival.getRealTime(), true);
	}

	@Override
	public PathStep createPathStepReverse(VirtualNode start,
			FlowCalculationSettings settings) {
		if (Debug.GLOBAL && Debug.STEP_CHECKS) {
			if (!(start instanceof VirtualNormalNode)) {
				throw new RuntimeException("Can only use edges with normal nodes!"); 
			}
		}
		
		// We are at a normal node. Using this arc backwards, we come to where we used to be.
		// So this is a backwards step.
		
		int arrivaltime = start.getRealTime() - settings.getLength(edge);
		return new StepEdge(edge, start.getRealTime(), arrivaltime, false);
	}
};

