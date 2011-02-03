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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.qsim.TransitQLaneFeature;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo.AgentState;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfoFactory;

/**
 * A builder for AgentSnapshotInfo objects that can be used by links with queue logic
 * @author dgrether
 */
 final class AgentSnapshotInfoBuilder {

	private static final Logger log = Logger.getLogger(AgentSnapshotInfoBuilder.class);

	private final double storageCapacityFactor;

	private final double cellSize;

	private final String snapshotStyle;


	 AgentSnapshotInfoBuilder( Scenario sc ){
		this.storageCapacityFactor = sc.getConfig().getQSimConfigGroup().getStorageCapFactor();
		this.cellSize = ((NetworkImpl) sc.getNetwork()).getEffectiveCellSize() ;
		this.snapshotStyle = sc.getConfig().getQSimConfigGroup().getSnapshotStyle() ;
		
		double effLaneWidth = sc.getNetwork().getEffectiveLaneWidth() ;
		if ( Double.isNaN( effLaneWidth ) ) {
			AgentSnapshotInfoFactory.setLaneWidth( 3.75 ) ; // yyyyyy magic number
		} else {
			AgentSnapshotInfoFactory.setLaneWidth( effLaneWidth );
		}
	}

	/**
	 * See overloaded method for parameter description.
	 * @param visLane TODO
	 */
	 void addVehiclePositions(VisLane visLane, final Collection<AgentSnapshotInfo> positions, Collection<QVehicle> buffer,
			Collection<QVehicle> vehQueue, Collection<QItem> holes, double linkLength,
			TransitQLaneFeature transitQueueLaneFeature){

		this.addVehiclePositions(visLane, positions, buffer, vehQueue, holes, linkLength, 0.0,
				null, transitQueueLaneFeature);
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
	 * @param transitQueueLaneFeature
	 */
	 void addVehiclePositions(VisLane visLane, final Collection<AgentSnapshotInfo> positions, Collection<QVehicle> buffer,
			Collection<QVehicle> vehQueue, Collection<QItem> holes, double linkLength,
			double offset, Integer laneNumber, TransitQLaneFeature transitQueueLaneFeature){

		if ("queue".equalsIgnoreCase(this.snapshotStyle)){
			this.addVehiclePositionsAsQueue(positions, buffer, vehQueue, linkLength, offset, laneNumber,
					transitQueueLaneFeature, visLane);
		}
		else  if ("equiDist".equalsIgnoreCase(this.snapshotStyle)){
			this.addVehiclePositionsEquil(positions, buffer, vehQueue, offset, laneNumber, transitQueueLaneFeature, linkLength,
					visLane);
		}
		else if ("withHolesExperimental".equalsIgnoreCase(this.snapshotStyle)){
			this.addVehiclePositionsWithHoles(positions, buffer, vehQueue, holes, linkLength, offset,
					laneNumber, transitQueueLaneFeature, visLane);
		}
		else {
			log.warn("The snapshotStyle \"" + this.snapshotStyle + "\" is not supported.");
		}
	}

	private void addVehiclePositionsWithHoles(final Collection<AgentSnapshotInfo> positions, Collection<QVehicle> buffer,
			Collection<QVehicle> vehQueue, Collection<QItem> holes, double linkLength, double offset,
			Integer laneNumber, TransitQLaneFeature transitQueueLaneFeature, VisLane visLane)
	{
		double storageCapacity = visLane.getStorageCapacity() ;
		double bufferStorageCapacity = visLane.getBufferStorage() ;

		double currentQueueEnd = linkLength; // queue end initialized at end of link

		// for holes: using the "queue" method, but with different vehSpacing:
		float vehSpacing = (float) calculateVehicleSpacingAsQueue(linkLength, storageCapacity, bufferStorageCapacity);
//		float vehSpacingWithHoles = (float) calculateVehicleSpacingWithHoles(linkLength, storageCapacity, bufferStorageCapacity,
//				congestedDensity);

		// treat vehicles from buffer:
		currentQueueEnd = positionVehiclesFromBufferAsQueue(positions, currentQueueEnd, vehSpacing, buffer, offset,
				laneNumber, transitQueueLaneFeature, visLane);

		// treat other driving vehicles:
		positionOtherDrivingVehiclesWithHoles(positions, currentQueueEnd, vehSpacing, vehQueue, holes,
				offset, laneNumber, transitQueueLaneFeature, visLane);

	}

	/*package*/ static class TupleDoubleComparator implements Comparator<Tuple<Double, QItem>>, Serializable {

		private static final long serialVersionUID = 1L;

		@Override
		public int compare(final Tuple<Double, QItem> o1, final Tuple<Double, QItem> o2) {
			int ret = - o1.getFirst().compareTo(o2.getFirst()); // the minus should, in theory, sort by decreasing "Double"
			if (ret == 0) {
				ret = o2.getSecond().toString().compareTo(o1.getSecond().toString());
			}
			return ret;
		}
	}

	private void positionOtherDrivingVehiclesWithHoles(final Collection<AgentSnapshotInfo> positions, double queueEnd,
			double vehSpacing, Collection<QVehicle> vehQueue, Collection<QItem> holes, double offset, Integer laneNumber,
			TransitQLaneFeature transitQueueLaneFeature, VisLane visLane)
	{
		if ( visLane instanceof QLane ) {
			throw new RuntimeException("holes visualization is not implemented for lanes since I don't understand which "
					+ "quantities refer to the link and which to the lane.  kai, nov'10" ) ;
		}

		double now = visLane.getQLink().getMobsim().getSimTimer().getTimeOfDay() ;
		Link  link = visLane.getQLink().getLink() ;

		Queue<Tuple<Double, QItem>> qItemList = new PriorityQueue<Tuple<Double, QItem>>(30, new TupleDoubleComparator() );
		for ( QVehicle veh : vehQueue ) {
			double distanceOnLink_m = (now - veh.getLinkEnterTime() ) * link.getFreespeed(now) ;
			qItemList.add( new Tuple<Double,QItem>( distanceOnLink_m, veh ) ) ;
		}
		for ( QItem hole : holes ) {
			double distanceOnLink_m = ( hole.getEarliestLinkExitTime() - now ) * 15.*1000./3600. ;
			// holes come from the end of the link; the remaining distance is the same as distanceOnLink_m
			qItemList.add( new Tuple<Double,QItem>( distanceOnLink_m, hole ) ) ;
		}

		for (QVehicle veh : vehQueue) {
			boolean inQueue = false ;
			double distanceOnLink_m = (now - veh.getLinkEnterTime()) * link.getFreespeed(now) ;
			if (distanceOnLink_m > queueEnd) { // vehicle is already in queue
				distanceOnLink_m = queueEnd;
				queueEnd -= vehSpacing;
				inQueue = true ;
			}

			double speedValueBetweenZeroAndOne = 1. ;
			if ( inQueue ) {
				speedValueBetweenZeroAndOne = 0. ; // yy could be something more realistic than 0.  kai, nov'10
			}

			int lane ;
			if (laneNumber == null){
				lane  = calculateLane(veh, NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link));
			} else {
				lane = laneNumber ;
			}

			List<PersonAgent> peopleInVehicle = getPeopleInVehicle(veh, transitQueueLaneFeature);

			this.createAndAddSnapshotInfoForPeopleInMovingVehicle(positions, peopleInVehicle, distanceOnLink_m, link, lane,
					speedValueBetweenZeroAndOne, offset);
		}
		throw new RuntimeException("this is not (yet) finished; aborting ...") ;

	}
//	private double calculateVehicleSpacingWithHoles(double linkLength, double storageCapacity, double bufferStorageCapacity,
//			double congestedDensity_veh_m ) {
//		// yyyyyy abusing congDens as nHolesMax!
//		return 1.*(linkLength / (storageCapacity + bufferStorageCapacity - congestedDensity_veh_m )) ;
//	}

	/**
	 * Calculates the positions of all vehicles on this link according to the queue-logic: Vehicles are placed on the link
	 * according to the ratio between the free-travel time and the time the vehicles are already on the link. If they could have
	 * left the link already (based on the time), the vehicles start to build a traffic-jam (queue) at the end of the link.
	 * @param visLane TODO
	 */
	private void addVehiclePositionsAsQueue(final Collection<AgentSnapshotInfo> positions, Collection<QVehicle> buffer,
			Collection<QVehicle> vehQueue, double linkLength, double offset, Integer laneNumber,
			TransitQLaneFeature transitQueueLaneFeature, VisLane visLane)
	{
		double storageCapacity = visLane.getStorageCapacity() ;
		double bufferStorageCapacity = visLane.getBufferStorage() ;

		double currentQueueEnd = linkLength; // queue end initialized at end of link
		float vehSpacingAsQueueCache = (float) calculateVehicleSpacingAsQueue(linkLength, storageCapacity, bufferStorageCapacity);
		// treat vehicles from buffer:
		currentQueueEnd = positionVehiclesFromBufferAsQueue(positions, currentQueueEnd, vehSpacingAsQueueCache, buffer, offset,
				laneNumber, transitQueueLaneFeature, visLane);

		// treat other driving vehicles:
		positionOtherDrivingVehiclesAsQueue(positions, currentQueueEnd, vehSpacingAsQueueCache, vehQueue, offset,
				laneNumber, transitQueueLaneFeature, linkLength, visLane);

		// yyyy waiting list, transit stops, persons at activity, etc. all do not depend on "queue" vs "equil"
		// and should thus not be treated in this method. kai, apr'10
	}

	private double calculateVehicleSpacingAsQueue(double linkLength, double storageCapacity, double bufferStorageCapacity) {
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
	 */
	private void positionOtherDrivingVehiclesAsQueue(final Collection<AgentSnapshotInfo> positions, double queueEnd,
			double vehSpacing, Collection<QVehicle> vehQueue, double offset, Integer laneNumber, TransitQLaneFeature transitQueueLaneFeature,
			double linkLength, VisLane qBufferItem)
	{
		double now = qBufferItem.getQLink().getMobsim().getSimTimer().getTimeOfDay() ;
		Link  link = qBufferItem.getQLink().getLink() ;
		double inverseSimulatedFlowCapacity = qBufferItem.getInverseSimulatedFlowCapacity() ;

		double lastDistance = Double.POSITIVE_INFINITY;
		double freeSpeedTravelTime = link.getLength() / link.getFreespeed(now);
		for (QVehicle veh : vehQueue) {
			boolean inQueue = false ;
			double travelTime = now - veh.getLinkEnterTime();
			double distanceOnLink = (freeSpeedTravelTime == 0.0 ? 0.0	: ((travelTime / freeSpeedTravelTime) * linkLength));
			if (distanceOnLink > queueEnd) { // vehicle is already in queue
				distanceOnLink = queueEnd;
				queueEnd -= vehSpacing;
				inQueue = true ;
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
			if ("withHolesExperimental".equalsIgnoreCase(this.snapshotStyle)){
				if ( inQueue ) {
					speedValueBetweenZeroAndOne = 0. ;
					// yy could be something more realistic than 0.  kai, nov'10
				} else {
					speedValueBetweenZeroAndOne = 1. ;
				}
			}
			int lane;
			if (laneNumber == null){
				lane  = calculateLane(veh, NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link));
			}
			else {
				lane = laneNumber;
			}
			List<PersonAgent> peopleInVehicle = getPeopleInVehicle(veh, transitQueueLaneFeature);
			this.createAndAddSnapshotInfoForPeopleInMovingVehicle(positions, peopleInVehicle, distanceOnLink, link, lane, speedValueBetweenZeroAndOne, offset);
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
	 * @param qBufferItem TODO
	 */
	private double positionVehiclesFromBufferAsQueue(final Collection<AgentSnapshotInfo> positions, double queueEnd,
			double vehSpacing, Collection<QVehicle> buffer, double offset, Integer laneNumber, TransitQLaneFeature transitQueueLaneFeature,
			VisLane qBufferItem)
	{
		double now = qBufferItem.getQLink().getMobsim().getSimTimer().getTimeOfDay() ;
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
			List<PersonAgent> peopleInVehicle = getPeopleInVehicle(veh, transitQueueLaneFeature);
			createAndAddSnapshotInfoForPeopleInMovingVehicle(positions, peopleInVehicle, queueEnd, link, lane, speedValueBetweenZeroAndOne, offset);
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
	 * @param buffer
	 * @param vehQueue
	 * @param transitQueueLaneFeature
	 * @param linkLength
	 * @param qBufferItem TODO
	 */
	protected void addVehiclePositionsEquil(final Collection<AgentSnapshotInfo> positions, Collection<QVehicle> buffer,
			Collection<QVehicle> vehQueue, double offset, Integer laneNumber, TransitQLaneFeature transitQueueLaneFeature,
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
				List<PersonAgent> peopleInVehicle = getPeopleInVehicle(veh, transitQueueLaneFeature);
				createAndAddSnapshotInfoForPeopleInMovingVehicle(positions, peopleInVehicle, distFromFromNode, link, lane, speed, offset);
				distFromFromNode -= spacing;
			}

			// the cars in the drivingQueue
			for (QVehicle veh : vehQueue) {
				int lane = calculateLane(veh, NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link));
				int cmp = (int) (veh.getEarliestLinkExitTime() + inverseSimulatedFlowCapacity + 2.0);
				double speedValueBetweenZeroAndOne = (now > cmp ? 0.0 : 1.0);
				List<PersonAgent> peopleInVehicle = getPeopleInVehicle(veh, null);
				createAndAddSnapshotInfoForPeopleInMovingVehicle(positions, peopleInVehicle, distFromFromNode, link, lane, speedValueBetweenZeroAndOne, offset);
				distFromFromNode -= spacing;
			}
		}
	}

	private void createAndAddSnapshotInfoForPeopleInMovingVehicle(Collection<AgentSnapshotInfo> positions,
			List<PersonAgent> peopleInVehicle, double distanceOnLink, Link link, int lane, double speedValueBetweenZeroAndOne, double offset)
	{
		distanceOnLink += offset;
		int cnt = peopleInVehicle.size() - 1 ;
//		for (PersonAgent passenger : peopleInVehicle) {
		for ( ListIterator<PersonAgent> it = peopleInVehicle.listIterator( peopleInVehicle.size() ) ; it.hasPrevious(); ) {
			// this now runs backwards so that the BVG vehicle type color is on top.  kai, sep'10
			PersonAgent passenger = it.previous();
			AgentSnapshotInfo passengerPosition = AgentSnapshotInfoFactory.staticCreateAgentSnapshotInfo(passenger.getPerson().getId(), link, distanceOnLink, lane, cnt);
			passengerPosition.setColorValueBetweenZeroAndOne(speedValueBetweenZeroAndOne);
//			double tmp = ( Double.valueOf( passenger.getPerson().getId().toString() ) % 100 ) / 100. ;
//			passengerPosition.setColorValueBetweenZeroAndOne(tmp);
			if (passenger.getPerson().getId().toString().startsWith("pt")) {
				passengerPosition.setAgentState(AgentState.TRANSIT_DRIVER);
			} else if (cnt==0) {
				passengerPosition.setAgentState(AgentState.PERSON_DRIVING_CAR);
			} else {
				passengerPosition.setAgentState(AgentState.PERSON_OTHER_MODE); // in 2010, probably a passenger
			}
			positions.add(passengerPosition);
			cnt-- ;
		}
	}


	 int positionAgentsInActivities(final Collection<AgentSnapshotInfo> positions, Link link,
			Collection<PlanAgent> agentsInActivities,  int cnt2) {
		int c = cnt2;
		for (PlanAgent pa : agentsInActivities) {
			AgentSnapshotInfo agInfo = AgentSnapshotInfoFactory.staticCreateAgentSnapshotInfo(pa.getId(), link, c) ;
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
	 void positionVehiclesFromWaitingList(final Collection<AgentSnapshotInfo> positions,
			final Link link, int cnt2, final Queue<QVehicle> waitingList, TransitQLaneFeature transitQueueLaneFeature) {
		for (QVehicle veh : waitingList) {
			Collection<PersonAgent> peopleInVehicle = getPeopleInVehicle(veh, transitQueueLaneFeature);
			boolean first = true;
			for (PersonAgent passenger : peopleInVehicle) {
				AgentSnapshotInfo passengerPosition = AgentSnapshotInfoFactory.staticCreateAgentSnapshotInfo(passenger.getPerson().getId(), link, cnt2); // for the time being, same position as facilities
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
	private List<PersonAgent> getPeopleInVehicle(QVehicle vehicle, TransitQLaneFeature transitQueueLaneFeature) {
		List<PersonAgent> passengers = null;
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
