/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.zone.skims;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.zone.SquareGridSystem;
import org.matsim.contrib.zone.ZonalSystems;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpTravelTimeMatrix {
	private final SquareGridSystem gridSystem;
	private final Matrix freeSpeedTravelTimeMatrix;

	public DvrpTravelTimeMatrix(Network dvrpNetwork, DvrpTravelTimeMatrixParams params, int numberOfThreads) {
		gridSystem = new SquareGridSystem(dvrpNetwork.getNodes().values(), params.getCellSize());
		var centralNodes = ZonalSystems.computeMostCentralNodes(dvrpNetwork.getNodes().values(), gridSystem);
		var travelTime = new FreeSpeedTravelTime();
		freeSpeedTravelTimeMatrix = TravelTimeMatrices.calculateTravelTimeMatrix(dvrpNetwork, centralNodes, 0,
				travelTime, new TimeAsTravelDisutility(travelTime), numberOfThreads);
	}

	public float getFreeSpeedTravelTime(Node fromNode, Node toNode) {
		return freeSpeedTravelTimeMatrix.get(gridSystem.getZone(fromNode), gridSystem.getZone(toNode));
	}
}
