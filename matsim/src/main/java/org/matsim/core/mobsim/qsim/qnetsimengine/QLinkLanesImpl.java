/* *********************************************************************** *
 * project: org.matsim.*
 * QueueLink.java
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.misc.Time;
import org.matsim.lanes.ModelLane;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.vis.VisLane;
import org.matsim.lanes.vis.VisLaneModelBuilder;
import org.matsim.lanes.vis.VisLinkWLanes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.vis.snapshotwriters.VisData;

/**
 * Please read the docu of QBufferItem, QLane, QLinkInternalI (arguably to be renamed into something
 * like AbstractQLink) and QLinkImpl jointly. kai, nov'11
 * 
 * @author dstrippgen
 * @author dgrether
 * @author mrieser
 * 
 *         A QLinkLanes can consist of one or more QLanes, which may have the following layout
 *         (Dashes stand for the borders of the QLink, equal signs (===) depict one lane, plus signs
 *         (+) symbolize a decision point where one lane splits into several lanes) :
 * 
 *         <pre>
 * ----------------
 * ================
 * ----------------
 * </pre>
 * 
 *         <pre>
 * ----------------
 *          =======
 * ========+
 *          =======
 * ----------------
 * </pre>
 * 
 *         <pre>
 * ----------------
 *         							========
 * =======+========
 *         							========
 * ----------------
 * </pre>
 * 
 * 
 *         The following layouts are not allowed:
 * 
 *         <pre>
 * ----------------
 * ================
 * ================
 * ----------------
 * </pre>
 * 
 *         <pre>
 * ----------------
 * =======
 *        +========
 * =======
 * ----------------
 * </pre>
 * 
 *         Queue Model Link implementation with the following properties:
 *         <ul>
 *         <li>The queue behavior itself is simulated by one or more instances of QueueLane</li>
 *         <li>Each QueueLink has at least one QueueLane. All QueueVehicles added to the QueueLink
 *         are placed on this instance.</li>
 *         <li>All QueueLane instances which are connected to the ToQueueNode are held in the
 *         attribute toNodeQueueLanes</li>
 *         <li>QueueLink is active as long as at least one of its QueueLanes is active.</li>
 *         </ul>
 */
public final class QLinkLanesImpl extends AbstractQLink {

	private static final Logger log = Logger.getLogger(QLinkLanesImpl.class);
	/**
	 * Reference to the QueueNode which is at the end of each QueueLink instance
	 */
	private final QNode toQueueNode;
	/**
	 * The QueueLane instance which always exists.
	 */
	private QLaneI firstLaneQueue;
	/**
	 * A List holding all QueueLane instances of the QueueLink
	 */

	private final LinkedHashMap<Id<Lane>, QLaneI> laneQueues;

	/**
	 * If more than one QueueLane exists this list holds all QueueLanes connected to the
	 * (To)QueueNode of the QueueLink
	 */
	private List<QLaneI> toNodeLaneQueues = null;

	private VisData visdata = null;

	private final List<ModelLane> lanes;

	private final Map<Id<Lane>, Map<Id<Link>, List<QLaneI>>> nextQueueToLinkCache;

	/**
	 * Initializes a QueueLink with one QueueLane.
	 */
	QLinkLanesImpl(final Link link2, QNetwork network, final QNode toNode, List<ModelLane> lanes) {
		super(link2, network);
		this.toQueueNode = toNode;
		this.laneQueues = new LinkedHashMap<>();
		this.toNodeLaneQueues = new ArrayList<>();
		this.lanes = lanes;
		this.nextQueueToLinkCache = new LinkedHashMap<>(); // maps a lane id to a map containing the
															// downstream queues indexed by a
															// downstream link
		this.initLaneQueues(lanes);
		this.visdata = this.new VisDataImpl();
		this.transitQLink = new TransitQLink(this.firstLaneQueue);
	}

