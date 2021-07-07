/* *********************************************************************** *
 * project: org.matsim.*
 * PositionInfoBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.vis.snapshotwriters.VisVehicle;

/**
 * A builder for AgentSnapshotInfo objects that can be used by links with queue logic
 * @author dgrether
 */
final class EquiDistAgentSnapshotInfoBuilder extends AbstractAgentSnapshotInfoBuilder {

	EquiDistAgentSnapshotInfoBuilder( Scenario sc, SnapshotLinkWidthCalculator linkWidthCalculator ){
		super(sc, linkWidthCalculator);
	}

	
	@Override
	double calculateVehicleSpacing(double linkLength, double overallStorageCapacity,
			Collection<? extends VisVehicle> vehs) {
		double sum = 0. ;
		for ( VisVehicle veh : vehs ) {
			sum += veh.getSizeInEquivalents() ;
		}
		return linkLength / sum ;
	}

	@Override
	double calculateOdometerDistanceFromFromNode(
			double time, double linkLength, double freespeed, double spacing, double prevVehicleDistance, double remainingTravelTime
	) {
		return Double.isNaN(prevVehicleDistance) ? linkLength - (spacing / 2.0) : prevVehicleDistance - spacing;
	}
}
