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


public class BreadCrumbOutOfSink implements BreadCrumb {	
	
	final private int t;

	/**
	 * default Constructor setting the arguments when using a Link 
	 * @param edge Link used
	 */
	public BreadCrumbOutOfSink(int t){
		this.t = t;
	}
	
	/**
	 * Method returning a String representation of the StepEdge
	 */
	@Override
	public String toString(){
		return  "Out of sink @ " + t;
	}

	@Override
	public PathStep createPathStepForward(VirtualNode arrival,
			FlowCalculationSettings settings) {
		if (Debug.GLOBAL && Debug.STEP_CHECKS) {
			if (!(arrival instanceof VirtualSink)) {
				throw new RuntimeException("Can only arrive at sink when coming from out of sink!"); 
			}
		}
		
		// We are at a sink and were found by a normal node.
		// This is a primal step starting at the stored time.
		return new StepSinkFlow(arrival.getRealNode(), t, true);
	}

	@Override
	public PathStep createPathStepReverse(VirtualNode start,
			FlowCalculationSettings settings) {
		if (Debug.GLOBAL && Debug.STEP_CHECKS) {
			if (!(start instanceof VirtualSink)) {
				throw new RuntimeException("Can only arrive at sink when coming from out of sink!"); 
			}
		}
		
		// We are at a sink and were found by a normal node.
		// This is a residual step going into the sink
		return new StepSinkFlow(start.getRealNode(), start.getRealTime(), false);
	}


};

