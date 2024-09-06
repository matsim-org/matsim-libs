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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QueueWithBuffer.Hole;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo.AgentState;
import org.matsim.vis.snapshotwriters.PositionInfo;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.vis.snapshotwriters.VisVehicle;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import static java.lang.Math.min;


/**
 * @author dgrether
 * @author knagel
 */
abstract class AbstractAgentSnapshotInfoBuilder {
	private static final Logger log = LogManager.getLogger(AbstractAgentSnapshotInfoBuilder.class);
	private static int wrnCnt = 0;

	private final Scenario scenario;
	private final SnapshotLinkWidthCalculator linkWidthCalculator;

	AbstractAgentSnapshotInfoBuilder(Scenario sc, SnapshotLinkWidthCalculator linkWidthCalculator) {
		this.scenario = sc;
		this.linkWidthCalculator = linkWidthCalculator;
	}

	private PositionInfo.LinkBasedBuilder newBuilder() {
		return new PositionInfo.LinkBasedBuilder().setLinkWidthCalculator(linkWidthCalculator);
	}

	private static double computeHolePositionAndReturnDistance(double freespeedTraveltime, Hole hole, double now, double curvedLength) {
		double remainingTravelTime = hole.getEarliestLinkExitTime() - now;
		return remainingTravelTime / freespeedTraveltime * curvedLength;
	}

	/**
	 * Put the vehicles from the waiting list in positions. Their actual position doesn't matter, PositionInfo provides a
	 * constructor for handling this situation.
	 */
	public final int positionVehiclesFromWaitingList(final Collection<AgentSnapshotInfo> positions,
													 final Link link, int cnt2, final Queue<QVehicle> waitingList) {
		return positionStack(positions, waitingList, cnt2);
	}

	public final int positionAgentsInActivities(final Collection<AgentSnapshotInfo> positions, Link link,
												Collection<? extends MobsimAgent> agentsInActivities, int cnt2) {
		var builder = newBuilder();
		builder.setVehicleId(null); // we don't have a vehicle in this case.
		for (MobsimAgent agent : agentsInActivities) {

			var position = builder
					.setPersonId(agent.getId())
					.setLinkId(link.getId())
					.setFromCoord(link.getFromNode().getCoord())
					.setToCoord(link.getToNode().getCoord())
					.setDistanceOnLink(link.getLength() * 0.9)
					.setLinkLength(link.getLength())
					.setLane(cnt2)
					.setAgentState(getAgentStateForActivity(agent.getId()))
					.build();
			positions.add(position);
			cnt2++ ;
		}
		return cnt2;
	}

	/**
	 * Put the transit vehicles from the transit stop list in positions.
	 */
	public final int positionVehiclesFromTransitStop(final Collection<AgentSnapshotInfo> positions, Link link,
													 Queue<QVehicle> transitVehicleStopQueue, int cnt2) {
		return positionStack(positions, transitVehicleStopQueue, cnt2);
	}

	public final void positionAgentGivenDistanceFromFNode(final Collection<AgentSnapshotInfo> positions, Coord startCoord, Coord endCoord,
														  double lengthOfCurve, QVehicle veh, double distanceFromFromNode,
														  int lane, double speedValueBetweenZeroAndOne) {
		// I think that the main reason why this exists as public method is that AssignmentEmulatingQLane wants to use it directly.
		// The reason for this, in return, is that positionVehiclesAlongLine(...) is a service method for queue models only.  kai, apr'16

		MobsimDriverAgent driverAgent = veh.getDriver();
		var builder = newBuilder();

		var position = builder
				.setPersonId(driverAgent.getId())
				.setVehicleId(veh.getId())
				.setLinkId(veh.getCurrentLink().getId())
				.setFromCoord(startCoord)
				.setToCoord(endCoord)
				.setDistanceOnLink(distanceFromFromNode)
				.setLane(lane)
				.setLinkLength(lengthOfCurve)
				.setAgentState(getAgentState(driverAgent))
				.setColorValue(speedValueBetweenZeroAndOne)
				.build();

		this.positionPassengers(positions, veh.getPassengers(), distanceFromFromNode, startCoord,
				endCoord, lengthOfCurve, lane + 5, speedValueBetweenZeroAndOne);
		// (this is deliberately first memorizing "pos" but then filling in the passengers first)

		positions.add(position);
	}