	private void initLaneQueues(List<ModelLane> lanes) {
		Stack<QLaneI> stack = new Stack<>();
		Map<Id<Lane>, QLaneI> queueByIdMap = new HashMap<>();
		Map<Id<Lane>, Set<Id<Link>>> laneIdToLinksMap = new HashMap<>();
		for (ModelLane lane : lanes) { // lanes is sorted downstream to upstream
			Id<Lane> laneId = lane.getLaneData().getId();
			double noEffectiveLanes = lane.getLaneData().getNumberOfRepresentedLanes();
			// --
			// QLaneI queue = new QueueWithBuffer(this, new FIFOVehicleQ(), laneId,
			// lane.getLength(), noEffectiveLanes,
			// (lane.getLaneData().getCapacityVehiclesPerHour()/3600.0));
			QueueWithBuffer.Builder builder = new QueueWithBuffer.Builder(this);
			builder.setVehicleQueue(new FIFOVehicleQ());
			builder.setId(laneId);
			builder.setLength(lane.getLength());
			builder.setEffectiveNumberOfLanes(noEffectiveLanes);
			builder.setFlowCapacity_s(lane.getLaneData().getCapacityVehiclesPerHour() / 3600.);
			QLaneI queue = builder.build();
			// --
			((QueueWithBuffer) queue).generatingEvents = true;
			queueByIdMap.put(laneId, queue);
			firstLaneQueue = queue;
			stack.push(queue);
			Set<Id<Link>> toLinkIds = new HashSet<>();

			if (lane.getToLanes() == null || lane.getToLanes().isEmpty()) { // lane is at the end of
																			// link
				this.toNodeLaneQueues.add(queue);
				toLinkIds.addAll(lane.getLaneData().getToLinkIds());
				laneIdToLinksMap.put(laneId, toLinkIds);
			} else { // lane is within the link and has no connection to a node
				Map<Id<Link>, List<QLaneI>> toLinkIdDownstreamQueues = new LinkedHashMap<>();
				nextQueueToLinkCache.put(Id.create(((QueueWithBuffer) queue).getId(), Lane.class),
						toLinkIdDownstreamQueues);
				for (ModelLane toLane : lane.getToLanes()) {
					Set<Id<Link>> toLinks = laneIdToLinksMap.get(toLane.getLaneData().getId());
					if (!laneIdToLinksMap.containsKey(laneId)) {
						laneIdToLinksMap.put(laneId, new HashSet<Id<Link>>());
					}
					laneIdToLinksMap.get(laneId).addAll(toLinks);
					for (Id<Link> toLinkId : toLinks) {
						List<QLaneI> downstreamQueues = toLinkIdDownstreamQueues.get(toLinkId);
						if (downstreamQueues == null) {
							downstreamQueues = new ArrayList<>();
							toLinkIdDownstreamQueues.put(toLinkId, downstreamQueues);
						}
						downstreamQueues.add(queueByIdMap.get(toLane.getLaneData().getId()));
					}
				}
			}
			queue.recalcTimeVariantAttributes(Time.UNDEFINED_TIME);
		}
		// reverse the order in the linked map, i.e. upstream to downstream
		while (!stack.isEmpty()) {
			QLaneI queue = stack.pop();
			this.laneQueues.put(((QueueWithBuffer) queue).getId(), queue);
		}
	}

	@Override
	List<QLaneI> getToNodeQueueLanes() {
		return this.toNodeLaneQueues;
	}

	/**
	 * Adds a vehicle to the link.
	 * 
	 * @param veh
	 *            the vehicle
	 */
	@Override
	void addFromUpstream(final QVehicle veh) {
		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		activateLink();
		this.firstLaneQueue.addFromUpstream(veh);
		this.network.simEngine
				.getMobsim()
				.getEventsManager()
				.processEvent(
						new LaneEnterEvent(now, veh.getId(), this.getLink().getId(),
								((QueueWithBuffer) this.firstLaneQueue).getId()));

		veh.setCurrentLink(this.getLink());
		this.network.simEngine
				.getMobsim()
				.getEventsManager()
				.processEvent(
						new LinkEnterEvent(now, veh.getId(), this.getLink().getId()));
	}

