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


public class BreadCrumbIntoSource implements BreadCrumb {	
	
	/**
	 * Method returning a String representation of the StepEdge
	 */
	@Override
	public String toString(){
		return  "into source";
	}

	@Override
	public PathStep createPathStepForward(VirtualNode arrival, FlowCalculationSettings settings) {
		if (Debug.GLOBAL && Debug.STEP_CHECKS) {
			if (!(arrival instanceof VirtualNormalNode)) {
				throw new RuntimeException("Can only step into a source from a normal node!"); 
			}
		}

		// Arrival is a normal node that was found from a source.
		// This is a primal step to arrival.
		return new StepSourceFlow(arrival.getRealNode(), arrival.getRealTime(), true);
	}

	@Override
	public PathStep createPathStepReverse(VirtualNode start, FlowCalculationSettings settings) {
		if (Debug.GLOBAL && Debug.STEP_CHECKS) {
			if (!(start instanceof VirtualNormalNode)) {
				throw new RuntimeException("Can only step into a source from a normal node!"); 
			}
		}

		// Start is a normal node that was found from a source.
		// This is a residual step into the source
		return new StepSourceFlow(start.getRealNode(), start.getRealTime(), false);
	}
};

