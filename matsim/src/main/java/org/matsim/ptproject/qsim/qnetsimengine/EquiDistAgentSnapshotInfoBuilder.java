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
package org.matsim.ptproject.qsim.qnetsimengine;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;

/**
 * A builder for AgentSnapshotInfo objects that can be used by links with queue logic
 * @author dgrether
 */
final class EquiDistAgentSnapshotInfoBuilder extends AbstractAgentSnapshotInfoBuilder implements AgentSnapshotInfoBuilder {

	private static final Logger log = Logger.getLogger(EquiDistAgentSnapshotInfoBuilder.class);

	EquiDistAgentSnapshotInfoBuilder( Scenario sc ){
		super(sc);
	}

	/**
	 * Adds AgentSnapshotInfo instances to the Collection given as parameter for a queue-logic object located on a specific link. A queue-logic object
	 * can be a QueueLinkImpl or a QueueLane instance or something else.
	 * @param visLane TODO
	 * @param positions The Collection in that the AgentSnapshotInfo instances are inserted.
	 * @param buffer
	 * @param vehQueue
	 * @param linkLength the length of the queue-based object
	 * @param offset The distance between the from node of the link to the beginning of the queue-logic object
	 * @param laneNumber
	 */
	public void addVehiclePositions(VisLane visLane, final Collection<AgentSnapshotInfo> positions, Collection<QVehicle> buffer,
			Collection<QVehicle> vehQueue, Collection<QItem> holes, double linkLength,
			double offset, Integer laneNumber){
		this.addVehiclePositionsEquil(positions, buffer, vehQueue, offset, laneNumber, linkLength, visLane);
		
	}
	/**
	 * Calculates the positions of all vehicles on this link so that there is always the same distance between following cars. A
	 * single vehicle will be placed at the middle (0.5) of the link, two cars will be placed at positions 0.25 and 0.75, three
	 * cars at positions 0.16, 0.50, 0.83, and so on.
	 *
	 * @param positions
	 *            A collection where the calculated positions can be stored.
	 * @param buffer
	 * @param vehQueue
	 * @param transitQueueLaneFeature
	 * @param linkLength
	 * @param qBufferItem TODO
	 */
	protected void addVehiclePositionsEquil(final Collection<AgentSnapshotInfo> positions, Collection<QVehicle> buffer,
			Collection<QVehicle> vehQueue, double offset, Integer laneNumber, 
			double linkLength, VisLane qBufferItem)
	{
		double now = qBufferItem.getQLink().getMobsim().getSimTimer().getTimeOfDay() ;
		Link  link = qBufferItem.getQLink().getLink() ;
		double inverseSimulatedFlowCapacity = qBufferItem.getInverseSimulatedFlowCapacity() ;

		int cnt = buffer.size() + vehQueue.size();
		if (cnt > 0) {
			double spacing = linkLength / cnt;
			double distFromFromNode = linkLength - (spacing / 2.0);
			double freespeed = link.getFreespeed();

			// the cars in the buffer
			for (QVehicle veh : buffer) {
				int lane;
				if (laneNumber == null){
					lane = calculateLane(veh, NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link));
				}
				else {
					lane = laneNumber;
				}
				int cmp = (int) (veh.getEarliestLinkExitTime() + inverseSimulatedFlowCapacity + 2.0);
				double speed = (now > cmp ? 0.0 : freespeed);
				List<MobsimAgent> peopleInVehicle = getPeopleInVehicle(veh);
				createAndAddSnapshotInfoForPeopleInMovingVehicle(positions, peopleInVehicle, distFromFromNode, link, lane, speed, offset);
				distFromFromNode -= spacing;
			}

			// the cars in the drivingQueue
			for (QVehicle veh : vehQueue) {
				int lane = calculateLane(veh, NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link));
				int cmp = (int) (veh.getEarliestLinkExitTime() + inverseSimulatedFlowCapacity + 2.0);
				double speedValueBetweenZeroAndOne = (now > cmp ? 0.0 : 1.0);
				List<MobsimAgent> peopleInVehicle = getPeopleInVehicle(veh);
				createAndAddSnapshotInfoForPeopleInMovingVehicle(positions, peopleInVehicle, distFromFromNode, link, lane, speedValueBetweenZeroAndOne, offset);
				distFromFromNode -= spacing;
			}
		}
	}








}
