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

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QueueWithBuffer.Hole;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo.AgentState;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.vis.snapshotwriters.VisVehicle;


/**
 * @author dgrether
 * @author knagel
 *
 */
abstract class AbstractAgentSnapshotInfoBuilder {

	private final AgentSnapshotInfoFactory snapshotInfoFactory;
	private Scenario scenario;

	AbstractAgentSnapshotInfoBuilder( Scenario sc, SnapshotLinkWidthCalculator linkWidthCalculator ){
		this.snapshotInfoFactory = new AgentSnapshotInfoFactory( linkWidthCalculator );
		this.scenario = sc ;
	}

	/**
	 * Put the vehicles from the waiting list in positions. Their actual position doesn't matter, PositionInfo provides a
	 * constructor for handling this situation.
	 */
	public final int positionVehiclesFromWaitingList(final Collection<AgentSnapshotInfo> positions,
			final Link link, int cnt2, final Queue<QVehicle> waitingList) {
		for (QVehicle veh : waitingList) {
			Collection<Identifiable<?>> peopleInVehicle = VisUtils.getPeopleInVehicle(veh);
			boolean first = true;
			for (Identifiable passenger : peopleInVehicle) {
				cnt2++ ;
				AgentSnapshotInfo passengerPosition = snapshotInfoFactory.createAgentSnapshotInfo(passenger.getId(), link, 
						0.9*link.getLength(), cnt2); // for the time being, same position as facilities
				if (passenger.getId().toString().startsWith("pt")) {
					passengerPosition.setAgentState(AgentState.TRANSIT_DRIVER);
				} else if (first) {
					passengerPosition.setAgentState(AgentState.PERSON_DRIVING_CAR);
				} else {
					passengerPosition.setAgentState(AgentState.PERSON_OTHER_MODE);
				}
				positions.add(passengerPosition);
				first = false;
			}
		}
		return cnt2 ;
	}

