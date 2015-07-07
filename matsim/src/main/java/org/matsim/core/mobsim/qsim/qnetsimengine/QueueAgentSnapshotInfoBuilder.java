/* *********************************************************************** *
 * project: org.matsim.*
 * QueueAgentSnapshotInfoBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Scenario;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;


/**
 * Calculates the positions of all vehicles on this link according to the queue-logic: Vehicles are placed on the link
 * according to the ratio between the free-travel time and the time the vehicles are already on the link. If they could have
 * left the link already (based on the time), the vehicles start to build a traffic-jam (queue) at the end of the link.
 
 * @author dgrether
 * @author nagel
 *
 */
class QueueAgentSnapshotInfoBuilder extends AbstractAgentSnapshotInfoBuilder {

	QueueAgentSnapshotInfoBuilder(Scenario scenario, AgentSnapshotInfoFactory agentSnapshotInfoFactory) {
		super(scenario, agentSnapshotInfoFactory);
	}

	@Override
	public double calculateVehicleSpacing(double linkLength, double numberOfVehiclesOnLink,
			double overallStorageCapacity) {
	// the length of a vehicle in visualization

		double vehLen = Math.min( 
		linkLength / (overallStorageCapacity), // number of ``cells''
		linkLength / (numberOfVehiclesOnLink) ); // the link may be more than ``full'' because of forward squeezing of stuck vehicles
		
		return vehLen;
	}
	

	@Override
	public double calculateDistanceOnVectorFromFromNode2(double length, double spacing,
			 double lastDistanceFromFNode, double now, double freespeedTraveltime, double remainingTravelTime) {
		double distanceFromFNode ;
		
		if (freespeedTraveltime == 0.0){
			distanceFromFNode = 0. ;
			// (insure against division by zero on non-physical links)
		}
		else {
			// we calculate where the vehicle would be with free speed.
			distanceFromFNode = (1. - (remainingTravelTime / freespeedTraveltime)) * length ;
			if ( distanceFromFNode < 0. ) {
				distanceFromFNode = 0. ;
			}
		}
		
		if (Double.isNaN(lastDistanceFromFNode)) {
			// (non-object-oriented way of "null" (?))
			
			lastDistanceFromFNode = length ;
		}

		if (distanceFromFNode >= lastDistanceFromFNode - spacing ) { 
			/* vehicle is already in queue or has to stay behind another vehicle
			 * -> position it directly after the last position
			 */
			distanceFromFNode = lastDistanceFromFNode - spacing;
		}
		
		//else just do nothing anymore
		return distanceFromFNode;
	}

	

	
}
