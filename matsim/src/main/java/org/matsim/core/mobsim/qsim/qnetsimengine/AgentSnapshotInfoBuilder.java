/* *********************************************************************** *
 * project: org.matsim.*
 * AgentSnapshotInfoBuilder
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
import java.util.Queue;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;


/**
 * A builder for AgentSnapshotInfo objects that can be used by links with queue logic
 * 
 * @author dgrether
 *
 */
interface AgentSnapshotInfoBuilder {

	int positionVehiclesFromTransitStop(Collection<AgentSnapshotInfo> positions, Link link,
			Queue<QVehicle> transitVehicleStopQueue, int cnt2);

	int positionVehiclesFromWaitingList(Collection<AgentSnapshotInfo> positions, Link link,
			int cnt2, Queue<QVehicle> waitingList);

	int positionAgentsInActivities(Collection<AgentSnapshotInfo> positions, Link link,
			Collection<MobsimAgent> values, int cnt2);

	/**
 	 *  Adds AgentSnapshotInfo instances to the Collection given as parameter for a queue-logic object, e.g. located on a specific link - the curve. A queue-logic object
	 * can be a QueueLinkImpl or a QueueLane instance or something else. The length of the link/curve can be longer than the euclidean distance between the 
	 * start and the end coordinate of the curve. 
	 * @param positions The Collection in that the AgentSnapshotInfo instances are inserted.
	 */
	void positionAgentOnLink(final Collection<AgentSnapshotInfo> positions, Coord startCoord, Coord endCoord, double lengthOfCurve, double eucledianLength, QVehicle veh, 
			double distanceFromFromNode,	Integer lane, double speedValueBetweenZeroAndOne);
	
	double calculateVehicleSpacing(double linkLength, double numberOfVehiclesOnLink, double overallStorageCapacity);

//	double calculateDistanceOnVectorFromFromNode( double length, double spacing,
//			 double lastDistanceFromFromNode, double now, double freespeedTraveltime, double travelTime);
	
	Integer guessLane(QVehicle veh, int numberOfLanes);

	double calcSpeedValueBetweenZeroAndOne(QVehicle veh, double inverseSimulatedFlowCapacity, double now, double freespeed);

	/**
	 * @param length
	 * @param spacing
	 * @param lastDistanceFromFromNode
	 * @param now
	 * @param freespeedTraveltime
	 * @param remainingTravelTime
	 * @return
	 */
	double calculateDistanceOnVectorFromFromNode2(double length, double spacing, double lastDistanceFromFromNode, double now,
			double freespeedTraveltime, double remainingTravelTime);

}