	@Override
	void clearVehicles() {
		super.clearVehicles();
		for (QLaneI lane : this.laneQueues.values()) {
			lane.clearVehicles();
		}
	}

	@Override
	boolean doSimStep(double now) {
		boolean lanesActive = false;
		boolean movedWaitToRoad = false;
		if (this.insertingWaitingVehiclesBeforeDrivingVehicles) {
			this.moveWaitToRoad(now);
			this.transitQLink.handleTransitVehiclesInStopQueue(now);
			lanesActive = this.moveLanes(now);
		} else {
			this.transitQLink.handleTransitVehiclesInStopQueue(now);
			lanesActive = this.moveLanes(now);
			movedWaitToRoad = this.moveWaitToRoad(now);
		}
		this.active = lanesActive || movedWaitToRoad || (!this.waitingList.isEmpty())
				|| !this.transitQLink.getTransitVehicleStopQueue().isEmpty();
		return this.active;
	}

	private boolean moveLanes(double now) {
		boolean activeLane = false;
		for (QLaneI queue : this.laneQueues.values()) {
			queue.updateRemainingFlowCapacity();
			
			/* part A */
			if (!this.toNodeLaneQueues.contains(queue)) {
				// move vehicles from the lane buffer to the next lane
				this.moveBufferToNextLane(now, queue);
			} else {
				// move vehicles from the lane buffer to the link buffer, i.e. prepare moving them
				// to the next link
				((QueueWithBuffer) queue).moveQueueToBuffer(now);
			}
			/* end of part A */
			
			/* part B */
			// move vehicles to the lane buffer if they have reached their earliest lane exit time
			queue.doSimStep(now);
			/* end of part B */
			
			activeLane = activeLane || queue.isActive();
			
			/*
			 * Remark: The order of part A and B influences the travel time on lanes.
			 * 
			 * Before Jul'15 order B, A was used, such that travel time on lanes was practically
			 * rounded down. This has the unintended effect that introducing lanes on a link may
			 * decrease its travel time: Dividing the link into sufficient many lanes (each one
			 * shorter than the number of meters that an agent may travel in 1 second) reduces the
			 * link travel time to 1 second.
			 * 
			 * Order A, B produces the same behavior for lanes as for links. I.e. up to one
			 * additional second may occur for every lane, because travel times are now practically
			 * rounded up.
			 * 
			 * Theresa, Jul'15
			 */
		}
		return activeLane;
	}

	private void moveBufferToNextLane(final double now, QLaneI queue) {
		QVehicle veh;
		while (!queue.isNotOfferingVehicle()) {
			veh = queue.getFirstVehicle();
			Id<Link> toLinkId = veh.getDriver().chooseNextLinkId();
			QLaneI nextQueue = this.chooseNextLane(queue, toLinkId);
			if (nextQueue != null) {
				if (nextQueue.isAcceptingFromUpstream()) {
					((QueueWithBuffer) queue).removeFirstVehicle();
					this.network.simEngine
							.getMobsim()
							.getEventsManager()
							.processEvent(
									new LaneLeaveEvent(now, veh.getId(), this.getLink()
											.getId(), ((QueueWithBuffer) queue).getId()));
					nextQueue.addFromUpstream(veh);
					this.network.simEngine
							.getMobsim()
							.getEventsManager()
							.processEvent(
									new LaneEnterEvent(now, veh.getId(), this.getLink()
											.getId(), ((QueueWithBuffer) nextQueue).getId()));
				} else {
					break;
				}
			} else {
				StringBuilder b = new StringBuilder();
				b.append("Person Id: ");
				b.append(veh.getDriver().getId());
				b.append(" is on Lane Id ");
				b.append(((QueueWithBuffer) queue).getId());
				b.append(" on Link Id ");
				b.append(this.getLink().getId());
				b.append(" and wants to drive to Link Id ");
				b.append(toLinkId);
				b.append(" but there is no Lane leading to that Link!");
				log.error(b.toString());
				throw new IllegalStateException(b.toString());
			}
		}
	}