	public final int positionAgentsInActivities(final Collection<AgentSnapshotInfo> positions, Link link,
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
	public final int positionVehiclesFromTransitStop(final Collection<AgentSnapshotInfo> positions, Link link, 
			Queue<QVehicle> transitVehicleStopQueue, int cnt2 ) {
		if (transitVehicleStopQueue.size() > 0) {
			for (QVehicle veh : transitVehicleStopQueue) {
				List<Identifiable<?>> peopleInVehicle = VisUtils.getPeopleInVehicle(veh);
				boolean last = false ;
				cnt2 += peopleInVehicle.size() ;
				for ( ListIterator<Identifiable<?>> it = peopleInVehicle.listIterator( peopleInVehicle.size() ) ; it.hasPrevious(); ) {
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

	public final void positionAgentGivenDistanceFromFNode(final Collection<AgentSnapshotInfo> positions, Coord startCoord, Coord endCoord,
			double lengthOfCurve, QVehicle veh, double distanceFromFromNode, 
			Integer lane,	double speedValueBetweenZeroAndOne){
		// I think that the main reason why this exists as public method is that AssignmentEmulatingQLane wants to use it directly.
		// The reason for this, in return, is that positionVehiclesAlongLine(...) is a service method for queue models only.  kai, apr'16
		
		MobsimDriverAgent driverAgent = veh.getDriver();
		AgentSnapshotInfo pos = snapshotInfoFactory.createAgentSnapshotInfo(driverAgent.getId(), startCoord, endCoord, 
				distanceFromFromNode, lane, lengthOfCurve);
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
				endCoord, lengthOfCurve, lane+5, speedValueBetweenZeroAndOne);
		// (this is deliberately first memorizing "pos" but then filling in the passengers first)

		positions.add(pos);
	}

	public final Collection<AgentSnapshotInfo> positionVehiclesAlongLine(Collection<AgentSnapshotInfo> positions,
			double now, Collection<MobsimVehicle> vehs, double curvedLength, double storageCapacity, 
			Coord upstreamCoord, Coord downstreamCoord, double inverseFlowCapPerTS, double freeSpeed, 
			int numberOfLanesAsInt, Queue<Hole> holes)
	{
		double spacingOfOnePCE = this.calculateVehicleSpacing( curvedLength, storageCapacity, vehs );

		double ttimeOfHoles = curvedLength / (QueueWithBuffer.HOLE_SPEED_KM_H*1000./3600.);

		TreeMap<Double,Hole> consumableHoles = new TreeMap<>() ;
		
		// holes, if applicable:
		if ( QSimConfigGroup.SnapshotStyle.withHoles==scenario.getConfig().qsim().getSnapshotStyle() ) {
			if ( !holes.isEmpty() ) {
				double firstHolePosition = Double.NaN ;
				double distanceOfHoleFromFromNode = Double.NaN ;
				double sum = 0 ;
				for (Hole hole : holes) {
					sum += hole.getSizeInEquivalents() ;
					distanceOfHoleFromFromNode = computeHolePositionAndReturnDistance( ttimeOfHoles, hole, now, curvedLength);
					if ( Double.isNaN( firstHolePosition ) ) {
						firstHolePosition = distanceOfHoleFromFromNode ;
						sum = 0 ; // don't include first vehicle
					}
					
					if ( Math.round(distanceOfHoleFromFromNode) == Math.round(curvedLength) ) {
						// since hole is already created even if vehicle is in buffer, thus excluding such holes in vehicle position updating
						// probably, don't create hole in visualizer also. amit May 2016						
					} else {
						consumableHoles.put( distanceOfHoleFromFromNode, hole ) ;
					}

					addHolePosition( positions, distanceOfHoleFromFromNode, hole, curvedLength, upstreamCoord, downstreamCoord ) ;
//					consumableHoles.put( distanceOfHoleFromFromNode, hole ) ;
				}
				final double spaceConsumptionOfHoles = sum*spacingOfOnePCE;
				final double spaceAvailableForHoles = distanceOfHoleFromFromNode - firstHolePosition;
				if ( spaceConsumptionOfHoles >= spaceAvailableForHoles ) {
					Logger.getLogger(getClass()).warn("we have a problem: holes consume too much space:" ) ;
					Logger.getLogger(getClass()).warn( "summed up space consumption of holes: " + spaceConsumptionOfHoles );
					Logger.getLogger(getClass()).warn("distance bw first and last hole: " + spaceAvailableForHoles ) ; 

				}
			}
		}
		
		// yyyyyy might be faster by sorting holes into a regular array list ...

		double freespeedTraveltime = curvedLength / freeSpeed ;

		double distanceFromFromNode = Double.NaN;

		for ( MobsimVehicle mveh : vehs ) {
			final QVehicle veh = (QVehicle) mveh ;

			final double remainingTravelTime = veh.getEarliestLinkExitTime() - now ;
			// (starts off relatively small (rightmost vehicle))
			
			final double vehicleSpacing = mveh.getSizeInEquivalents()*spacingOfOnePCE;
			distanceFromFromNode = this.calculateOdometerDistanceFromFromNode(curvedLength, vehicleSpacing , distanceFromFromNode, 
					now, freespeedTraveltime, remainingTravelTime);
			// (starts off relatively large (rightmost vehicle))
			
			Integer lane = VisUtils.guessLane(veh, numberOfLanesAsInt );
			double speedValue = VisUtils.calcSpeedValueBetweenZeroAndOne(veh, inverseFlowCapPerTS, now, freeSpeed);
			Gbl.assertNotNull( upstreamCoord ) ;
			Gbl.assertNotNull( downstreamCoord ) ;
			this.positionAgentGivenDistanceFromFNode(positions, upstreamCoord, downstreamCoord, curvedLength, veh, distanceFromFromNode, lane, speedValue);

			if ( this.scenario.getConfig().qsim().getTrafficDynamics()==TrafficDynamics.withHoles ) {
				while ( !consumableHoles.isEmpty() && distanceFromFromNode < consumableHoles.lastKey() ) {
					Entry<Double, Hole> entry = consumableHoles.pollLastEntry() ;
					distanceFromFromNode -= spacingOfOnePCE * entry.getValue().getSizeInEquivalents() ;
				}
			}
		}
		
		/* Can't explain the above in easy words.  Essentially, when vehicles leave the link at max rate, there still must be some space between
		 * the holes that this generates.  That space is added up until a full vehicle fits into it.  There must be some better way of
		 * explaining this, but I don't know it right now.  kai, apr'16
		 */
		
		
		return positions;
		
	}



	 private static double computeHolePositionAndReturnDistance(double freespeedTraveltime, Hole hole, double now, double curvedLength) 
	{
		double remainingTravelTime = hole.getEarliestLinkExitTime() - now ;
		double distanceFromFromNode = remainingTravelTime/freespeedTraveltime * curvedLength ;
		return distanceFromFromNode;
	}
		
	private void addHolePosition(final Collection<AgentSnapshotInfo> positions, double distanceFromFromNode, Hole veh, 
			double curvedLength, Coord upstreamCoord, Coord downstreamCoord)
	{
		Integer lane = 20 ;
		double speedValue = 1. ;
		AgentSnapshotInfo pos = this.snapshotInfoFactory.createAgentSnapshotInfo(Id.create("hole", Person.class), upstreamCoord, downstreamCoord, 
				distanceFromFromNode, lane, curvedLength);
		pos.setColorValueBetweenZeroAndOne(speedValue);
		pos.setAgentState(AgentState.PERSON_OTHER_MODE );
		positions.add(pos);
	}
	
	final void positionPassengers(Collection<AgentSnapshotInfo> positions,
			Collection<? extends PassengerAgent> passengers, double distanceOnLink, Coord startCoord, Coord endCoord,
			double lengthOfCurve, Integer lane, double speedValueBetweenZeroAndOne) {
		int cnt = passengers.size();
		int laneInt = 2*(cnt+1);
		if (lane != null){
			laneInt += lane;
		}
		for (PassengerAgent passenger : passengers) {
			int lanePos = laneInt - 2*cnt ;
			AgentSnapshotInfo passengerPosition = snapshotInfoFactory.createAgentSnapshotInfo(passenger.getId(), startCoord, endCoord, 
					distanceOnLink, lanePos, lengthOfCurve);
			passengerPosition.setColorValueBetweenZeroAndOne(speedValueBetweenZeroAndOne);
			passengerPosition.setAgentState(AgentState.PERSON_OTHER_MODE); // in 2010, probably a passenger
			positions.add(passengerPosition);
			cnt-- ;
		}
	}

	public abstract double calculateVehicleSpacing(double linkLength, double overallStorageCapacity, Collection<? extends VisVehicle> vehs);

	public abstract double calculateOdometerDistanceFromFromNode(double length, double spacing, double lastDistanceFromFromNode, 
			double now, double freespeedTraveltime, double remainingTravelTime);
}
