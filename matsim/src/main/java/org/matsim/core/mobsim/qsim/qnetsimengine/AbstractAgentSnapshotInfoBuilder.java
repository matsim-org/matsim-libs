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
package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo.AgentState;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;


/**
 * @author dgrether
 * @author knagel
 *
 */
abstract class AbstractAgentSnapshotInfoBuilder {

    private final AgentSnapshotInfoFactory snapshotInfoFactory;
	private Scenario scenario;
	
	AbstractAgentSnapshotInfoBuilder( Scenario sc, final AgentSnapshotInfoFactory agentSnapshotInfoFactory ){
        this.snapshotInfoFactory = agentSnapshotInfoFactory;
        this.scenario = sc ;
	}
	
	/**
	 * Put the vehicles from the waiting list in positions. Their actual position doesn't matter, PositionInfo provides a
	 * constructor for handling this situation.
	 */
	public int positionVehiclesFromWaitingList(final Collection<AgentSnapshotInfo> positions,
			final Link link, int cnt2, final Queue<QVehicle> waitingList) {
		for (QVehicle veh : waitingList) {
			Collection<Identifiable> peopleInVehicle = getPeopleInVehicle(veh);
			boolean first = true;
			for (Identifiable passenger : peopleInVehicle) {
				cnt2++ ;
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
		return cnt2 ;
	}

	public int positionAgentsInActivities(final Collection<AgentSnapshotInfo> positions, Link link,
			Collection<MobsimAgent> agentsInActivities,  int cnt2) {
		for (MobsimAgent pa : agentsInActivities) {
			AgentSnapshotInfo agInfo = snapshotInfoFactory.createAgentSnapshotInfo(pa.getId(), link, 0.9*link.getLength(), cnt2) ;
			agInfo.setAgentState( AgentState.PERSON_AT_ACTIVITY ) ;
			positions.add(agInfo) ;
			cnt2++ ;
		}
		return cnt2;
	}

	/**
	 * Put the transit vehicles from the transit stop list in positions.
	 * @param transitVehicleStopQueue 
	 */
	public int positionVehiclesFromTransitStop(final Collection<AgentSnapshotInfo> positions, Link link, Queue<QVehicle> transitVehicleStopQueue, int cnt2 ) {
		if (transitVehicleStopQueue.size() > 0) {
			for (QVehicle veh : transitVehicleStopQueue) {
				List<Identifiable> peopleInVehicle = getPeopleInVehicle(veh);
				boolean last = false ;
				cnt2 += peopleInVehicle.size() ;
				for ( ListIterator<Identifiable> it = peopleInVehicle.listIterator( peopleInVehicle.size() ) ; it.hasPrevious(); ) {
					Identifiable passenger = it.previous();
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
				cnt2 += peopleInVehicle.size() ; // setting it correctly for the next output

			}

		}
		return cnt2 ;
	}

	public void positionAgentOnLink(final Collection<AgentSnapshotInfo> positions, Coord startCoord, Coord endCoord,
			double lengthOfCurve, double euclideanLength, QVehicle veh, 
			double distanceFromFromNode,	Integer lane, double speedValueBetweenZeroAndOne){
		MobsimDriverAgent driverAgent = veh.getDriver();
		AgentSnapshotInfo pos = snapshotInfoFactory.createAgentSnapshotInfo(driverAgent.getId(), startCoord, endCoord, 
				distanceFromFromNode, lane, lengthOfCurve, euclideanLength);
		pos.setColorValueBetweenZeroAndOne(speedValueBetweenZeroAndOne);
		if (driverAgent instanceof TransitDriverAgent){
			pos.setAgentState(AgentState.TRANSIT_DRIVER);
		} else if ( driverAgent.getMode().equals(TransportMode.car)) {
			pos.setAgentState(AgentState.PERSON_DRIVING_CAR);
		} else {
			pos.setAgentState(AgentState.PERSON_OTHER_MODE );
		}
		if ( scenario.getPopulation().getPersonAttributes().getAttribute( driverAgent.getId().toString(), "marker" ) != null ) { 
			pos.setAgentState( AgentState.PERSON_OTHER_MODE ) ;
		}

		this.positionPassengers(positions, veh.getPassengers(), distanceFromFromNode, startCoord, 
				endCoord, lengthOfCurve, euclideanLength, lane+5, speedValueBetweenZeroAndOne);
		// (this is deliberately first memorizing "pos" but then filling in the passengers first)

		positions.add(pos);
	}
	
	public void positionVehiclesAlongLine(Collection<AgentSnapshotInfo> positions,
			double now, Collection<MobsimVehicle> vehs, double curvedLength, double storageCapacity, 
			double euklideanDistance, Coord upstreamCoord, Coord downstreamCoord, double inverseFlowCapPerTS, 
			double freeSpeed, int numberOfLanesAsInt)
	{
		double spacing = this.calculateVehicleSpacing( curvedLength, vehs.size(), storageCapacity );
		double freespeedTraveltime = curvedLength / freeSpeed ;

		double lastDistanceFromFromNode = Double.NaN;

		for ( MobsimVehicle mveh : vehs ) {
			QVehicle veh = (QVehicle) mveh ;
			double remainingTravelTime = veh.getEarliestLinkExitTime() - now ;
			double distanceFromFromNode = this.calculateDistanceOnVectorFromFromNode2(curvedLength, spacing,
					lastDistanceFromFromNode, now, freespeedTraveltime, remainingTravelTime);
			Integer lane = this.guessLane(veh, numberOfLanesAsInt );
			double speedValue = this.calcSpeedValueBetweenZeroAndOne(veh,
					inverseFlowCapPerTS, now, freeSpeed);
			this.positionAgentOnLink(positions, upstreamCoord, downstreamCoord,
						curvedLength, euklideanDistance, veh,
						distanceFromFromNode, lane, speedValue);
			lastDistanceFromFromNode = distanceFromFromNode;
		}
	}



	public void positionQItem(final Collection<AgentSnapshotInfo> positions, Coord startCoord, Coord endCoord, 
			double lengthOfCurve, double euclideanLength, QItem veh, 
			double distanceFromFromNode,	Integer lane, double speedValueBetweenZeroAndOne){
		AgentSnapshotInfo pos = snapshotInfoFactory.createAgentSnapshotInfo(Id.create("hole", Person.class), endCoord, startCoord, 
				distanceFromFromNode, lane, lengthOfCurve, euclideanLength);
		pos.setColorValueBetweenZeroAndOne(speedValueBetweenZeroAndOne);
		pos.setAgentState(AgentState.PERSON_OTHER_MODE );
		positions.add(pos);
	}
	
	public double calcSpeedValueBetweenZeroAndOne(QVehicle veh, double inverseSimulatedFlowCapacity, double now, double freespeed){
		int cmp = (int) (veh.getEarliestLinkExitTime() + inverseSimulatedFlowCapacity + 2.0);
		// "inverseSimulatedFlowCapacity" is there to keep vehicles green that only wait for capacity (i.e. have no vehicle
		// ahead). Especially important with small samples sizes.  This is debatable :-).  kai, jan'11
		
		double speed = (now > cmp ? 0.0 : 1.0);
		return speed;
	}
	
	public Integer guessLane(QVehicle veh, int numberOfLanes){
		Integer tmpLane;
		try {
			tmpLane = Integer.parseInt(veh.getId().toString()) ;
		} catch ( NumberFormatException ee ) {
			tmpLane = veh.getId().hashCode() ;
			if (tmpLane < 0 ){
				tmpLane = -tmpLane;
			}
		}
		int lane = 1 + (tmpLane % numberOfLanes);
		return lane;
	}
	
	void positionPassengers(Collection<AgentSnapshotInfo> positions,
                            Collection<? extends PassengerAgent> passengers, double distanceOnLink, Coord startCoord, Coord endCoord,
                            double lengthOfCurve, double euclideanLength, Integer lane, double speedValueBetweenZeroAndOne) {
		int cnt = passengers.size();
		int laneInt = 2*(cnt+1);
		if (lane != null){
			laneInt += lane;
		}
		for (PassengerAgent passenger : passengers) {
			int lanePos = laneInt - 2*cnt ;
			AgentSnapshotInfo passengerPosition = snapshotInfoFactory.createAgentSnapshotInfo(passenger.getId(), startCoord, endCoord, 
					distanceOnLink, lanePos, lengthOfCurve, euclideanLength);
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
	 * @return All the people in this vehicle. If there is more than one, the first entry is the driver.
	 */
    List<Identifiable> getPeopleInVehicle(QVehicle vehicle) {
		ArrayList<Identifiable> people = new ArrayList<>();
		people.add(vehicle.getDriver());
//		if (vehicle instanceof TransitVehicle) {
//			for (PassengerAgent passenger : ((TransitVehicle) vehicle).getPassengers()) {
//				people.add((MobsimAgent) passenger);
//			}
//		}
		for ( PassengerAgent passenger : vehicle.getPassengers() ) {
			people.add(passenger) ;
		}
		return people;
	}

    public abstract double calculateVehicleSpacing(double linkLength, double numberOfVehiclesOnLink, double overallStorageCapacity);

    /**
     * @param length
     * @param spacing
     * @param lastDistanceFromFromNode
     * @param now
     * @param freespeedTraveltime
     * @param remainingTravelTime
     * @return
     */
    public abstract double calculateDistanceOnVectorFromFromNode2(double length, double spacing, double lastDistanceFromFromNode, double now,
                                                                  double freespeedTraveltime, double remainingTravelTime);
}
