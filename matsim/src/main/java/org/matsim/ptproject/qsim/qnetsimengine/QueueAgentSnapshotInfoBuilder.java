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
package org.matsim.ptproject.qsim.qnetsimengine;

import org.matsim.api.core.v01.Scenario;


/**
 * Calculates the positions of all vehicles on this link according to the queue-logic: Vehicles are placed on the link
 * according to the ratio between the free-travel time and the time the vehicles are already on the link. If they could have
 * left the link already (based on the time), the vehicles start to build a traffic-jam (queue) at the end of the link.
 
 * @author dgrether
 * @author nagel
 *
 */
public class QueueAgentSnapshotInfoBuilder extends AbstractAgentSnapshotInfoBuilder {

	public QueueAgentSnapshotInfoBuilder(Scenario scenario) {
		super(scenario);
	}

	@Override
	public double calculateVehicleSpacing(double linkLength, double numberOfVehiclesOnLink,
			double storageCapacity, double bufferStorageCapacity) {
	// the length of a vehicle in visualization
		double vehLen = Math.min( 
				linkLength / (storageCapacity + bufferStorageCapacity), // all vehicles must have place on the link
				this.cellSize / this.storageCapacityFactor); // a vehicle should not be larger than it's actual size. yyyy why is that an issue? kai, apr'10
		return vehLen;
	}
	
	@Override
	public double calculateDistanceOnVectorFromFromNode(double length, double spacing,
			 double lastDistanceFromFromNode, double now, double freespeedTraveltime, double travelTime) {
		if (Double.isNaN(lastDistanceFromFromNode)) {
			lastDistanceFromFromNode = length;
		}
		double distanceFromFromNode = 0.0;
		
		if (freespeedTraveltime == 0.0){
			distanceFromFromNode = 0.0;
		}
		else {
			distanceFromFromNode = (travelTime / freespeedTraveltime) * length;
		}
		
		if (distanceFromFromNode > lastDistanceFromFromNode) { 
			/* vehicle is already in queue or has to stay behind another vehicle
			 * -> position it directly after the last position
			 */
			distanceFromFromNode = lastDistanceFromFromNode - spacing;
		} 
		//else just do nothing anymore
		return distanceFromFromNode;
	}

	

	
}