	private QLaneI chooseNextLane(QLaneI queue, Id<Link> toLinkId) {
		List<QLaneI> toQueues = this.nextQueueToLinkCache.get(((QueueWithBuffer) queue).getId())
				.get(toLinkId);
		QLaneI retLane = toQueues.get(0);
		if (toQueues.size() == 1) {
			return retLane;
		}
		// else chose lane by storage cap
		for (int i = 1; i < toQueues.size(); i++) {
			QLaneI toQueue = toQueues.get(i);
			if (((QueueWithBuffer) toQueue).usedStorageCapacity < ((QueueWithBuffer) retLane).usedStorageCapacity) {
				retLane = toQueue;
			}
		}
		return retLane;
	}

	/**
	 * Move as many waiting cars to the link as it is possible
	 * 
	 * @param now
	 *            the current time
	 * @return true if at least one vehicle is moved to the buffer of this lane
	 */
	private boolean moveWaitToRoad(final double now) {
		boolean movedWaitToRoad = false;
		while (this.firstLaneQueue.isAcceptingFromWait()) {
			QVehicle veh = this.waitingList.poll();
			if (veh == null) {
				return movedWaitToRoad;
			}
			movedWaitToRoad = true;
			this.network.simEngine
					.getMobsim()
					.getEventsManager()
					.processEvent(
							new VehicleEntersTrafficEvent(now, veh.getDriver().getId(),
									this.getLink().getId(), veh.getId(), veh.getDriver().getMode(), 1.0));

			if (this.transitQLink.addTransitToStopQueue(now, veh, this.getLink().getId())) {
				continue;
			}

			// if (veh.getDriver().chooseNextLinkId() == null) {
			if (veh.getDriver().isWantingToArriveOnCurrentLink()) {
				// If the driver wants to stop on this link, give them a special treatment.
				// addFromWait doesn't work here, because after that, they cannot stop anymore.
				this.firstLaneQueue.addTransitSlightlyUpstreamOfStop(veh);
				continue;
			}

			this.firstLaneQueue.addFromWait(veh, now);
		}
		return movedWaitToRoad;
	}

	@Override
	boolean isNotOfferingVehicle() {
		// otherwise we have to do a bit more work
		for (QLaneI lane : this.toNodeLaneQueues) {
			if (!lane.isNotOfferingVehicle()) {
				return false;
			}
		}
		return true;
	}

	@Override
	boolean isAcceptingFromUpstream() {
		return this.firstLaneQueue.isAcceptingFromUpstream();
	}

	@Override
	public void recalcTimeVariantAttributes(double time) {
		for (QLaneI lane : this.laneQueues.values()) {
			lane.recalcTimeVariantAttributes(time);
		}
	}

	@Override
	QVehicle getVehicle(Id<Vehicle> vehicleId) {
		QVehicle ret = super.getVehicle(vehicleId);
		if (ret != null) {
			return ret;
		}
		for (QVehicle veh : this.waitingList) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		for (QLaneI lane : this.laneQueues.values()) {
			ret = lane.getVehicle(vehicleId);
			if (ret != null) {
				return ret;
			}
		}
		return ret;
	}

	@Override
	public final Collection<MobsimVehicle> getAllNonParkedVehicles() {
		Collection<MobsimVehicle> ret = new ArrayList<MobsimVehicle>(this.waitingList);
		for (QLaneI lane : this.laneQueues.values()) {
			ret.addAll(lane.getAllVehicles());
		}
		return ret;
	}

