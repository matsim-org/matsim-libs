/* *********************************************************************** *
 * project: org.matsim.*
 * SnapshotGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.events.algorithms;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.PositionInfo;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.vis.snapshotwriters.SnapshotWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class SnapshotGenerator implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkEnterEventHandler,
		LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, PersonStuckEventHandler, VehicleLeavesTrafficEventHandler {

	private final static Logger log = LogManager.getLogger(SnapshotGenerator.class);
	private final Network network;
	private int lastSnapshotIndex = -1;
	private final double snapshotPeriod;
	private final IdMap<Link, EventLink> eventLinks;
	private final ArrayList<EventLink> linkList;
	private final HashMap<Id<Person>, EventAgent> eventAgents;
	private final List<SnapshotWriter> snapshotWriters = new ArrayList<>();
	private final double capCorrectionFactor;
	private final double storageCapFactor;
	private final SnapshotStyle snapshotStyle;
	private final SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
	private final PositionInfo.LinkBasedBuilder builder = new PositionInfo.LinkBasedBuilder().setLinkWidthCalculator(linkWidthCalculator);

	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	public SnapshotGenerator(final Network network, final double snapshotPeriod, final QSimConfigGroup config) {
		this.network = network;
		int initialCapacity = (int) (network.getLinks().size() * 1.1);
		this.eventLinks = new IdMap<>(Link.class);
		this.linkList = new ArrayList<>(initialCapacity);
		this.eventAgents = new HashMap<>(1000, 0.95f);
		this.snapshotPeriod = snapshotPeriod;
		this.capCorrectionFactor = config.getFlowCapFactor() / network.getCapacityPeriod();
		this.storageCapFactor = config.getStorageCapFactor();
		this.snapshotStyle = config.getSnapshotStyle();

		if (!Double.isNaN(config.getLinkWidthForVis())) {
			this.linkWidthCalculator.setLinkWidthForVis(config.getLinkWidthForVis());
		} 
		if (! Double.isNaN(network.getEffectiveLaneWidth())){
			this.linkWidthCalculator.setLaneWidth(network.getEffectiveLaneWidth());
		}

		reset(-1);
	}

	public final void addSnapshotWriter(final SnapshotWriter writer) {
		this.snapshotWriters.add(writer);
	}

	public final boolean removeSnapshotWriter(final SnapshotWriter writer) {
		return this.snapshotWriters.remove(writer);
	}

	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		testForSnapshot(event.getTime());
		this.eventLinks.get(event.getLinkId()).departure(getEventAgent(event.getPersonId(), event.getTime()));
	}

	@Override
	public void handleEvent(final PersonArrivalEvent event) {
		testForSnapshot(event.getTime());
		this.eventLinks.get(event.getLinkId()).arrival(getEventAgent(event.getPersonId(), event.getTime()));
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		testForSnapshot(event.getTime());
		this.eventLinks.get(event.getLinkId()).enter(getEventAgent(delegate.getDriverOfVehicle(event.getVehicleId()), event.getTime()));
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		testForSnapshot(event.getTime());
		this.eventLinks.get(event.getLinkId()).leave(getEventAgent(delegate.getDriverOfVehicle(event.getVehicleId()), event.getTime()));
	}

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		testForSnapshot(event.getTime());
		this.eventLinks.get(event.getLinkId()).wait2link(getEventAgent(event.getPersonId(), event.getTime()));
		
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(final PersonStuckEvent event) {
		testForSnapshot(event.getTime());
		if (event.getLinkId() != null) { // link id is optional - agent can be teleporting or whatever.
			this.eventLinks.get(event.getLinkId()).stuck(getEventAgent(event.getPersonId(), event.getTime()));
		}
	}

	@Override
	public void reset(final int iteration) {
		this.eventLinks.clear();
		for (Link link : this.network.getLinks().values()) {
			final double effectiveCellSize;
			effectiveCellSize = this.network.getEffectiveCellSize();
			this.eventLinks.put(link.getId(), new EventLink(link, this.capCorrectionFactor, effectiveCellSize, this.storageCapFactor));
		}
		this.linkList.clear();
		this.linkList.addAll(eventLinks.values());
		this.eventAgents.clear();
		this.lastSnapshotIndex = -1;
		
		delegate.reset(iteration);
	}

	private EventAgent getEventAgent(final Id<Person> id, double time) {
		EventAgent agent = this.eventAgents.get(id);
		if (agent == null) {
			agent = new EventAgent(id, time);
			this.eventAgents.put(id, agent);
		}
		agent.time = time;
		return agent;
	}

	private void testForSnapshot(final double time) {
		int snapshotIndex = (int) (time / this.snapshotPeriod);
		if (this.lastSnapshotIndex == -1) {
			this.lastSnapshotIndex = snapshotIndex;
		}
		while (snapshotIndex > this.lastSnapshotIndex) {
			this.lastSnapshotIndex++;
			double snapshotTime = this.lastSnapshotIndex * this.snapshotPeriod;
			doSnapshot(snapshotTime);
		}
	}

	private void doSnapshot(final double time) {

			if (!this.snapshotWriters.isEmpty()) {
				Collection<AgentSnapshotInfo> positions = getVehiclePositions(time);
				for (SnapshotWriter writer : this.snapshotWriters) {
					writer.beginSnapshot(time);
					for (AgentSnapshotInfo position : positions) {
						writer.addAgent(position);
					}
					writer.endSnapshot();
				}
			}
	}

	private Collection<AgentSnapshotInfo> getVehiclePositions(final double time) {
		Collection<AgentSnapshotInfo> positions = new ArrayList<>();
		if (this.snapshotStyle == SnapshotStyle.queue) {
			for (EventLink link : this.linkList) {
				link.getVehiclePositionsQueue(positions, time, this.builder);
			}
		} else if (this.snapshotStyle == SnapshotStyle.equiDist) {
			for (EventLink link : this.linkList) {
				link.getVehiclePositionsEquil(positions, time, this.builder);
			}
		} else {
			// log statement to clarify: why only two snapshot styles. Amit Mar'17
			log.warn("Cannot generate snapshots offline (e.g., from events) for "+this.snapshotStyle
					+ ". This snapshot style is supported during simulation only.");
			throw new RuntimeException("The snapshotStyle \"" + this.snapshotStyle + "\" is not supported.");
		}
		return positions;
	}

	public final void finish() {
		for (SnapshotWriter writer : this.snapshotWriters) {
			writer.finish();
		}
	}

	private static class EventLink {
		private final Link link;
		private final List<EventAgent> drivingQueue;
		private final List<EventAgent> parkingQueue;
		private final List<EventAgent> waitingQueue;
		private final List<EventAgent> buffer;

		private final double euklideanDist;
		private final double freespeedTravelTime;
		private final double spaceCap;
		private final double storageCapFactor;
		private final double inverseTimeCap;

		private final double effectiveCellSize;

		private EventLink(final Link link2, final double capCorrectionFactor, final double effectiveCellSize, final double storageCapFactor) {
			this.link = link2;
			this.drivingQueue = new ArrayList<>();
			this.parkingQueue = new ArrayList<>();
			this.waitingQueue = new ArrayList<>();
			this.buffer = new ArrayList<>();
			this.euklideanDist = CoordUtils.calcEuclideanDistance(link2.getFromNode().getCoord(), link2.getToNode().getCoord());
			this.freespeedTravelTime = Math.ceil(this.link.getLength() / this.link.getFreespeed()) + 1;
			double timeCap = this.link.getCapacity() * capCorrectionFactor;
			this.storageCapFactor = storageCapFactor;
			this.inverseTimeCap = 1.0 / timeCap;
			this.effectiveCellSize = effectiveCellSize;
			this.spaceCap = (this.link.getLength() * this.link.getNumberOfLanes()) / this.effectiveCellSize * storageCapFactor;
		}

		private void enter(final EventAgent agent) {
			if (agent.currentLink != null) {
				agent.currentLink.stuck(agent); // use stuck to remove it from wherever it is
			}
			agent.currentLink = this;
			this.drivingQueue.add(agent);
		}

		private void leave(final EventAgent agent) {
			this.drivingQueue.remove(agent);
			this.buffer.remove(agent);
			agent.currentLink = null;
		}

		private void arrival(final EventAgent agent) {
			this.buffer.remove(agent);
			this.drivingQueue.remove(agent);
			this.parkingQueue.add(agent);
		}

		private void departure(final EventAgent agent) {
			agent.currentLink = this;
			this.parkingQueue.remove(agent);
			this.waitingQueue.add(agent);
		}

		private void wait2link(final EventAgent agent) {
			this.waitingQueue.remove(agent);
			this.buffer.add(agent);
		}

		private void stuck(final EventAgent agent) {
			// vehicles can be anywhere when they get stuck
			this.drivingQueue.remove(agent);
			this.parkingQueue.remove(agent);
			this.waitingQueue.remove(agent);
			this.buffer.remove(agent);
			agent.currentLink = null;
		}

		private static AgentSnapshotInfo createAgentSnapshotInfo(PositionInfo.LinkBasedBuilder builder, EventAgent agent, Link link, double distanceFromNode, AgentSnapshotInfo.AgentState agentState) {
			return builder
					.setPersonId(agent.id)
					.setLinkId(link.getId())
					//.setVehicleId(???) this should be set
					.setLane(agent.lane)
					.setDistanceOnLink(distanceFromNode)
					.setFromCoord(link.getFromNode().getCoord())
					.setToCoord(link.getToNode().getCoord())
					.setLinkLength(link.getLength())
					.setColorValue(agent.speed)
					.setAgentState(agentState)
					.build();
		}

		/**
		 * Calculates the positions of all vehicles on this link according to the queue-logic: Vehicles are placed on the link
		 * according to the ratio between the free-travel time and the time the vehicles are already on the link. If they could
		 * have left the link already (based on the time), the vehicles start to build a traffic-jam (queue) at the end of the link.
		 *
		 * @param positions A collection where the calculated positions can be stored.
		 * @param time      The current timestep
		 */
		private void getVehiclePositionsQueue(final Collection<AgentSnapshotInfo> positions, final double time, PositionInfo.LinkBasedBuilder builder) {
			double queueEnd = this.link.getLength(); // the length of the queue jammed vehicles build at the end of the link
			double vehLen = Math.min(    // the length of a vehicle in visualization
					this.euklideanDist / this.spaceCap, // all vehicles must have place on the link
					this.effectiveCellSize / this.storageCapFactor); // a vehicle should not be larger than it's actual size

			// put all cars in the buffer one after the other
			for (EventAgent agent : this.buffer) {

				agent.lane = 1 + (agent.intId % NetworkUtils.getNumberOfLanesAsInt(time, this.link));
				int cmp = (int) (agent.time + this.freespeedTravelTime + this.inverseTimeCap + 2.0);
				agent.speed = (time > cmp) ? 0.0 : this.link.getFreespeed(time);
				var position = createAgentSnapshotInfo(builder, agent, link, queueEnd, AgentSnapshotInfo.AgentState.PERSON_DRIVING_CAR);
				positions.add(position);
				queueEnd -= vehLen;
			}

			/* place other driving cars according the following rule:
			 * - calculate the time how long the vehicle is on the link already
			 * - calculate the position where the vehicle should be if it could drive with freespeed
			 * - if the position is already within the congestion queue, add it to the queue with slow speed
			 * - if the position is not within the queue, just place the car with free speed at that place
			 */
			double lastDistance = Integer.MAX_VALUE;
			for (EventAgent agent : this.drivingQueue) {
				double travelTime = time - agent.time;
				double distanceOnLink = (this.freespeedTravelTime == 0.0 ? 0.0 : ((travelTime / this.freespeedTravelTime) * this.euklideanDist));
				if (distanceOnLink > queueEnd) { // vehicle is already in queue
					distanceOnLink = queueEnd;
					queueEnd -= vehLen;
				}
				if (distanceOnLink >= lastDistance) {
					/* we have a queue, so it should not be possible that one vehicles overtakes another.
					 * additionally, if two vehicles entered at the same time, they would be drawn on top of each other.
					 * we don't allow this, so in this case we put one after the other. Theoretically, this could lead to
					 * vehicles placed at negative distance when a lot of vehicles all enter at the same time on an empty
					 * link. not sure what to do about this yet... just setting them to 0 currently.
					 */
					distanceOnLink = lastDistance - vehLen;
					if (distanceOnLink < 0) distanceOnLink = 0.0;
				}
				int cmp = (int) (agent.time + this.freespeedTravelTime + this.inverseTimeCap + 2.0);
				agent.speed = (time > cmp) ? 0.0 : this.link.getFreespeed(time);
				agent.lane = 1 + (agent.intId % NetworkUtils.getNumberOfLanesAsInt(this.link));
				var position = createAgentSnapshotInfo(builder, agent, link, queueEnd, AgentSnapshotInfo.AgentState.PERSON_DRIVING_CAR);
				positions.add(position);
				lastDistance = distanceOnLink;
			}

			/* Put the vehicles from the waiting list in positions.
			 * Their actual position doesn't matter, so they are just placed
			 * to the coordinates of the from node */
			int lane = NetworkUtils.getNumberOfLanesAsInt(this.link) + 1; // place them next to the link
			for (EventAgent agent : this.waitingQueue) {

				agent.lane = lane;
				agent.speed = 0;
				var position = createAgentSnapshotInfo(builder, agent, link, queueEnd, AgentSnapshotInfo.AgentState.PERSON_AT_ACTIVITY);
				positions.add(position);
			}

			/* put the vehicles from the parking list in positions
			 * their actual position doesn't matter, so they are just placed
			 * to the coordinates of the from node */
			lane = NetworkUtils.getNumberOfLanesAsInt(this.link) + 2; // place them next to the link
			for (EventAgent agent : this.parkingQueue) {
				agent.lane = lane;
				agent.speed = 0;
				var position = createAgentSnapshotInfo(builder, agent, link, queueEnd, AgentSnapshotInfo.AgentState.PERSON_AT_ACTIVITY);
				positions.add(position);
			}
		}

		/**
		 * Calculates the positions of all vehicles on this link so that there is always the same distance between following cars.
		 * A single vehicle will be placed at the middle (0.5) of the link, two cars will be placed at positions 0.25 and 0.75,
		 * three cars at positions 0.16, 0.50, 0.83, and so on.
		 *
		 * @param positions A collection where the calculated positions can be stored.
		 * @param time      The current timestep
		 */
		private void getVehiclePositionsEquil(final Collection<AgentSnapshotInfo> positions, final double time, PositionInfo.LinkBasedBuilder builder) {
			int bufferSize = this.buffer.size();
			int drivingQueueSize = this.drivingQueue.size();
			int waitingQueueSize = this.waitingQueue.size();
			int parkingQueueSize = this.parkingQueue.size();
			if (bufferSize + drivingQueueSize + waitingQueueSize + parkingQueueSize > 0) {
				int cnt = bufferSize + drivingQueueSize;
				int nLanes = NetworkUtils.getNumberOfLanesAsInt(time, this.link);
				double linkLength = this.link.getLength();
				if (cnt > 0) {
					double cellSize = linkLength / cnt;
					double distFromFromNode = linkLength - cellSize / 2.0;
					double freespeed = this.link.getFreespeed(time);

					// the cars in the buffer
					for (EventAgent agent : this.buffer) {
						agent.lane = 1 + agent.intId % nLanes;
						int cmp = (int) (agent.time + this.freespeedTravelTime + this.inverseTimeCap + 2.0);
						if (time > cmp) {
							agent.speed = 0.0;
						} else {
							agent.speed = freespeed;
						}

						var position = createAgentSnapshotInfo(builder, agent, link, distFromFromNode, AgentSnapshotInfo.AgentState.PERSON_DRIVING_CAR);
						positions.add(position);
						distFromFromNode -= cellSize;
					}

					// the cars in the drivingQueue
					for (EventAgent agent : this.drivingQueue) {
						agent.lane = 1 + agent.intId % nLanes;
						int cmp = (int) (agent.time + this.freespeedTravelTime + this.inverseTimeCap + 2.0);
						if (time > cmp) {
							agent.speed = 0.0;
						} else {
							agent.speed = freespeed;
						}

						var position = createAgentSnapshotInfo(builder, agent, link, distFromFromNode, AgentSnapshotInfo.AgentState.PERSON_DRIVING_CAR);
						positions.add(position);
						distFromFromNode -= cellSize;
					}
				}

				// the cars in the waitingQueue
				// the actual position doesn't matter, so they're just placed next to the link at the end
				if (waitingQueueSize > 0) {
					int lane = nLanes + 2;
					double cellSize = Math.min(this.effectiveCellSize, linkLength / waitingQueueSize);
					double distFromFromNode = linkLength - cellSize / 2.0;
					for (EventAgent agent : this.waitingQueue) {
						agent.lane = lane;
						agent.speed = 0.0;
						var position = createAgentSnapshotInfo(builder, agent, link, distFromFromNode, AgentSnapshotInfo.AgentState.PERSON_AT_ACTIVITY);
						positions.add(position);
						distFromFromNode -= cellSize;
					}
				}

				// the cars in the parkingQueue
				// the actual position  doesn't matter, so they're distributed next to the link
				if (parkingQueueSize > 0) {
					int lane = nLanes + 4;
					double cellSize = linkLength / parkingQueueSize;
					double distFromFromNode = linkLength - cellSize / 2.0;
					for (EventAgent agent : this.parkingQueue) {
						agent.lane = lane;
						agent.speed = 0.0;
						var position = createAgentSnapshotInfo(builder, agent, link, distFromFromNode, AgentSnapshotInfo.AgentState.PERSON_AT_ACTIVITY);
						positions.add(position);
						distFromFromNode -= cellSize;
					}
				}
			}
		}
	}

	private static class EventAgent implements Comparable<EventAgent> {
		protected final Id<Person> id;
		protected final int intId;
		protected double time;
		protected EventLink currentLink = null;
		protected double speed = 0.0;
		protected int lane = 1;

		EventAgent(final Id<Person> id, final double time) {
			this.id = id;
			this.time = time;
			this.intId = id.hashCode();
		}

		@Override
		public int compareTo(final EventAgent o) {
			return this.id.compareTo(o.id);
		}

		@Override
		public boolean equals(final Object o) {
			if (o instanceof EventAgent) {
				return this.id.equals(((EventAgent) o).id);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.id.hashCode();
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
	}
}
