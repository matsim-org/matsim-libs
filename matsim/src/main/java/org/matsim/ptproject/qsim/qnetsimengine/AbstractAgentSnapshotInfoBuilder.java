/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractAgentSnapshotInfoBuilder
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.pt.qsim.PassengerAgent;
import org.matsim.pt.qsim.TransitDriverAgent;
import org.matsim.pt.qsim.TransitVehicle;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo.AgentState;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;


/**
 * @author dgrether
 *
 */
abstract class AbstractAgentSnapshotInfoBuilder implements AgentSnapshotInfoBuilder{

	private static final Logger log = Logger.getLogger(AbstractAgentSnapshotInfoBuilder.class);

	protected final double storageCapacityFactor;

	protected final double cellSize;

	private final AgentSnapshotInfoFactory snapshotInfoFactory;
	
	AbstractAgentSnapshotInfoBuilder( Scenario sc, final AgentSnapshotInfoFactory agentSnapshotInfoFactory ){
		this.storageCapacityFactor = sc.getConfig().getQSimConfigGroup().getStorageCapFactor();
		this.cellSize = ((NetworkImpl) sc.getNetwork()).getEffectiveCellSize() ;
		this.snapshotInfoFactory = agentSnapshotInfoFactory;
		double effLaneWidth = sc.getNetwork().getEffectiveLaneWidth() ;
		if ( Double.isNaN( effLaneWidth ) ) {
			snapshotInfoFactory.setLaneWidth( 3.75 ) ; // yyyyyy magic number
		} else {
			snapshotInfoFactory.setLaneWidth( effLaneWidth );
		}
	}
	
	  
	
