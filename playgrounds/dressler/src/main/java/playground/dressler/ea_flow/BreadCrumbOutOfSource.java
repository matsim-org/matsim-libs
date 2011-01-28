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


public class BreadCrumbOutOfSource implements BreadCrumb {	
	
	private final int t;
	
	public BreadCrumbOutOfSource(int t){
		this.t = t;
	}
	
	/**
	 * Method returning a String representation of the StepEdge
	 */
	@Override
	public String toString(){
		String s;
		
		s =  "Out of source @ " + t;

		return s;
	}

	@Override
	public PathStep createPathStepForward(VirtualNode arrival, 
			FlowCalculationSettings settings) {
		if (Debug.GLOBAL && Debug.STEP_CHECKS) {
			if (!(arrival instanceof VirtualSource)) {
				throw new RuntimeException("Can only step out of source in a source!"); 
			}
		}
		
		// Arrival is a source, and this was found from the normal node.
		// This is a residual step to the stored time.		
		return new StepSourceFlow(arrival.getRealNode(), t, false);
	}

	@Override
	public PathStep createPathStepReverse(VirtualNode start,
			FlowCalculationSettings settings) {
		if (Debug.GLOBAL && Debug.STEP_CHECKS) {
			if (!(start instanceof VirtualSource)) {
				throw new RuntimeException("Can only step out of source in a source!"); 
			}
		}
		
		// Start is a source, and this was found from outside.
		// This is a forward step to the stored time.
		
		return new StepSourceFlow(start.getRealNode(), t, true);
	}
};

