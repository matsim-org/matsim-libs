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

import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.contrib.zone.SquareGridSystem;
import org.matsim.contrib.zone.ZonalSystems;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.router.util.TravelTime;

/**
 * @author Michal Maciejewski (michalm)
 */
public class FreeSpeedTravelTimeMatrix implements TravelTimeMatrix {
	public static FreeSpeedTravelTimeMatrix createFreeSpeedMatrix(Network dvrpNetwork, DvrpTravelTimeMatrixParams params, int numberOfThreads,
			double qSimTimeStepSize) {
		return new FreeSpeedTravelTimeMatrix(dvrpNetwork, params, numberOfThreads, new QSimFreeSpeedTravelTime(qSimTimeStepSize));
	}

	private final Matrix freeSpeedTravelTimeMatrix;
	private final SparseMatrix freeSpeedTravelTimeSparseMatrix;
	private final IdMap<Node, Zone> originZones;
	private final IdMap<Node, Zone> destinationZones;

	public FreeSpeedTravelTimeMatrix(Network dvrpNetwork, DvrpTravelTimeMatrixParams params, int numberOfThreads, TravelTime travelTime) {
		SquareGridSystem gridSystem = new SquareGridSystem(dvrpNetwork.getNodes().values(), params.cellSize);
		var centralNodes = ZonalSystems.computeMostCentralNodes(dvrpNetwork.getNodes().values(), gridSystem);
		var travelDisutility = new TimeAsTravelDisutility(travelTime);
		var routingParams = new TravelTimeMatrices.RoutingParams(dvrpNetwork, travelTime, travelDisutility, numberOfThreads);
		freeSpeedTravelTimeMatrix = TravelTimeMatrices.calculateTravelTimeMatrix(routingParams, centralNodes, 0);
		freeSpeedTravelTimeSparseMatrix = TravelTimeMatrices.calculateTravelTimeSparseMatrix(routingParams, params.maxNeighborDistance, 0);
		originZones = TravelTimeMatrices.findOriginDestinationZones(routingParams, centralNodes, 0, true);
		destinationZones = TravelTimeMatrices.findOriginDestinationZones(routingParams, centralNodes, 0, false);
	}

	@Override
	public int getTravelTime(Node fromNode, Node toNode, double departureTime) {
		if (fromNode == toNode) {
			return 0;
		}
		int time = freeSpeedTravelTimeSparseMatrix.get(fromNode, toNode);
		if (time >= 0) {// value is present
			return time;
		}
		return getZonalTravelTime(fromNode, toNode, departureTime);
	}

	public int getZonalTravelTime(Node fromNode, Node toNode, double departureTime) {
		var fromZone = originZones.get(fromNode.getId());
		var toZone = destinationZones.get(toNode.getId());
		return freeSpeedTravelTimeMatrix.get(fromZone, toZone);
	}
}