	/**
	 * @return the total space capacity available on that link (includes the space on lanes if
	 *         available)
	 */
	double getSpaceCap() {
		// (only for tests)
		double total = 0.0;
		for (QLaneI lane : this.laneQueues.values()) {
			total += lane.getStorageCapacity();
		}
		return total;
	}

	@Override
	public Link getLink() {
		return this.link;
	}

	@Override
	public QNode getToNode() {
		return this.toQueueNode;
	}

	/**
	 * This method returns the normalized capacity of the link, i.e. the capacity of vehicles per
	 * second. It is considering the capacity reduction factors set in the config and the
	 * simulation's tick time.
	 * 
	 * @return the flow capacity of this link per second, scaled by the config values and in
	 *         relation to the SimulationTimer's simticktime.
	 */
	double getSimulatedFlowCapacity() {
		return this.firstLaneQueue.getSimulatedFlowCapacity();
	}

	/**
	 * @return the QLanes of this QueueLink
	 */
	public LinkedHashMap<Id<Lane>, QLaneI> getQueueLanes() {
		return this.laneQueues;
	}

	@Override
	public VisData getVisData() {
		return this.visdata;
	}

	QLaneI getOriginalLane() {
		return this.firstLaneQueue;
	}

	/**
	 * Inner class to capsulate visualization methods
	 * 
	 * @author dgrether
	 * 
	 */
	class VisDataImpl implements VisData {
		private VisLaneModelBuilder visModelBuilder = null;
		private VisLinkWLanes visLink = null;

		VisDataImpl() {
			double nodeOffset = QLinkLanesImpl.this.network.simEngine.getMobsim().getScenario()
					.getConfig().qsim().getNodeOffset();
			if (nodeOffset != 0.0) {
				nodeOffset = nodeOffset + 2.0; // +2.0: eventually we need a bit space for the
												// signal
				visModelBuilder = new VisLaneModelBuilder();
				CoordinateTransformation transformation = new IdentityTransformation();
				visLink = visModelBuilder.createVisLinkLanes(transformation, QLinkLanesImpl.this,
						nodeOffset, lanes);
				SnapshotLinkWidthCalculator linkWidthCalculator = QLinkLanesImpl.this.network
						.getLinkWidthCalculatorForVis();
				visModelBuilder.recalculatePositions(visLink, linkWidthCalculator);
			}
		}

		@Override
		public Collection<AgentSnapshotInfo> addAgentSnapshotInfo(
				final Collection<AgentSnapshotInfo> positions) {
			AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder = QLinkLanesImpl.this.network.simEngine
					.getAgentSnapshotInfoBuilder();

			if (visLink != null) {
				for (QLaneI ql : QLinkLanesImpl.this.laneQueues.values()) {
					VisLane otfLane = visLink.getLaneData().get(
							((QueueWithBuffer) ql).getId().toString());
					((QueueWithBuffer.VisDataImpl) ql.getVisData()).setVisInfo(
							otfLane.getStartCoord(), otfLane.getEndCoord(),
							otfLane.getEuklideanDistance());
				}
			}

			for (QLaneI road : QLinkLanesImpl.this.getQueueLanes().values()) {
				road.getVisData().addAgentSnapshotInfo(positions);
			}

			int cnt2 = 10;

			// treat vehicles from transit stops
			cnt2 = snapshotInfoBuilder.positionVehiclesFromTransitStop(positions, link,
					transitQLink.getTransitVehicleStopQueue(), cnt2);
			// treat vehicles from waiting list:
			snapshotInfoBuilder.positionVehiclesFromWaitingList(positions,
					QLinkLanesImpl.this.link, cnt2, QLinkLanesImpl.this.waitingList);
			cnt2 = QLinkLanesImpl.this.waitingList.size();
			snapshotInfoBuilder.positionAgentsInActivities(positions, QLinkLanesImpl.this.link,
					QLinkLanesImpl.this.getAdditionalAgentsOnLink(), cnt2);

			return positions;
		}
	}

}
