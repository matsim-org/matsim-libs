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
package org.matsim.ptproject.qsim.qnetsimengine;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;


/**
 * @author dgrether
 *
 */
public class QueueAgentSnapshotInfoBuilder extends AbstractAgentSnapshotInfoBuilder {

	public QueueAgentSnapshotInfoBuilder(Scenario scenario) {
		super(scenario);
	}

	/**
	 * Calculates the positions of all vehicles on this link according to the queue-logic: Vehicles are placed on the link
	 * according to the ratio between the free-travel time and the time the vehicles are already on the link. If they could have
	 * left the link already (based on the time), the vehicles start to build a traffic-jam (queue) at the end of the link.
	 */
	@Override
	public void addVehiclePositions(VisLane visLane, Collection<AgentSnapshotInfo> positions,
			Collection<QVehicle> buffer, Collection<QVehicle> vehQueue, Collection<QItem> holes,
			double linkLength, double offset, Integer laneNumber) {

		double storageCapacity = visLane.getStorageCapacity() ;
		double bufferStorageCapacity = visLane.getBufferStorage() ;

		double currentQueueEnd = linkLength; // queue end initialized at end of link
		float vehSpacingAsQueueCache = (float) calculateVehicleSpacingAsQueue(linkLength, storageCapacity, bufferStorageCapacity);
		// treat vehicles from buffer:
		currentQueueEnd = positionVehiclesFromBufferAsQueue(positions, currentQueueEnd, vehSpacingAsQueueCache, buffer, offset,
				laneNumber, visLane);

		// treat other driving vehicles:
		positionOtherDrivingVehiclesAsQueue(positions, currentQueueEnd, vehSpacingAsQueueCache, vehQueue, offset,
				laneNumber, linkLength, visLane);

		// yyyy waiting list, transit stops, persons at activity, etc. all do not depend on "queue" vs "equil"
		// and should thus not be treated in this method. kai, apr'10
	}

	
	protected double calculateVehicleSpacingAsQueue(double linkLength, double storageCapacity, double bufferStorageCapacity) {
		double vehLen = Math.min( // the length of a vehicle in visualization
				linkLength / (storageCapacity + bufferStorageCapacity), // all vehicles must have place on the link
				this.cellSize / this.storageCapacityFactor); // a vehicle should not be larger than it's actual size. yyyy why is that an issue? kai, apr'10
		return vehLen;
	}

	/**
	 *  put all cars in the buffer one after the other
	 * @param buffer
	 * @param qBufferItem TODO
	 */
	protected double positionVehiclesFromBufferAsQueue(final Collection<AgentSnapshotInfo> positions, double queueEnd,
			double vehSpacing, Collection<QVehicle> buffer, double offset, Integer laneNumber, 
			VisLane qBufferItem)
	{
		double now = qBufferItem.getQLink().network.simEngine.getMobsim().getSimTimer().getTimeOfDay() ;
		Link  link = qBufferItem.getQLink().getLink() ;
		double inverseSimulatedFlowCapacity = qBufferItem.getInverseSimulatedFlowCapacity() ;

		for (QVehicle veh : buffer) {
			int lane;
			if (laneNumber == null){
				lane = calculateLane(veh, NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link));
			}
			else {
				lane = laneNumber;
			}
			int cmp = (int) (veh.getEarliestLinkExitTime() + inverseSimulatedFlowCapacity + 2.0);
			double speedValueBetweenZeroAndOne = (now > cmp) ? 0.0 : 1.0 ;
			List<MobsimAgent> peopleInVehicle = getPeopleInVehicle(veh);
			createAndAddSnapshotInfoForPeopleInMovingVehicle(positions, peopleInVehicle, queueEnd, link, lane, speedValueBetweenZeroAndOne, offset);
			queueEnd -= vehSpacing;
		}
		return queueEnd;
	}

	/**
	 * place other driving cars according the following rule: - calculate the time how long the vehicle is on the link already -
	 * calculate the position where the vehicle should be if it could drive with freespeed - if the position is already within
	 * the congestion queue, add it to the queue with slow speed - if the position is not within the queue, just place the car
	 * with free speed at that place
	 */
	private void positionOtherDrivingVehiclesAsQueue(final Collection<AgentSnapshotInfo> positions, double queueEnd,
			double vehSpacing, Collection<QVehicle> vehQueue, double offset, Integer laneNumber,
			double length, VisLane qBufferItem)
	{
		double now = qBufferItem.getQLink().network.simEngine.getMobsim().getSimTimer().getTimeOfDay() ;
		Link  link = qBufferItem.getQLink().getLink() ;
		double inverseSimulatedFlowCapacity = qBufferItem.getInverseSimulatedFlowCapacity() ;

		double lastDistance = Double.POSITIVE_INFINITY;
		double freeSpeedTravelTime = link.getLength() / link.getFreespeed(now);
		for (QVehicle veh : vehQueue) {
			double travelTime = now - veh.getLinkEnterTime();
			double distanceOnLink = (freeSpeedTravelTime == 0.0 ? 0.0	: ((travelTime / freeSpeedTravelTime) * length));
			if (distanceOnLink > queueEnd) { // vehicle is already in queue
				distanceOnLink = queueEnd;
				queueEnd -= vehSpacing;
			}
			if (distanceOnLink >= lastDistance) {
				/*
				 * we have a queue, so it should not be possible that one vehicles overtakes another. additionally, if two
				 * vehicles entered at the same time, they would be drawn on top of each other. we don't allow this, so in this
				 * case we put one after the other. Theoretically, this could lead to vehicles placed at negative distance when
				 * a lot of vehicles all enter at the same time on an empty link. not sure what to do about this yet... just
				 * setting them to 0 currently.
				 */
				distanceOnLink = lastDistance - vehSpacing;
				if (distanceOnLink < 0)
					distanceOnLink = 0.0;
			}
			int cmp = (int) (veh.getEarliestLinkExitTime() + inverseSimulatedFlowCapacity + 2.0);
			double speedValueBetweenZeroAndOne = (now > cmp) ? 0.0 : 1.0 ;
			int lane;
			if (laneNumber == null){
				lane  = calculateLane(veh, NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link));
			}
			else {
				lane = laneNumber;
			}
			List<MobsimAgent> peopleInVehicle = getPeopleInVehicle(veh);
			this.createAndAddSnapshotInfoForPeopleInMovingVehicle(positions, peopleInVehicle, distanceOnLink, link, lane, speedValueBetweenZeroAndOne, offset);
			lastDistance = distanceOnLink;
		}
	}

	
}