	public final Collection<AgentSnapshotInfo> positionVehiclesAlongLine( Collection<AgentSnapshotInfo> positions,
									      double now, Collection<? extends MobsimVehicle> vehs, double curvedLength, double storageCapacity,
									      Coord upstreamCoord, Coord downstreamCoord, double inverseFlowCapPerTS, double freeSpeed,
									      int numberOfLanesAsInt, Queue<Hole> holes, AbstractQLink.QLinkInternalInterface qLinkInternalInterface )
	{
		double spacingOfOnePCE = this.calculateVehicleSpacing( curvedLength, storageCapacity, vehs );
		// ("vehs" is needed since the link may be more than full because of squeezing.  In this case, spacingOfOnePCE is smaller than one "cell".)

		double ttimeOfHoles = curvedLength / (QueueWithBuffer.HOLE_SPEED_KM_H*1000./3600.);

		TreeMap<Double,Hole> consumableHoles = new TreeMap<>() ;

		// holes or kinematicWaves, if applicable:

		switch (scenario.getConfig().qsim().getSnapshotStyle()) {
			case equiDist:
			case queue:
				break;
			case withHoles:
			case withHolesAndShowHoles:
			case kinematicWaves:
				if ( !holes.isEmpty() ) {
					double firstHolePosition = Double.NaN ;
					double distanceOfHoleFromFromNode = Double.NaN ;
					double sum = 0 ;
					for (Hole hole : holes) {
						sum += hole.getSizeInEquivalents();
						distanceOfHoleFromFromNode = computeHolePositionAndReturnDistance(ttimeOfHoles, hole, now, curvedLength);
						if (Double.isNaN(firstHolePosition)) {
							firstHolePosition = distanceOfHoleFromFromNode;
							sum = 0; // don't include first vehicle
						}

						if (Math.round(distanceOfHoleFromFromNode) != Math.round(curvedLength)) {
							consumableHoles.put(distanceOfHoleFromFromNode, hole);
						} // else {
						// since hole is already created even if vehicle is in buffer, thus excluding such holes in vehicle position updating
						// probably, don't create hole in visualizer also. amit May 2016
						//}

						if (QSimConfigGroup.SnapshotStyle.withHolesAndShowHoles == scenario.getConfig().qsim().getSnapshotStyle()) {
							addHolePosition(positions, distanceOfHoleFromFromNode, hole, curvedLength, upstreamCoord, downstreamCoord);
						}
					}
					final double spaceConsumptionOfHoles = sum*spacingOfOnePCE;
					final double spaceAvailableForHoles = distanceOfHoleFromFromNode - firstHolePosition;
					if ( wrnCnt < 10 ) {
						wrnCnt++ ;
						if ( spaceConsumptionOfHoles > spaceAvailableForHoles ) {
							log.warn("we have a problem: holes consume too much space:" ) ;
							log.warn( "summed up space consumption of holes: " + spaceConsumptionOfHoles );
							log.warn("distance bw first and last hole: " + spaceAvailableForHoles ) ;
						}
						if (wrnCnt == 10) {
							log.warn(Gbl.FUTURE_SUPPRESSED ) ;
						}
					}
				}
				break;
			default: throw new RuntimeException("The traffic dynmics "+scenario.getConfig().qsim().getSnapshotStyle()+" is not implemented yet.");
		}

		// yyyyyy might be faster by sorting holes into a regular array list ...

		double distanceFromFromNode = Double.NaN;

		for ( MobsimVehicle mveh : vehs) {
			final QVehicle veh = (QVehicle) mveh;

			final double remainingTravelTime = veh.getEarliestLinkExitTime() - now;
			// (starts off relatively small (rightmost vehicle))

			final double vehicleSpacing = mveh.getSizeInEquivalents() * spacingOfOnePCE;

			double speed = min( freeSpeed, veh.getMaximumVelocity() );
			if ( qLinkInternalInterface!=null ){
				speed = qLinkInternalInterface.getMaximumVelocityFromLinkSpeedCalculator( veh, now );
			}

			distanceFromFromNode = this.calculateOdometerDistanceFromFromNode(
					now, curvedLength,
					speed, // min( freeSpeed, veh.getMaximumVelocity()),
					vehicleSpacing, distanceFromFromNode, remainingTravelTime
			);
			// yyyy if the LinkSpeedCalculator says something that is not free speed, we are out of luck here.  kai, jan'23

			int lane = VisUtils.guessLane(veh, numberOfLanesAsInt);
			double speedValue = VisUtils.calcSpeedValueBetweenZeroAndOne(veh, inverseFlowCapPerTS, now, freeSpeed);
			Gbl.assertNotNull(upstreamCoord);
			Gbl.assertNotNull(downstreamCoord);
			this.positionAgentGivenDistanceFromFNode(positions, upstreamCoord, downstreamCoord, curvedLength, veh, distanceFromFromNode, lane, speedValue);

			switch (this.scenario.getConfig().qsim().getTrafficDynamics()) {
				case queue:
					break;
				case withHoles:
				case kinematicWaves:
					while ( !consumableHoles.isEmpty() && distanceFromFromNode < consumableHoles.lastKey() ) {
						Map.Entry<Double, Hole> entry = consumableHoles.pollLastEntry() ;
						distanceFromFromNode -= spacingOfOnePCE * entry.getValue().getSizeInEquivalents() ;
					}
					break;
				default: throw new RuntimeException("The traffic dynmics "+this.scenario.getConfig().qsim().getTrafficDynamics()+" is not implemented yet.");
			}
		}

		/* Can't explain the above in easy words.  Essentially, when vehicles leave the link at max rate, there still must be some space between
		 * the holes that this generates.  That space is added up until a full vehicle fits into it.  There must be some better way of
		 * explaining this, but I don't know it right now.  kai, apr'16
		 */
		return positions;
	}


