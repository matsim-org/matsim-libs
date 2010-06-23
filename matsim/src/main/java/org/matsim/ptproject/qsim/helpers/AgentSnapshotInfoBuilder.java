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
package org.matsim.ptproject.qsim.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.qsim.TransitQLaneFeature;
import org.matsim.ptproject.qsim.interfaces.QVehicle;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.PositionInfo;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo.AgentState;

/**
 * A builder for AgentSnapshotInfo objects that can be used by links with queue logic
 * @author dgrether
 */
public class AgentSnapshotInfoBuilder {
	
	private static final Logger log = Logger.getLogger(AgentSnapshotInfoBuilder.class);
	
	private final double storageCapacityFactor; 
	
	private final double cellSize; 
	
	private final String snapshotStyle;


	public AgentSnapshotInfoBuilder(QSimConfigGroup configGroup, double cellSize){
		this.storageCapacityFactor = configGroup.getStorageCapFactor();
		this.cellSize = cellSize;
		snapshotStyle = configGroup.getSnapshotStyle() ;
	}

	public void addVehiclePositions(final Collection<AgentSnapshotInfo> positions, double now, Link link,
			Collection<QVehicle> buffer, Collection<QVehicle> vehQueue, double inverseSimulatedFlowCapacity, 
			double storageCapacity, int bufferStorageCapacity, double linkLength, TransitQLaneFeature transitQueueLaneFeature){
		
		this.addVehiclePositions(positions, now, link, buffer, vehQueue, inverseSimulatedFlowCapacity, storageCapacity, 
				bufferStorageCapacity, linkLength, 0.0, null, transitQueueLaneFeature);
	}	
	
	public void addVehiclePositions(final Collection<AgentSnapshotInfo> positions, double now, Link link,
			Collection<QVehicle> buffer, Collection<QVehicle> vehQueue, double inverseSimulatedFlowCapacity, 
			double storageCapacity, int bufferStorageCapacity, double linkLength, double offset, Integer laneNumber, TransitQLaneFeature transitQueueLaneFeature){
			
		if ("queue".equalsIgnoreCase(this.snapshotStyle)){
			this.addVehiclePositionsAsQueue(positions, now, link, buffer, vehQueue, inverseSimulatedFlowCapacity, 
					storageCapacity, bufferStorageCapacity, linkLength, offset, laneNumber, transitQueueLaneFeature);
		}
		else  if ("equiDist".equalsIgnoreCase(this.snapshotStyle)){
			this.addVehiclePositionsEquil(positions, now, buffer, link, vehQueue, inverseSimulatedFlowCapacity, offset, 
					laneNumber, transitQueueLaneFeature);
		}
		else {
			log.warn("The snapshotStyle \"" + this.snapshotStyle + "\" is not supported.");
		}
	}
	
	/**
	 * Calculates the positions of all vehicles on this link according to the queue-logic: Vehicles are placed on the link
	 * according to the ratio between the free-travel time and the time the vehicles are already on the link. If they could have
	 * left the link already (based on the time), the vehicles start to build a traffic-jam (queue) at the end of the link.
	 *
	 * @param positions
	 *            A collection where the calculated positions can be stored.
	 * @param transitQueueLaneFeature 
	 * @param link2 
	 */
	public void addVehiclePositionsAsQueue(final Collection<AgentSnapshotInfo> positions, double now, 
			Link link, Collection<QVehicle> buffer, Collection<QVehicle> vehQueue, double inverseSimulatedFlowCapacity, 
			double storageCapacity, int bufferStorageCapacity, double linkLength, double offset, Integer laneNumber, TransitQLaneFeature transitQueueLaneFeature) {

		double currentQueueEnd = linkLength; // queue end initialized at end of link
		float vehSpacingAsQueueCache = (float) calculateQueueVehicleSpacing(linkLength, storageCapacity, bufferStorageCapacity);
		// treat vehicles from buffer:
		currentQueueEnd = positionVehiclesFromBufferAsQueue(positions, now, currentQueueEnd, link, vehSpacingAsQueueCache, 
				buffer, inverseSimulatedFlowCapacity, offset, laneNumber, transitQueueLaneFeature);

		// treat other driving vehicles:
		positionOtherDrivingVehiclesAsQueue(positions, now, currentQueueEnd, link, vehSpacingAsQueueCache,
				vehQueue, inverseSimulatedFlowCapacity, offset, laneNumber, transitQueueLaneFeature, linkLength);

		// yyyy waiting list, transit stops, persons at activity, etc. all do not depend on "queue" vs "equil"
		// and should thus not be treated in this method. kai, apr'10
	}
	
