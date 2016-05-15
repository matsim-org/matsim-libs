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

import java.util.Collection;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.vis.snapshotwriters.VisVehicle;


/**
 * Calculates the positions of all vehicles on this link according to the queue-logic: Vehicles are placed on the link
 * according to the ratio between the free-travel time and the time the vehicles are already on the link. If they could have
 * left the link already (based on the time), the vehicles start to build a traffic-jam (queue) at the end of the link.
 
 * @author dgrether
 * @author nagel
 *
 */
class QueueAgentSnapshotInfoBuilder extends AbstractAgentSnapshotInfoBuilder {

	@Inject
	QueueAgentSnapshotInfoBuilder(Scenario scenario, SnapshotLinkWidthCalculator linkWidthCalculator) {
		super(scenario, linkWidthCalculator);
	}

	@Override
	public double calculateVehicleSpacing(double curvedLength, double overallStorageCapacity,
			Collection<? extends VisVehicle> vehs) {
		// the length of a vehicle in visualization
		
		double sum = 0. ;
		for ( VisVehicle veh : vehs ) {
			sum += veh.getSizeInEquivalents() ;
		}


		double vehLen = Math.min( 
				curvedLength / overallStorageCapacity , // number of ``cells''
				curvedLength / sum  // the link may be more than ``full'' because of forward squeezing of stuck vehicles 
				);
		
		return vehLen;
	}
	

	@Override
	public double calculateOdometerDistanceFromFromNode(double curvedLength, double spacing,
			 double lastDistanceFromFNode, double now, double freespeedTraveltime, double remainingTravelTime) {
		double distanceFromFNode ;
		boolean isFirstVehicle = false;
		
		if (freespeedTraveltime == 0.0){
			distanceFromFNode = 0. ;
			// (insure against division by zero on non-physical links)
		}
		else {
			// we calculate where the vehicle would be with free speed.
			distanceFromFNode = (1. - (remainingTravelTime / freespeedTraveltime)) * curvedLength ;
			if ( distanceFromFNode < 0. ) {
				distanceFromFNode = 0. ;
			}
		}
		
		if (Double.isNaN(lastDistanceFromFNode)) {
			// (non-object-oriented way of "null" (?))
			isFirstVehicle = true;
			lastDistanceFromFNode = curvedLength ;
		}

		if (isFirstVehicle && distanceFromFNode >= lastDistanceFromFNode) {
			// first vehicle can be at the end of the link. amit May 2016
			// == --> if remainingTravelTime == 0 ; >= remaining travel time is zero or negative. 
			distanceFromFNode = lastDistanceFromFNode;
		} else if (distanceFromFNode >= lastDistanceFromFNode - spacing ) {  
			/* vehicle is already in queue or has to stay behind another vehicle
			 * -> position it directly after the last position
			 */
			distanceFromFNode = lastDistanceFromFNode - spacing;
		}

		//else just do nothing anymore
		return distanceFromFNode;
	}

	

	
}
