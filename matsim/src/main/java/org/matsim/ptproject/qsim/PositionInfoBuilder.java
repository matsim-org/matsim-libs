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
package org.matsim.ptproject.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.pt.qsim.TransitQLaneFeature;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.PositionInfo;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo.AgentState;

/**
 * @author dgrether
 * 
 */
public class PositionInfoBuilder {

	private Link link;

	public void init(Link link) {
		this.link = link;
	}

	public int positionAgentsInActivities(final Collection<AgentSnapshotInfo> positions,
			Collection<PersonAgent> agentsInActivities, int cnt2) {
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
	 * 
	 * @param waitingList
	 * @param transitQueueLaneFeature
	 */
	public void positionVehiclesFromWaitingList(final Collection<AgentSnapshotInfo> positions,
			int cnt2, final Queue<QVehicle> waitingList, TransitQLaneFeature transitQueueLaneFeature) {
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
		Collection<PersonAgent> passengers = transitQueueLaneFeature
				.getPassengers(vehicle); // yy seems to me that "getPassengers" is a vehicle feature???
		if (passengers.isEmpty()) {
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
