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
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.FIFOVehicleQ;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
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

	private NetsimEngineContext context;

	/**
	 * Initializes a QueueLink with one QueueLane.
	 * @param context TODO
	 * @param netsimEngine TODO
	 */
	QLinkLanesImpl(final Link link2, final QNode toNode, List<ModelLane> lanes, NetsimEngineContext context, NetsimInternalInterface netsimEngine) {
		super(link2, toNode, context, netsimEngine);
		this.context = context ;
		this.toQueueNode = toNode;
		this.laneQueues = new LinkedHashMap<>();
		this.toNodeLaneQueues = new ArrayList<>();
		this.lanes = lanes;
		this.nextQueueToLinkCache = new LinkedHashMap<>(); // maps a lane id to a map containing the
															// downstream queues indexed by a
															// downstream link
		this.initLaneQueues();
		this.visdata = this.new VisDataImpl();
		this.setTransitQLink(new TransitQLink(this.firstLaneQueue));
	}

	private void initLaneQueues() {
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

			QueueWithBuffer.Builder builder = new QueueWithBuffer.Builder( context ) ;
			builder.setVehicleQueue(new FIFOVehicleQ());
			builder.setLaneId(laneId);
			builder.setLength(lane.getLength());
			builder.setEffectiveNumberOfLanes(noEffectiveLanes);
			builder.setFlowCapacity_s(lane.getLaneData().getCapacityVehiclesPerHour() / 3600.);
			QLaneI queue = builder.createLane(this);
			// --
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
			queue.changeSpeedMetersPerSecond( this.getLink().getFreespeed() );
			
		}
		// reverse the order in the linked map, i.e. upstream to downstream
		while (!stack.isEmpty()) {
			QLaneI queue = stack.pop();
			this.laneQueues.put(((QueueWithBuffer) queue).getId(), queue);
		}
	}

	@Override
	List<QLaneI> getOfferingQLanes() {
		return this.toNodeLaneQueues;
	}

	@Override
	void clearVehicles() {
		super.clearVehicles();
		for (QLaneI lane : this.laneQueues.values()) {
			lane.clearVehicles();
		}
	}

	@Override
	boolean doSimStep() {
		double now = context.getSimTimer().getTimeOfDay() ;
		boolean lanesActive = false;
		boolean movedWaitToRoad = false;
		if ( context.qsimConfig.isInsertingWaitingVehiclesBeforeDrivingVehicles() ) {
			this.moveWaitToRoad(now);
			this.getTransitQLink().handleTransitVehiclesInStopQueue(now);
			lanesActive = this.moveLanes();
		} else {
			this.getTransitQLink().handleTransitVehiclesInStopQueue(now);
			lanesActive = this.moveLanes();
			movedWaitToRoad = this.moveWaitToRoad(now);
		}
		this.setActive(lanesActive || movedWaitToRoad || (!this.getWaitingList().isEmpty())
				|| !this.getTransitQLink().getTransitVehicleStopQueue().isEmpty());
		return this.isActive();
	}

	private boolean moveLanes() {
		boolean activeLane = false;
		for (QLaneI lane : this.laneQueues.values()) {
			// (go through all lanes)
			
//			((QueueWithBuffer) lane).updateRemainingFlowCapacity();
			
			/* part A */
			if (!this.toNodeLaneQueues.contains(lane)) {
				// (so it HAS a link-internal next lane)
				
				// move vehicles from the lane buffer to the next lane
				this.moveBufferToNextLane(lane);
			} else {
				// move vehicles from the lane buffer to the link buffer, i.e. prepare moving them
				// to the next link
//				((QueueWithBuffer) queue).moveQueueToBuffer();
				// yy just commented out the above line.  Can't say why it might needed; doSimStep also calls moveQueueToBuffer.
				// Tests run ok, but it may have capacity ramifications outside tests.  kai, mar'16
			}
			/* end of part A */
		}
		for (QLaneI lane : this.laneQueues.values()) {
			// (go through all lanes)
		
			/* part B */
			// move vehicles to the lane buffer if they have reached their earliest lane exit time
			lane.doSimStep();
			/* end of part B */
			
			activeLane = activeLane || lane.isActive();
			
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

	private void moveBufferToNextLane(QLaneI qlane) {
		QVehicle veh;
		while (!qlane.isNotOfferingVehicle()) {
			veh = qlane.getFirstVehicle();
			Id<Link> toLinkId = veh.getDriver().chooseNextLinkId();
			QLaneI nextQueue = this.chooseNextLane(qlane, toLinkId);
			if (nextQueue != null) {
				if (nextQueue.isAcceptingFromUpstream()) {
					((QueueWithBuffer) qlane).popFirstVehicle();
//					context .getEventsManager() .processEvent(
//									new LaneLeaveEvent(now, veh.getId(), this.getLink()
//											.getId(), ((QueueWithBuffer) qlane).getId()));
					nextQueue.addFromUpstream(veh);
//					context .getEventsManager() .processEvent(
//									new LaneEnterEvent(now, veh.getId(), this.getLink()
//											.getId(), ((QueueWithBuffer) nextQueue).getId()));
				} else {
					break;
				}
			} else {
				StringBuilder b = new StringBuilder();
				b.append("Person Id: ").append(veh.getDriver().getId());
				b.append(" is on Lane Id ").append(((QueueWithBuffer) qlane).getId());
				b.append(" on Link Id ").append(this.getLink().getId());
				b.append(" and wants to drive to Link Id ").append(toLinkId);
				b.append(" but there is no Lane leading to that Link!");
				log.error(b.toString());
				throw new IllegalStateException(b.toString());
			}
		}
	}

	private QLaneI chooseNextLane(QLaneI queue, Id<Link> toLinkId) {
		List<QLaneI> toQueues = this.nextQueueToLinkCache.get(queue.getId()).get(toLinkId);
		QLaneI retLane = toQueues.get(0);
		if (toQueues.size() == 1) {
			return retLane;
		}
		// else chose lane by storage cap
		for (int i = 1; i < toQueues.size(); i++) {
			QLaneI toQueue = toQueues.get(i);
			if (((QueueWithBuffer) toQueue).getLoadIndicator() < ((QueueWithBuffer) retLane).getLoadIndicator()) {
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
			QVehicle veh = this.getWaitingList().poll();
			if (veh == null) {
				return movedWaitToRoad;
			}
			movedWaitToRoad = true;
			context .getEventsManager() .processEvent(
							new VehicleEntersTrafficEvent(now, veh.getDriver().getId(),
									this.getLink().getId(), veh.getId(), veh.getDriver().getMode(), 1.0));

			if (this.getTransitQLink().addTransitToStopQueue(now, veh, this.getLink().getId())) {
				continue;
			}

			// if (veh.getDriver().chooseNextLinkId() == null) {
			if (veh.getDriver().isWantingToArriveOnCurrentLink()) {
				// If the driver wants to stop on this link, give them a special treatment.
				// addFromWait doesn't work here, because after that, they cannot stop anymore.
				this.firstLaneQueue.addTransitSlightlyUpstreamOfStop(veh);
				continue;
			}

			this.firstLaneQueue.addFromWait(veh);
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
	public void recalcTimeVariantAttributes() {
		double now = context.getSimTimer().getTimeOfDay() ;

		for (QLaneI lane : this.laneQueues.values()) {
			lane.changeEffectiveNumberOfLanes( getLink().getNumberOfLanes( now ) ) ;
			lane.changeSpeedMetersPerSecond( getLink().getFreespeed(now) ) ;
			lane.changeUnscaledFlowCapacityPerSecond( ((LinkImpl)getLink()).getFlowCapacityPerSec(now) );
		}
	}

	@Override
	QVehicle getVehicle(Id<Vehicle> vehicleId) {
		QVehicle ret = super.getVehicle(vehicleId);
		if (ret != null) {
			return ret;
		}
		for (QVehicle veh : this.getWaitingList()) {
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
		Collection<MobsimVehicle> ret = new ArrayList<MobsimVehicle>(this.getWaitingList());
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
		return this.firstLaneQueue.getSimulatedFlowCapacityPerTimeStep();
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
			double nodeOffset = context.qsimConfig.getNodeOffset();
			if (nodeOffset != 0.0) {
				nodeOffset = nodeOffset + 2.0; // +2.0: eventually we need a bit space for the
												// signal
				visModelBuilder = new VisLaneModelBuilder();
				CoordinateTransformation transformation = new IdentityTransformation();
				visLink = visModelBuilder.createVisLinkLanes(transformation, QLinkLanesImpl.this, nodeOffset, lanes);
				visModelBuilder.recalculatePositions(visLink, context.linkWidthCalculator);
			}
		}

		@Override
		public Collection<AgentSnapshotInfo> addAgentSnapshotInfo(
				final Collection<AgentSnapshotInfo> positions) {
			
			double now = context.getSimTimer().getTimeOfDay() ;


			if (visLink != null) {
				for (QLaneI ql : QLinkLanesImpl.this.laneQueues.values()) {
					VisLane otfLane = visLink.getLaneData().get(
							((QueueWithBuffer) ql).getId().toString());
					((QueueWithBuffer.VisDataImpl) ql.getVisData()).setVisInfo(
							otfLane.getStartCoord(), otfLane.getEndCoord());
				}
			}

			for (QLaneI road : QLinkLanesImpl.this.getQueueLanes().values()) {
				road.getVisData().addAgentSnapshotInfo(positions, now);
			}

			int cnt2 = 10;

			// treat vehicles from transit stops
			cnt2 = context.snapshotInfoBuilder.positionVehiclesFromTransitStop(positions, getLink(),
					getTransitQLink().getTransitVehicleStopQueue(), cnt2);
			// treat vehicles from waiting list:
			context.snapshotInfoBuilder.positionVehiclesFromWaitingList(positions,
					QLinkLanesImpl.this.getLink(), cnt2, QLinkLanesImpl.this.getWaitingList());
			cnt2 = QLinkLanesImpl.this.getWaitingList().size();
			context.snapshotInfoBuilder.positionAgentsInActivities(positions, QLinkLanesImpl.this.getLink(),
					QLinkLanesImpl.this.getAdditionalAgentsOnLink(), cnt2);

			return positions;
		}
	}
	
	@Override
	QLaneI getAcceptingQLane() {
		return this.firstLaneQueue ;
	}

}
