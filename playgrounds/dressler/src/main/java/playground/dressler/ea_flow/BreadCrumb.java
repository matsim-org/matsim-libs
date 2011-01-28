package playground.dressler.ea_flow;

import playground.dressler.control.FlowCalculationSettings;
import playground.dressler.network.IndexedNodeI;

/* *********************************************************************** *
* project: org.matsim.*
* BreadCrumb.java
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


public interface BreadCrumb {	

		/**
		 * Returns a suitable PathStep for the Forward Search.
		 * The time the flow leaves the PathStep is set to arrival
		 * @param arrival
		 * @return a new PathStep
		 */
		PathStep createPathStepForward(VirtualNode arrival, FlowCalculationSettings settings);
		
		/**
		 * Returns a suitable PathStep for the Reverse Search.
		 * The time the flow entes the PathStep is set to start
		 * @param start
		 * @return a new PathStep
		 */
		PathStep createPathStepReverse(VirtualNode start, FlowCalculationSettings settings);
		
}
