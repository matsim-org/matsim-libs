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
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.vis.snapshotwriters.VisVehicle;

import javax.inject.Inject;
import java.util.Collection;


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


		return Math.min(
				curvedLength / overallStorageCapacity , // number of ``cells''
				curvedLength / sum  // the link may be more than ``full'' because of forward squeezing of stuck vehicles
				);
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
			/*
			 * In the old code version we had the problem that vehicles did not change position while changing the link,
			 * i.e. they had a positionEvent at time 10 at the junction coordinate (0,0) where the previous link ends,
			 * and the following link starts, then at time 11 they left the old link, entered the new link and had a new
			 * positionEvent at the very same coordinate (0,0) because this is the from coord of the second link.
			 *
			 * This is an attempt to let the vehicle proceed further onto the second link already at the first
			 * positionEvent on that link. Whereas previously the link change took 1 sec in which the vehicle did not
			 * change its position, we now have to redistribute that second over the whole link. Therefore we have to
			 * reduce the distance travelled per time step below what the freespeed of the link would suggest. Then we
			 * add that distance once to move the first vehicle position from the from coord of the link away onto
			 * the link.
			 */

			double distancePerSecond = curvedLength / ( freespeedTraveltime + 1 ); // the first position of the vehicle on the link is not at the from coordinate, but one time step onto the link. So we have one more time step we have to cater for by reducing the distance driven per time step.
			distanceFromFNode = (1. - ( remainingTravelTime / freespeedTraveltime)) * ( curvedLength - distancePerSecond ) + distancePerSecond ;

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

	public AgentSnapshotInfo.DrivingState calculateDrivingState(double length, double spacing, double lastDistanceToFromNode, double now, double freespeedTraveltime, double remainingTravelTime) {

		var distanceFromFNode = calculateFreespeedDistanceToFromNode(freespeedTraveltime, remainingTravelTime, length);

		return AgentSnapshotInfo.DrivingState.CONGESTED;
	}

	private double calculateFreespeedDistanceToFromNode(double freespeedTraveltime, double remainingTravelTime, double curvedLength) {

		if (freespeedTraveltime == 0) {
			return 0;
		}

		var result = (1.0 - (remainingTravelTime / freespeedTraveltime)) * curvedLength;
		return Math.min(result, 0.0);
	}

	private double calculateCongestedDinstanceToFromNode(double value) {return 0;}
}
