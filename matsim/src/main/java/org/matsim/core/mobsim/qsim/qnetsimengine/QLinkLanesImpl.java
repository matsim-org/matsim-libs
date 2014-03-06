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
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.misc.Time;
import org.matsim.lanes.LaneImpl;
import org.matsim.lanes.vis.VisLaneModelBuilder;
import org.matsim.lanes.vis.VisLinkWLanes;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.vis.snapshotwriters.VisData;

/**
 * Please read the docu of QBufferItem, QLane, QLinkInternalI (arguably to be renamed
 * into something like AbstractQLink) and QLinkImpl jointly. kai, nov'11
 * 
 * @author dstrippgen
 * @author dgrether
 * @author mrieser
 *
 * A QLinkLanes can consist of one or more QLanes, which may have the following layout
 * (Dashes stand for the borders of the QLink, equal signs (===) depict one lane,
 * plus signs (+) symbolize a decision point where one lane splits into several lanes) :
 * <pre>
 * ----------------
 * ================
 * ----------------
 *</pre>
 *
 *<pre>
 * ----------------
 *          =======
 * ========+
 *          =======
 * ----------------
 *</pre>
 *
 *<pre>
 * ----------------
 *         							========
 * =======+========
 *         							========
 * ----------------
 * </pre>
 *
 *
 * The following layouts are not allowed:
 * <pre>
 * ----------------
 * ================
 * ================
 * ----------------
 *</pre>
 *
 *<pre>
 * ----------------
 * =======
 *        +========
 * =======
 * ----------------
 * </pre>
 *
 * Queue Model Link implementation with the following properties:
 * <ul>
 *   <li>The queue behavior itself is simulated by one or more instances of QueueLane</li>
 *   <li>Each QueueLink has at least one QueueLane. All QueueVehicles added to the QueueLink
 *   are placed on this instance.
 *    </li>
 *   <li>All QueueLane instances which are connected to the ToQueueNode are
 *   held in the attribute toNodeQueueLanes</li>
 *   <li>QueueLink is active as long as at least one
 * of its QueueLanes is active.</li>
 * </ul>
 */
public final class QLinkLanesImpl extends AbstractQLink {

	private static final Logger log = Logger.getLogger(QLinkLanesImpl.class);
	
	final private static QLaneFromLinkEndComparator fromLinkEndComparator = new QLaneFromLinkEndComparator();

	/**
	 * Reference to the QueueNode which is at the end of each QueueLink instance
	 */
	private final QNode toQueueNode;
	/**
	 * The QueueLane instance which always exists.
	 */
	private QueueWithBuffer firstLaneQueue;
	/**
	 * A List holding all QueueLane instances of the QueueLink
	 */
	private LinkedHashMap<Id, QueueWithBuffer> laneQueues;
	
	/**
	 * If more than one QueueLane exists this list holds all QueueLanes connected to
	 * the (To)QueueNode of the QueueLink
	 */
	private List<QueueWithBuffer> toNodeLaneQueues = null;

	private boolean active = false;

	private VisData visdata = null;

	private Map<Id, LaneImpl> lanesByIdMap;
	
	private List<LaneImpl> lanes;
	
	private Map<Id, Set<Id>> laneIdToLinksMap;

	/**
	 * Initializes a QueueLink with one QueueLane.
	 * @param link2
	 * @param queueNetwork
	 * @param toNode
	 * @see NetsimLink#createLanes(List)
	 */
	QLinkLanesImpl(final Link link2, QNetwork network, final QNode toNode, List<LaneImpl> lanes) {
		super(link2, network) ;
		this.toQueueNode = toNode;
		this.laneQueues = new LinkedHashMap<Id, QueueWithBuffer>();
		this.toNodeLaneQueues = new ArrayList<QueueWithBuffer>();
		this.lanesByIdMap = new LinkedHashMap<Id, LaneImpl>(); //HashMap might be sufficient
		this.laneIdToLinksMap = new HashMap<Id, Set<Id>>();
		this.lanes = lanes;
		this.initLaneQueues(lanes);
		this.visdata = this.new VisDataImpl();
	}

