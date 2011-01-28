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
import playground.dressler.network.IndexedNodeI;


public class BreadCrumbIntoSink implements BreadCrumb {	
	

	/**
	 * default Constructor setting the arguments when using a Link 
	 * @param edge Link used
	 */
	public BreadCrumbIntoSink(){
	}
	
	/**
	 * Method returning a String representation of the StepEdge
	 */
	@Override
	public String toString(){
		return  "Into sink";
	}

	@Override
	public PathStep createPathStepForward(VirtualNode arrival,
			FlowCalculationSettings settings) {
		if (Debug.GLOBAL && Debug.STEP_CHECKS) {
			if (!(arrival instanceof VirtualNormalNode)) {
				throw new RuntimeException("Can only step into a sink from a normal node!"); 
			}
		}
		
		// We are at a normal node and were found by a sink.
		// This is a residual step.
		return new StepSinkFlow(arrival.getRealNode(), arrival.getRealTime(), false);
	}

	@Override
	public PathStep createPathStepReverse(VirtualNode start,
			FlowCalculationSettings settings) {
		if (Debug.GLOBAL && Debug.STEP_CHECKS) {
			if (!(start instanceof VirtualNormalNode)) {
				throw new RuntimeException("Can only step into a sink from a normal node!"); 
			}
		}
		
		// We are at a normal node and were found by a sink.
		// This is a forward step from the time in start to the sink (@0)
		return new StepSinkFlow(start.getRealNode(), start.getRealTime(), true);
	}


};

