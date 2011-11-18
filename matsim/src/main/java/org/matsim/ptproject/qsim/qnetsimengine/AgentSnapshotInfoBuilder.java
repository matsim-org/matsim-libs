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
package org.matsim.ptproject.qsim.qnetsimengine;

import java.util.Collection;
import java.util.Queue;

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

	void positionVehiclesFromTransitStop(Collection<AgentSnapshotInfo> positions, Link link,
			Queue<QVehicle> transitVehicleStopQueue, int cnt2);

	void positionVehiclesFromWaitingList(Collection<AgentSnapshotInfo> positions, Link link,
			int cnt2, Queue<QVehicle> waitingList);

	int positionAgentsInActivities(Collection<AgentSnapshotInfo> positions, Link link,
			Collection<MobsimAgent> values, int cnt2);

	/**
 	 *  Adds AgentSnapshotInfo instances to the Collection given as parameter for a queue-logic object located on a specific link. A queue-logic object
	 * can be a QueueLinkImpl or a QueueLane instance or something else.
	 * @param positions The Collection in that the AgentSnapshotInfo instances are inserted.
	 * @param linkLength linkLength the length of the queue-based object
	 * @param offset The distance between the from node of the link to the beginning of the queue-logic object
	 * @param laneNumber computed by builder if null
	 */
	void addVehiclePositions(VisLane visLane, final Collection<AgentSnapshotInfo> positions, Collection<QVehicle> buffer,
			Collection<QVehicle> vehQueue, Collection<QItem> holes, double linkLength,
			double offset, Integer laneNumber);	
}