	private void initLaneQueues(List<LaneImpl> lanes){
		Stack<QueueWithBuffer> stack = new Stack<QueueWithBuffer>();
		for (LaneImpl lane : lanes) {
			Id laneId = lane.getLaneData().getId();
			this.lanesByIdMap.put(laneId, lane);
			double noEffectiveLanes = lane.getLaneData().getNumberOfRepresentedLanes();
			QueueWithBuffer queue = new QueueWithBuffer(this, new FIFOVehicleQ(), laneId, 
					lane.getLength(), noEffectiveLanes, (lane.getLaneData().getCapacityVehiclesPerHour()/3600.0));
			queue.generatingEvents = true;
			firstLaneQueue = queue;
			stack.push(queue);
			Set<Id> toLinkIds = new HashSet<Id>();
			
			if (lane.getToLanes() == null || lane.getToLanes().isEmpty()){
				this.toNodeLaneQueues.add(queue);
				toLinkIds.addAll(lane.getLaneData().getToLinkIds());
				this.laneIdToLinksMap.put(laneId, toLinkIds);
			}
			else {
				for (LaneImpl toLane : lane.getToLanes()) {
					Set<Id> toLinks = laneIdToLinksMap.get(toLane.getLaneData().getId());
					if (! laneIdToLinksMap.containsKey(laneId)){
						laneIdToLinksMap.put(laneId, new HashSet<Id>());
					}
					laneIdToLinksMap.get(laneId).addAll(toLinks);
				}
			}
			queue.recalcTimeVariantAttributes(Time.UNDEFINED_TIME);
		}
		//reverse the order in the linked map, i.e. upstream to downstream
		while (! stack.isEmpty()){
			QueueWithBuffer queue = stack.pop();
			this.laneQueues.put(queue.getId(), queue);
		}
	}

	List<QueueWithBuffer> getToNodeQueueLanes() {
		return this.toNodeLaneQueues;
	}

	@Override
	void activateLink() {
		if (!this.active) {
			netElementActivator.activateLink(this);
			this.active = true;
		}
	}

	/**
	 * Adds a vehicle to the link, called by
	 * {@link QNode#moveVehicleOverNode(QVehicle, QLane, double)}.
	 *
	 * @param veh
	 *          the vehicle
	 */
	@Override
	void addFromUpstream(final QVehicle veh) {
		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		activateLink();
		this.firstLaneQueue.addFromUpstream(veh );
		veh.setCurrentLink(this.getLink());
		this.network.simEngine.getMobsim().getEventsManager().processEvent(
				new LinkEnterEvent(now, veh.getDriver().getId(),
						this.getLink().getId(), veh.getId()));
	}

	@Override
	void clearVehicles() {
		super.clearVehicles();
		for (QueueWithBuffer lane : this.laneQueues.values()){
			lane.clearVehicles();
		}
	}

	@Override
	boolean doSimStep(double now) {
		boolean activeWaitBuffer = false;
		boolean lanesActive = false;
		if (this.insertingWaitingVehiclesBeforeDrivingVehicles){
			activeWaitBuffer = this.moveWaitToBuffer(now);
			lanesActive = this.moveLanes(now);
		}
		else {
			lanesActive = this.moveLanes(now);
			activeWaitBuffer = this.moveWaitToBuffer(now);
		}
		this.active = true; //= lanesActive || activeWaitBuffer || (!this.waitingList.isEmpty());
		return true;
	}

	private boolean moveLanes(double now){
		for (QueueWithBuffer queue : this.laneQueues.values()){ 
			queue.updateRemainingFlowCapacity();
			queue.doSimStep(now);
			if (! this.toNodeLaneQueues.contains(queue)) {
				this.moveBufferToNextLane(now, queue);
			}
			else {
				queue.moveQueueToBuffer(now);
			}
		}
		return true;
	}

