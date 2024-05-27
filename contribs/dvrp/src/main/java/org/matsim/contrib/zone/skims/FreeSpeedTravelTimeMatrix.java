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
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.core.router.util.TravelTime;

/**
 * @author Michal Maciejewski (michalm)
 */
public class FreeSpeedTravelTimeMatrix implements TravelTimeMatrix {
	public static FreeSpeedTravelTimeMatrix createFreeSpeedMatrix(Network dvrpNetwork, ZoneSystem zoneSystem, DvrpTravelTimeMatrixParams params, int numberOfThreads,
		double qSimTimeStepSize) {
		return new FreeSpeedTravelTimeMatrix(dvrpNetwork, zoneSystem, params, numberOfThreads, new QSimFreeSpeedTravelTime(qSimTimeStepSize));
	}

	private final ZoneSystem zoneSystem;
	private final Matrix freeSpeedTravelTimeMatrix;
	private final SparseMatrix freeSpeedTravelTimeSparseMatrix;

	public FreeSpeedTravelTimeMatrix(Network dvrpNetwork, ZoneSystem zoneSystem, DvrpTravelTimeMatrixParams params, int numberOfThreads, TravelTime travelTime) {
		this.zoneSystem = zoneSystem;
		var centralNodes = ZoneSystemUtils.computeMostCentralNodes(dvrpNetwork.getNodes().values(), zoneSystem);
		var travelDisutility = new TimeAsTravelDisutility(travelTime);
		var routingParams = new TravelTimeMatrices.RoutingParams(dvrpNetwork, travelTime, travelDisutility, numberOfThreads);
		freeSpeedTravelTimeMatrix = TravelTimeMatrices.calculateTravelTimeMatrix(routingParams, centralNodes, 0);
		freeSpeedTravelTimeSparseMatrix = TravelTimeMatrices.calculateTravelTimeSparseMatrix(routingParams, params.maxNeighborDistance,
			params.maxNeighborTravelTime, 0).orElse(null);
	}

	@Override
	public int getTravelTime(Node fromNode, Node toNode, double departureTime) {
		if (fromNode == toNode) {
			return 0;
		}
		if (freeSpeedTravelTimeSparseMatrix != null) {
			int time = freeSpeedTravelTimeSparseMatrix.get(fromNode, toNode);
			if (time >= 0) {// value is present
				return time;
			}
		}
		return freeSpeedTravelTimeMatrix.get(zoneSystem.getZoneForNodeId(fromNode.getId()).orElseThrow(), zoneSystem.getZoneForNodeId(toNode.getId()).orElseThrow());
	}

	public int getZonalTravelTime(Node fromNode, Node toNode, double departureTime) {
		return freeSpeedTravelTimeMatrix.get(zoneSystem.getZoneForNodeId(fromNode.getId()).orElseThrow(), zoneSystem.getZoneForNodeId(toNode.getId()).orElseThrow());
	}
}