	private double calculateQueueVehicleSpacing(double linkLength, double storageCapacity, double bufferStorageCapacity) {
		double vehLen = Math.min( // the length of a vehicle in visualization
				linkLength / (storageCapacity + bufferStorageCapacity), // all vehicles must have place on the link
				this.cellSize / this.storageCapacityFactor); // a vehicle should not be larger than it's actual size. yyyy why is that an issue? kai, apr'10
		return vehLen;
	}
	
	/**
	 * place other driving cars according the following rule: - calculate the time how long the vehicle is on the link already -
	 * calculate the position where the vehicle should be if it could drive with freespeed - if the position is already within
	 * the congestion queue, add it to the queue with slow speed - if the position is not within the queue, just place the car
	 * with free speed at that place
	 * @param inverseSimulatedFlowCapacity 
	 * @param vehQueue 
	 */
	private void positionOtherDrivingVehiclesAsQueue(final Collection<AgentSnapshotInfo> positions, double now,
			double queueEnd, Link link, double vehSpacing, Collection<QVehicle> vehQueue, double inverseSimulatedFlowCapacity, 
			double offset, Integer laneNumber, TransitQLaneFeature transitQueueLaneFeature, double linkLength)
	{
		double lastDistance = Double.POSITIVE_INFINITY;
		double ttfs = link.getLength() / link.getFreespeed(now);
		for (QVehicle veh : vehQueue) {
			double travelTime = now - veh.getLinkEnterTime();
			double distanceOnLink = (ttfs == 0.0 ? 0.0	: ((travelTime / ttfs) * linkLength));
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
			double speed = (now > cmp) ? 0.0 : link.getFreespeed(now);
			int lane;
			if (laneNumber == null){
				lane  = calculateLane(veh, NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link));
			}
			else {
				lane = laneNumber;
			}
			Collection<PersonAgent> peopleInVehicle = getPeopleInVehicle(veh, transitQueueLaneFeature);
			this.createAndAddSnapshotInfoForPeopleInMovingVehicle(positions, peopleInVehicle, distanceOnLink, link, lane, speed, offset);
			lastDistance = distanceOnLink;
		}
	}
	
	private int calculateLane(QVehicle veh, int numberOfLanes){
		int tmpLane;
		try {
			tmpLane = Integer.parseInt(veh.getId().toString()) ;
		} catch ( NumberFormatException ee ) {
			tmpLane = veh.getId().hashCode() ;
		}
		int lane = 1 + (tmpLane % numberOfLanes);
		return lane;
	}
	
	/**
	 *  put all cars in the buffer one after the other
	 * @param buffer 
	 */
	private double positionVehiclesFromBufferAsQueue(final Collection<AgentSnapshotInfo> positions, double now,
			double queueEnd, Link link, double vehSpacing, Collection<QVehicle> buffer, double inverseSimulatedFlowCapacity, 
			double offset, Integer laneNumber, TransitQLaneFeature transitQueueLaneFeature)
	{
		for (QVehicle veh : buffer) {
			int lane;
			if (laneNumber == null){
				lane = calculateLane(veh, NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link));
			}
			else {
				lane = laneNumber;
			}
			int cmp = (int) (veh.getEarliestLinkExitTime() + inverseSimulatedFlowCapacity + 2.0);
			double speed = (now > cmp) ? 0.0 : link.getFreespeed();
			Collection<PersonAgent> peopleInVehicle = getPeopleInVehicle(veh, transitQueueLaneFeature);
			createAndAddSnapshotInfoForPeopleInMovingVehicle(positions, peopleInVehicle, queueEnd, link, lane, speed, offset);
			queueEnd -= vehSpacing;
		}
		return queueEnd;
	}

	
	/**
	 * Calculates the positions of all vehicles on this link so that there is always the same distance between following cars. A
	 * single vehicle will be placed at the middle (0.5) of the link, two cars will be placed at positions 0.25 and 0.75, three
	 * cars at positions 0.16, 0.50, 0.83, and so on.
	 *
	 * @param positions
	 *            A collection where the calculated positions can be stored.
	 * @param time 
	 * @param buffer 
	 * @param link 
	 * @param vehQueue 
	 * @param inverseSimulatedFlowCapacity 
	 * @param transitQueueLaneFeature 
	 */
	protected void addVehiclePositionsEquil(final Collection<AgentSnapshotInfo> positions, double time, 
			Collection<QVehicle> buffer, Link link, Collection<QVehicle> vehQueue, double inverseSimulatedFlowCapacity, 
			double offset, Integer laneNumber, TransitQLaneFeature transitQueueLaneFeature) {
		int cnt = buffer.size() + vehQueue.size();
		if (cnt > 0) {
			double spacing = link.getLength() / cnt;
			double distFromFromNode = link.getLength() - spacing / 2.0;
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
				double speed = (time > cmp ? 0.0 : freespeed);
				Collection<PersonAgent> peopleInVehicle = getPeopleInVehicle(veh, transitQueueLaneFeature);
				createAndAddSnapshotInfoForPeopleInMovingVehicle(positions, peopleInVehicle, distFromFromNode, link, lane, speed, offset);
				distFromFromNode -= spacing;
			}

			// the cars in the drivingQueue
			for (QVehicle veh : vehQueue) {
				int lane = calculateLane(veh, NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link));
				int cmp = (int) (veh.getEarliestLinkExitTime() + inverseSimulatedFlowCapacity + 2.0);
				double speed = (time > cmp ? 0.0 : freespeed);
				Collection<PersonAgent> peopleInVehicle = getPeopleInVehicle(veh, null);
				createAndAddSnapshotInfoForPeopleInMovingVehicle(positions, peopleInVehicle, distFromFromNode, link, lane, speed, offset);
				distFromFromNode -= spacing;
			}
		}
	}
	
	private void createAndAddSnapshotInfoForPeopleInMovingVehicle(Collection<AgentSnapshotInfo> positions,
			Collection<PersonAgent> peopleInVehicle, double distanceOnLink, Link link, int lane, double speed, double offset)
	{
		distanceOnLink += offset;
		int cnt = 0 ;
		for (PersonAgent passenger : peopleInVehicle) {
			AgentSnapshotInfo passengerPosition = new PositionInfo(passenger.getPerson().getId(), link, distanceOnLink, lane, cnt );
			passengerPosition.setColorValueBetweenZeroAndOne(speed);
			if (passenger.getPerson().getId().toString().startsWith("pt")) {
				passengerPosition.setAgentState(AgentState.TRANSIT_DRIVER);
			} else if (cnt==0) {
				passengerPosition.setAgentState(AgentState.PERSON_DRIVING_CAR);
			} else {
				passengerPosition.setAgentState(AgentState.PERSON_OTHER_MODE); // in 2010, probably a passenger
			}
			positions.add(passengerPosition);
			cnt++ ;
		}
	}
	

	public int positionAgentsInActivities(final Collection<AgentSnapshotInfo> positions, Link link,
			Collection<PersonAgent> agentsInActivities,  int cnt2) {
		int c = cnt2;
		for (PersonAgent pa : agentsInActivities) {
			PositionInfo agInfo = new PositionInfo( pa.getPerson().getId(), link, c) ;
			agInfo.setAgentState( AgentState.PERSON_AT_ACTIVITY ) ;
			positions.add(agInfo) ;
			c++ ;
		}
		return c;
	}

	
	/**
	 * Put the vehicles from the waiting list in positions. Their actual position doesn't matter, PositionInfo provides a
	 * constructor for handling this situation.
	 * @param waitingList
	 * @param transitQueueLaneFeature
	 */
	public void positionVehiclesFromWaitingList(final Collection<AgentSnapshotInfo> positions,
			final Link link, int cnt2, final Queue<QVehicle> waitingList, TransitQLaneFeature transitQueueLaneFeature) {
		for (QVehicle veh : waitingList) {
			Collection<PersonAgent> peopleInVehicle = getPeopleInVehicle(veh, transitQueueLaneFeature);
			boolean first = true;
			for (PersonAgent passenger : peopleInVehicle) {
				AgentSnapshotInfo passengerPosition = new PositionInfo(passenger.getPerson().getId(), link,
						cnt2); // for the time being, same position as facilities
				if (passenger.getPerson().getId().toString().startsWith("pt")) {
					passengerPosition.setAgentState(AgentState.TRANSIT_DRIVER);
				}
				else if (first) {
					passengerPosition.setAgentState(AgentState.PERSON_DRIVING_CAR);
				}
				else {
					passengerPosition.setAgentState(AgentState.PERSON_OTHER_MODE);
				}
				positions.add(passengerPosition);
				first = false;
			}
		}
	}

	/**
	 * Returns all the people sitting in this vehicle.
	 * 
	 * @param vehicle
	 * @param transitQueueLaneFeature 
	 * @return All the people in this vehicle. If there is more than one, the first entry is the driver.
	 */
	private Collection<PersonAgent> getPeopleInVehicle(QVehicle vehicle, TransitQLaneFeature transitQueueLaneFeature) {
		Collection<PersonAgent> passengers = null;
		if (transitQueueLaneFeature != null) {
			passengers = transitQueueLaneFeature
			.getPassengers(vehicle); // yy seems to me that "getPassengers" is a vehicle feature???
		}
		if (passengers == null || passengers.isEmpty()) {
			return Collections.singletonList((PersonAgent) vehicle.getDriver());
		}
		else {
			ArrayList<PersonAgent> people = new ArrayList<PersonAgent>();
			people.add(vehicle.getDriver());
			people.addAll(passengers);
			return people;
		}
	}

}