	private void moveBufferToNextLane(final double now, QueueWithBuffer queue) {
		QVehicle veh;
		//TODO debug here infinite loop
		while (! queue.isNotOfferingVehicle()) {
			veh = queue.peekFirstVehicle();
			Id toLinkId = veh.getDriver().chooseNextLinkId();
			QueueWithBuffer nextQueue = this.chooseNextLane(queue, toLinkId);
			if (nextQueue != null) {
				if (nextQueue.isAcceptingFromUpstream()) {
					queue.popFirstVehicle();
					nextQueue.addFromUpstream(veh);
				}
				else {
					break;
				}
			}
			else 	{
				StringBuilder b = new StringBuilder();
				b.append("Person Id: ");
				b.append(veh.getDriver().getId());
				b.append(" is on Lane Id ");
				b.append(queue.getId());
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

	
	private QueueWithBuffer chooseNextLane(QueueWithBuffer queue, Id toLinkId){
		LaneImpl currentLane = this.lanesByIdMap.get(queue.getId());
		List<LaneImpl> toLanes = new ArrayList<LaneImpl>();
		toLanes.addAll(currentLane.getToLanes());
		ListIterator<LaneImpl> li = toLanes.listIterator();
//		System.err.println(queue.getId());
//		System.err.println("  ToLinkId: " + toLinkId);
		while (li.hasNext()){
			LaneImpl toLane = li.next();
//			System.err.println("  " + laneIdToLinksMap.get(toLane.getLaneData().getId()));
			if (! laneIdToLinksMap.get(toLane.getLaneData().getId()).contains(toLinkId)){
				li.remove();
			}
		}
		QueueWithBuffer retLane = laneQueues.get(toLanes.get(0).getLaneData().getId());
		if (toLanes.size() == 1){
			return retLane;
		}
		//else chose lane by storage cap
		for (int i = 1; i < toLanes.size(); i++) {
			LaneImpl toLane = toLanes.get(i);
			QueueWithBuffer toQueue = laneQueues.get(toLane.getLaneData().getId());
			if (toQueue.usedStorageCapacity < retLane.usedStorageCapacity){
				retLane = toQueue;
			}
		}
		return retLane;
	}

	

	/**
	 * Move as many waiting cars to the link as it is possible
	 *
	 * @param now
	 *          the current time
	 *  @return true if at least one vehicle is moved to the buffer of this lane
	 */
	private boolean moveWaitToBuffer(final double now) {
		boolean movedAtLeastOne = false;
		while (this.firstLaneQueue.isAcceptingFromWait()) {
			QVehicle veh = this.waitingList.poll();
			if (veh == null) {
				return movedAtLeastOne;
			}
			movedAtLeastOne = true;
			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new Wait2LinkEvent(now, veh.getDriver().getId(), this.getLink().getId(), veh.getId()));
//			boolean handled = this.firstLane.addTransitToStopQueue(now, veh);
			boolean handled = false ;
			if (!handled) {
				this.firstLaneQueue.addFromWait(veh, now);
			}
		}
		return movedAtLeastOne;
	}
	
	@Override
	boolean isNotOfferingVehicle() {
		//otherwise we have to do a bit more work
		for (QueueWithBuffer lane : this.toNodeLaneQueues){
			if (!lane.isNotOfferingVehicle()){
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
		for (QueueWithBuffer lane : this.laneQueues.values()){
			lane.recalcTimeVariantAttributes(time);
		}
	}

	@Override
	QVehicle getVehicle(Id vehicleId) {
		QVehicle ret = super.getVehicle(vehicleId);
		if (ret != null) {
			return ret;
		}
		for (QVehicle veh : this.waitingList) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		for (QueueWithBuffer lane : this.laneQueues.values()){
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
		for  (QueueWithBuffer lane : this.laneQueues.values()){
			ret.addAll(lane.getAllVehicles());
		}
		return ret;
	}

	/**
	 * @return the total space capacity available on that link (includes the space on lanes if available)
	 */
	double getSpaceCap() {
		// (only for tests)
		double total = 0.0;
		for (QueueWithBuffer lane : this.laneQueues.values()) {
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
	 * This method returns the normalized capacity of the link, i.e. the capacity
	 * of vehicles per second. It is considering the capacity reduction factors
	 * set in the config and the simulation's tick time.
	 *
	 * @return the flow capacity of this link per second, scaled by the config
	 *         values and in relation to the SimulationTimer's simticktime.
	 */
	double getSimulatedFlowCapacity() {
		return this.firstLaneQueue.getSimulatedFlowCapacity();
	}

	/**
	 * @return the QLanes of this QueueLink
	 */
	public LinkedHashMap<Id, QueueWithBuffer> getQueueLanes(){
		return this.laneQueues;
	}

	@Override
	public VisData getVisData() {
		return this.visdata;
	}

	QueueWithBuffer getOriginalLane(){
		return this.firstLaneQueue;
	}

	/**
	 * Inner class to capsulate visualization methods
	 * @author dgrether
	 *
	 */
	class VisDataImpl implements VisData {
		private VisLaneModelBuilder laneModelBuilder = new VisLaneModelBuilder();
		private VisLinkWLanes otfLink;

		VisDataImpl(){
			double nodeOffset = QLinkLanesImpl.this.network.simEngine.getMobsim().getScenario().getConfig().qsim().getNodeOffset();
			if (nodeOffset != 0){
				 nodeOffset = nodeOffset +2.0; // +2.0: eventually we need a bit space for the signal
			}
			CoordinateTransformation transformation = new IdentityTransformation();
			otfLink = laneModelBuilder.createVisLinkLanes(transformation, QLinkLanesImpl.this, nodeOffset, lanes);
			SnapshotLinkWidthCalculator linkWidthCalculator = QLinkLanesImpl.this.network.getLinkWidthCalculator();
			laneModelBuilder.recalculatePositions(otfLink, linkWidthCalculator);
//			for (QueueWithBuffer  ql : QLinkLanesImpl.this.laneQueues.values()){
//				VisLane otfLane = otfLink.getLaneData().get(ql.getId().toString());
//				ql.ssetOTFLane(otfLane);
//			}
		}
		
		@Override
		public Collection<AgentSnapshotInfo> getAgentSnapshotInfo( final Collection<AgentSnapshotInfo> positions) {

			AgentSnapshotInfoBuilder agentSnapshotInfoBuilder = QLinkLanesImpl.this.network.simEngine.getAgentSnapshotInfoBuilder();

			for (QueueWithBuffer lane : QLinkLanesImpl.this.getQueueLanes().values()) {
				lane.getVisData().getAgentSnapshotInfo(positions);
			}
			int cnt2 = 10;
			// treat vehicles from waiting list:
			agentSnapshotInfoBuilder.positionVehiclesFromWaitingList(positions, QLinkLanesImpl.this.link, cnt2,
					QLinkLanesImpl.this.waitingList);
			cnt2 = QLinkLanesImpl.this.waitingList.size();
			agentSnapshotInfoBuilder.positionAgentsInActivities(positions, QLinkLanesImpl.this.link,
					QLinkLanesImpl.this.getAdditionalAgentsOnLink(), cnt2);

			return positions;
		}
	}

	// The following contains a number of methods that are defined in the upstream interfaces but not needed here. 
	// In principle, one would need two separate interfaces, one for the "QLane" and one for the "QLink".  They would be
	// combined into the QLinkImpl, whereas for QLane and QLinkLanesImpl they would be separate.  Can't do this with
	// abstract classes (no multiple inheritance), but we need to use them because we do not want _public_ interfaces here.


	@Override
	double getLastMovementTimeOfFirstVehicle() {
		throw new UnsupportedOperationException("Method should not be called on this instance");
	}


	@Override
	QVehicle getFirstVehicle() {
		throw new UnsupportedOperationException("Method should not be called on this instance");
	}


	@Override
	boolean hasGreenForToLink(Id toLinkId) {
		throw new UnsupportedOperationException("Method should not be called on this instance");
	}


	@Override
	QVehicle popFirstVehicle() {
		throw new UnsupportedOperationException("Method should not be called on this instance");
	}

}
