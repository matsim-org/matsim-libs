/* *********************************************************************** *
 * project: org.matsim.*
 * BreadCrumbEdgeForward.java
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


public class BreadCrumbEdge implements BreadCrumb {	
	
	/**
	 * Edge in a path
	 */
	private final IndexedLinkI edge;
	
	BreadCrumbEdge(IndexedLinkI edge) {
		this.edge = edge;
	}

	@Override
	public String toString() {
		return edge.getFromNode().getId().toString()+"-->" + edge.getToNode().getId().toString();
	}
	
	@Override
	public PathStep createPathStepForward(VirtualNode arrival,
			FlowCalculationSettings settings) {
		if (Debug.GLOBAL && Debug.STEP_CHECKS) {
			if (!(arrival instanceof VirtualNormalNode)) {
				throw new RuntimeException("Can only use edges with normal nodes!"); 
			}
		}
		
		// We are at a normal node and using this edge forward we come to where we used to be ...
		// So this is a residual step.
		
		int starttime = arrival.getRealTime() + settings.getLength(edge);			
		return new StepEdge(edge, starttime, arrival.getRealTime(), false);
	}

	@Override
	public PathStep createPathStepReverse(VirtualNode start,
			FlowCalculationSettings settings) {
		
		if (Debug.GLOBAL && Debug.STEP_CHECKS) {
			if (!(start instanceof VirtualNormalNode)) {
				throw new RuntimeException("Can only use edges with normal nodes!"); 
			}
		}
		
		// We are at a normal node. Using this arc forwards, we come to where we used to be.
		// So this is a forward step.
		
		int arrivaltime = start.getRealTime() + settings.getLength(edge);
		return new StepEdge(edge, start.getRealTime(), arrivaltime, true);
	}
};

