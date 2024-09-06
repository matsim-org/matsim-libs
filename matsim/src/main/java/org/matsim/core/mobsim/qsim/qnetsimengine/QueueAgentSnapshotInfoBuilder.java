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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.vis.snapshotwriters.VisVehicle;

import jakarta.inject.Inject;
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

	private static Logger log = LogManager.getLogger(QueueAgentSnapshotInfoBuilder.class);

	@Inject
	QueueAgentSnapshotInfoBuilder(Scenario scenario, SnapshotLinkWidthCalculator linkWidthCalculator) {
		super(scenario, linkWidthCalculator);
	}

	@Override
	double calculateVehicleSpacing(double curvedLength, double overallStorageCapacity,
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
	double calculateOdometerDistanceFromFromNode(
			double time, double curvedLength, double freespeed, double spacing, double prevVehiclesDistance, double remainingTravelTime
	) {

		// avoid divide by 0 and place vehicles at root of link when link has zero length
		if (curvedLength == 0) {
			return 0;
		}

		// we calculate where the vehicle would be with free speed.
		/*
		 * In the old code version we had the problem that vehicles did not change position while changing the link,
		 * i.e. they had a positionEvent at time 10 at the junction coordinate (0,0) where the previous link ends,
		 * and the following link starts, then at time 11 they left the old link, entered the new link and had a new
		 * positionEvent at the very same coordinate (0,0) because this is the from coord of the second link.
		 *
		 * The current version tries to simplify this logic by calculating the vehicle's position by simply using
		 * v = s/t -> s = v*t. Because t is the remaining time s is the distance to the toNode. We have to therefore
		 * subtract s from the overall curvedLength to receive the distance from fromNode:
		 * curvedLength - s -> curvedLength - v*t -> curvedLength - freespeed * remainingTravelTime
		 *
		 * This will produce a constant motion since the qsim will generate only a single position for the timestep in
		 * which the vehicle has its link enter, link leave event when crossing two links. The last position on the old
		 * link will be the position of the toNode. The first position generated on the new link is going to be the
		 * timestep after the link enter event for that link occurred. janek june'21
		 */
		var result = curvedLength - freespeed * remainingTravelTime;

		// if someone passes negative parameters place the vehicle at the beginning of the link
		if (result < 0.0) { result = 0.0; }

		// This is a bit weird if prevVehiclesDistance is NAN, this is the first vehicle. I guess this is a little uncommon
		// in Java, but this is how I found things.
		// Place a virtual vehicle at the end of the link + spacing so this vehicle can queue at the link's end
		if (Double.isNaN(prevVehiclesDistance)) {
			prevVehiclesDistance = curvedLength + spacing;
		}
		// if the freeflow position is further along the link as the prev vehicle, this vehicle has to queue behind the
		// previous.
		if (result >= prevVehiclesDistance - spacing) {
			result = prevVehiclesDistance - spacing;
		}

		return result;
	}
}