	abstract double calculateVehicleSpacing(double linkLength, double overallStorageCapacity, Collection<? extends VisVehicle> vehs);

	abstract double calculateOdometerDistanceFromFromNode(double time, double linkLength, double freespeed,
			double spacing, double prevVehicleDistance, double remainingTravelTime);

	private int positionStack(final Collection<AgentSnapshotInfo> positions, final Collection<QVehicle> vehicles, final int startCount) {
		var builder = newBuilder();

		var counter = startCount;
		for (var vehicle : vehicles) {
			var link = vehicle.getCurrentLink();
			for (var passenger : VisUtils.getPeopleInVehicle(vehicle)) {
				var position = builder
						.setPersonId(passenger.getId())
						.setVehicleId(vehicle.getId())
						.setLinkId(link.getId())
						.setFromCoord(link.getFromNode().getCoord())
						.setToCoord(link.getToNode().getCoord())
						.setDistanceOnLink(link.getLength() * 0.9)
						.setLane(counter)
						.setLinkLength(link.getLength())
						.setAgentState(getAgentState(passenger))
						.build();
				positions.add(position);
				counter++;
			}
		}
		return counter;
	}

	private void positionPassengers(Collection<AgentSnapshotInfo> positions,
									Collection<? extends PassengerAgent> passengers, double distanceOnLink, Coord startCoord, Coord endCoord,
									double lengthOfCurve, int lane, double speedValueBetweenZeroAndOne) {
		var builder = newBuilder();

		int cnt = passengers.size();
		int laneInt = 2 * (cnt + 1) + lane;

		for (PassengerAgent passenger : passengers) {
			int lanePos = laneInt - 2 * cnt;
			var passengerPosition = builder
					.setPersonId(passenger.getId())
					.setVehicleId(passenger.getVehicle().getId())
					.setLinkId(passenger.getCurrentLinkId())
					.setFromCoord(startCoord)
					.setToCoord(endCoord)
					.setDistanceOnLink(distanceOnLink)
					.setLane(lanePos)
					.setLinkLength(lengthOfCurve)
					.setColorValue(speedValueBetweenZeroAndOne)
					.setAgentState(getAgentState(passenger))
					.build();

			positions.add(passengerPosition);
			cnt--;
		}
	}

	private void addHolePosition(final Collection<AgentSnapshotInfo> positions, double distanceFromFromNode, Hole veh,
								 double curvedLength, Coord upstreamCoord, Coord downstreamCoord) {
		int lane = 20;
		double speedValue = 1.;
		var builder = newBuilder();

		var position = builder
				.setPersonId(Id.createPersonId("hole"))
				.setVehicleId(Id.createVehicleId(veh.getId()))
				.setLinkId(null)
				.setFromCoord(upstreamCoord)
				.setToCoord(downstreamCoord)
				.setDistanceOnLink(distanceFromFromNode)
				.setLane(lane)
				.setLinkLength(curvedLength)
				.setColorValue(speedValue)
				.setAgentState(AgentState.PERSON_OTHER_MODE)
				.build();
		positions.add(position);
	}

	private AgentState getAgentState(Identifiable<Person> identifiable) {

		// I don't know whether I have gotten this right, but I think this is tested in every case in every method
		var marker = getMarkerFromAttributes(identifiable.getId());
		if (marker != null) return AgentState.MARKER;

		// these are the regular agent states
		if (identifiable instanceof TransitDriverAgent) return AgentState.TRANSIT_DRIVER;
		if (identifiable instanceof MobsimDriverAgent && ((MobsimDriverAgent) identifiable).getMode().equals(TransportMode.car))
			return AgentState.PERSON_DRIVING_CAR;

		// old tests keep them here, since I'm unsure whether the other ones work
		//if (identifiable.getId().toString().startsWith("pt")) return AgentState.TRANSIT_DRIVER;
		//if (isFirst) return AgentState.PERSON_DRIVING_CAR;

		// we don't know. Set other mode
		return AgentState.PERSON_OTHER_MODE;
	}

	private Object getMarkerFromAttributes(Id<Person> id) {
		var person = scenario.getPopulation().getPersons().get(id);
		return person != null ? person.getAttributes().getAttribute(AgentSnapshotInfo.marker) : null;
	}

	private AgentState getAgentStateForActivity(Id<Person> id) {

		// I don't know whether I have gotten this right, but I think this is tested in every case in every method
		var marker = getMarkerFromAttributes(id);
		if (marker != null) return AgentState.MARKER;

		return AgentState.PERSON_AT_ACTIVITY;
	}
}
