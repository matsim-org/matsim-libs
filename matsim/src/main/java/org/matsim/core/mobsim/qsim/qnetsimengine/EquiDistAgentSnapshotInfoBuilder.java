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

import org.matsim.api.core.v01.Scenario;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;

/**
 * A builder for AgentSnapshotInfo objects that can be used by links with queue logic
 * @author dgrether
 */
final class EquiDistAgentSnapshotInfoBuilder extends AbstractAgentSnapshotInfoBuilder {

	EquiDistAgentSnapshotInfoBuilder( Scenario sc, AgentSnapshotInfoFactory agentSnapshotInfoFactory ){
		super(sc, agentSnapshotInfoFactory);
	}

	
	@Override
	public double calculateVehicleSpacing(double linkLength, double numberOfVehiclesOnLink,
			double overallStorageCapacity) {
		return linkLength / numberOfVehiclesOnLink;
	}

	@Override
	public double calculateDistanceOnVectorFromFromNode2(double length, double spacing,
			 double lastDistanceFromFromNode, double now, double freespeedTraveltime, double remainingTravelTime) {
		double distanceOnVector = 0.;
		if (Double.isNaN(lastDistanceFromFromNode)){
			distanceOnVector = length - (spacing / 2.0);
		}
		else {
			distanceOnVector = lastDistanceFromFromNode - spacing;
		}
		return distanceOnVector;
	}

}