	/**
	 * Put the vehicles from the waiting list in positions. Their actual position doesn't matter, PositionInfo provides a
	 * constructor for handling this situation.
	 * @param waitingList
	 * @param transitQueueLaneFeature
	 */
	public void positionVehiclesFromWaitingList(final Collection<AgentSnapshotInfo> positions,
			final Link link, int cnt2, final Queue<QVehicle> waitingList) {
		for (QVehicle veh : waitingList) {
			Collection<MobsimAgent> peopleInVehicle = getPeopleInVehicle(veh);
			boolean first = true;
			for (MobsimAgent passenger : peopleInVehicle) {
				AgentSnapshotInfo passengerPosition = snapshotInfoFactory.createAgentSnapshotInfo(passenger.getId(), link, 0.9*link.getLength(), cnt2); // for the time being, same position as facilities
				if (passenger.getId().toString().startsWith("pt")) {
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

	public int positionAgentsInActivities(final Collection<AgentSnapshotInfo> positions, Link link,
			Collection<MobsimAgent> agentsInActivities,  int cnt2) {
		int c = cnt2;
		for (MobsimAgent pa : agentsInActivities) {
			AgentSnapshotInfo agInfo = snapshotInfoFactory.createAgentSnapshotInfo(pa.getId(), link, 0.9*link.getLength(), c) ;
			agInfo.setAgentState( AgentState.PERSON_AT_ACTIVITY ) ;
			positions.add(agInfo) ;
			c++ ;
		}
		return c;
	}

	/**
	 * Put the transit vehicles from the transit stop list in positions.
	 * @param transitVehicleStopQueue 
	 */
	public void positionVehiclesFromTransitStop(final Collection<AgentSnapshotInfo> positions, Link link, Queue<QVehicle> transitVehicleStopQueue, int cnt2 ) {
		if (transitVehicleStopQueue.size() > 0) {
			for (QVehicle veh : transitVehicleStopQueue) {
				List<MobsimAgent> peopleInVehicle = getPeopleInVehicle(veh);
				boolean last = false ;
				cnt2 += peopleInVehicle.size() ;
				for ( ListIterator<MobsimAgent> it = peopleInVehicle.listIterator( peopleInVehicle.size() ) ; it.hasPrevious(); ) {
					MobsimAgent passenger = it.previous();
					if ( !it.hasPrevious() ) {
						last = true ;
					}
					AgentSnapshotInfo passengerPosition = snapshotInfoFactory.createAgentSnapshotInfo(passenger.getId(), link, 0.9*link.getLength(), cnt2); // for the time being, same position as facilities
					if ( passenger.getId().toString().startsWith("pt")) {
						passengerPosition.setAgentState(AgentState.TRANSIT_DRIVER);
					} else if (last) {
						passengerPosition.setAgentState(AgentState.PERSON_DRIVING_CAR);
					} else {
						passengerPosition.setAgentState(AgentState.PERSON_OTHER_MODE);
					}
					positions.add(passengerPosition);
					cnt2-- ;
				}

			}

		}
	}
	
	public void createAndAddVehiclePosition(final Collection<AgentSnapshotInfo> positions, Link link, QVehicle veh, 
			double distanceFromFromNode,	int lane, double speedValueBetweenZeroAndOne){
		MobsimDriverAgent driverAgent = veh.getDriver();
		AgentSnapshotInfo pos = snapshotInfoFactory.createAgentSnapshotInfo(driverAgent.getId(), link, 
				distanceFromFromNode, lane);
		pos.setColorValueBetweenZeroAndOne(speedValueBetweenZeroAndOne);
		if (driverAgent instanceof TransitDriverAgent){
			pos.setAgentState(AgentState.TRANSIT_DRIVER);
			TransitVehicle transitVehicle = (TransitVehicle) veh; //currently only TransitVehicles are able to have passengers
			this.createAndAddSnapshotInfoForPassengers(positions, transitVehicle.getPassengers(), distanceFromFromNode, link, 
					lane, speedValueBetweenZeroAndOne);
		}
		else {
			pos.setAgentState(AgentState.PERSON_DRIVING_CAR);
		}
		positions.add(pos);
	}
	
	public double calcSpeedValueBetweenZeroAndOne(QVehicle veh, double inverseSimulatedFlowCapacity, double now, double freespeed){
//		log.error("  earliestLinkExitTime: " + veh.getEarliestLinkExitTime() + " inverseFlowCap: " + inverseSimulatedFlowCapacity);
		int cmp = (int) (veh.getEarliestLinkExitTime() + inverseSimulatedFlowCapacity + 2.0);
//		log.error("  now: " + now + " cmp: " + cmp);
		double speed = (now > cmp ? 0.0 : 1.0);
		return speed;
	}
	
	public  int guessLane(QVehicle veh, int numberOfLanes){
		int tmpLane;
		try {
			tmpLane = Integer.parseInt(veh.getId().toString()) ;
		} catch ( NumberFormatException ee ) {
			tmpLane = veh.getId().hashCode() ;
		}
		int lane = 1 + (tmpLane % numberOfLanes);
		return lane;
	}
	
	protected void createAndAddSnapshotInfoForPassengers(Collection<AgentSnapshotInfo> positions,
			Collection<PassengerAgent> passengers, double distanceOnLink, Link link, int lane, double speedValueBetweenZeroAndOne)
	{
		int cnt = passengers.size();
		for (PassengerAgent passenger : passengers) {
			AgentSnapshotInfo passengerPosition = snapshotInfoFactory.createAgentSnapshotInfo(passenger.getId(), link, 
					distanceOnLink, lane+2*cnt);
			passengerPosition.setColorValueBetweenZeroAndOne(speedValueBetweenZeroAndOne);
			passengerPosition.setAgentState(AgentState.PERSON_OTHER_MODE); // in 2010, probably a passenger
			positions.add(passengerPosition);
			cnt-- ;
		}
	}

	
	/**
	 * Returns all the people sitting in this vehicle.
	 *
	 * @param vehicle
	 * @param transitQueueLaneFeature
	 * @return All the people in this vehicle. If there is more than one, the first entry is the driver.
	 */
	protected List<MobsimAgent> getPeopleInVehicle(QVehicle vehicle) {
		ArrayList<MobsimAgent> people = new ArrayList<MobsimAgent>();
		people.add(vehicle.getDriver());
		if (vehicle instanceof TransitVehicle) {
			for (PassengerAgent passenger : ((TransitVehicle) vehicle).getPassengers()) {
				people.add((MobsimAgent) passenger);
			}
		}
		return people;
	}

	
}
