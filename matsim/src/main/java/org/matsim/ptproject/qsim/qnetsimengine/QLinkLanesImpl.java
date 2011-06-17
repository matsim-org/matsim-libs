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

package org.matsim.ptproject.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.events.AgentWait2LinkEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LaneMeterFromLinkEndComparator;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.NetsimEngine;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.VisData;

/**
 * @author dstrippgen
 * @author dgrether
 * @author mrieser
 *
 * A QueueLink can consist of one or more QueueLanes, which may have the following layout
 * (Dashes stand for the borders of the QueueLink, equal signs (===) depict one lane,
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
 *         ========
 * =======+========
 *         ========
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
public class QLinkLanesImpl extends QLinkInternalI {

	final private static Logger log = Logger.getLogger(QLinkLanesImpl.class);

	final private static QLane.FromLinkEndComparator fromLinkEndComparator = new QLane.FromLinkEndComparator();

	/**
	 * The Link instance containing the data
	 */
	private final Link link;
	/**
	 * Reference to the QueueNode which is at the end of each QueueLink instance
	 */
	private final QNode toQueueNode;
	/**
	 * The QueueLane instance which always exists.
	 */
	private QLane originalLane;
	/**
	 * A List holding all QueueLane instances of the QueueLink
	 */
	private List<QLane> queueLanes;

	/**
	 * If more than one QueueLane exists this list holds all QueueLanes connected to
	 * the (To)QueueNode of the QueueLink
	 */
	private List<QLane> toNodeQueueLanes = null;

	private boolean active = false;

	private final Map<Id, QVehicle> parkedVehicles = new LinkedHashMap<Id, QVehicle>(10);

	private final Map<Id, MobsimAgent> agentsInActivities = new LinkedHashMap<Id, MobsimAgent>();

	private VisData visdata = this.new VisDataImpl();

	private QSimEngineImpl qsimEngine = null;

	/**
	 * All vehicles from parkingList move to the waitingList as soon as their time
	 * has come. They are then filled into the vehQueue, depending on free space
	 * in the vehQueue
	 */
	/*package*/ final Queue<QVehicle> waitingList = new LinkedList<QVehicle>();

	/**
	 * Initializes a QueueLink with one QueueLane.
	 * @param link2
	 * @param queueNetwork
	 * @param toNode
	 * @see NetsimLink#createLanes(List)
	 */
	 QLinkLanesImpl(final Link link2, NetsimEngine engine,
			final QNode toNode, Map<Id, Lane> laneMap) {
		this.link = link2;
		this.toQueueNode = toNode;
		this.qsimEngine = (QSimEngineImpl) engine;
		// yyyy this cast is not so bad because this is not meant to be pluggable (QLinkImpl together with some other engine).
		// But it would still be better to do it correctly.  kai, aug'10

		this.queueLanes = new ArrayList<QLane>();
		this.toNodeQueueLanes = new ArrayList<QLane>();
		this.createLanes(laneMap);
	}


	/**
	 * Initialize the QueueLink with more than one QueueLane
	 * @param map
	 */
	private void createLanes(Map<Id, Lane> map) {
		List<Lane> sortedLanes =  new ArrayList<Lane>(map.values());
		Collections.sort(sortedLanes, new LaneMeterFromLinkEndComparator());
		Collections.reverse(sortedLanes);

		List<QLane> laneList = new LinkedList<QLane>();
		Lane firstLane = sortedLanes.remove(0);
		if (firstLane.getStartsAtMeterFromLinkEnd() != this.link.getLength()) {
			throw new IllegalStateException("First Lane Id " + firstLane.getId() + " on Link Id " + this.link.getId() +
			"isn't starting at the beginning of the link!");
		}
		this.originalLane = new QLane(this, firstLane, true);
		laneList.add(this.originalLane);
		Stack<QLane> laneStack = new Stack<QLane>();

		while (!laneList.isEmpty()){
			QLane lastQLane = laneList.remove(0);
			laneStack.push(lastQLane);
			lastQLane.setFireLaneEvents(true);
			this.queueLanes.add(lastQLane);

			//if existing create the subsequent lanes
			List<Id> toLaneIds = lastQLane.getLaneData().getToLaneIds();
			double nextMetersFromLinkEnd = 0.0;
			double laneLength = 0.0;
			if (toLaneIds != null 	&& (!toLaneIds.isEmpty())) {
				for (Id toLaneId : toLaneIds){
					Lane currentLane = map.get(toLaneId);
					nextMetersFromLinkEnd = currentLane.getStartsAtMeterFromLinkEnd();
					QLane currentQLane = new QLane(this, currentLane, false);
					laneList.add(currentQLane);
					lastQLane.addToLane(currentQLane);
				}
				laneLength = (link.getLength() - nextMetersFromLinkEnd) - (link.getLength() -  lastQLane.getLaneData().getStartsAtMeterFromLinkEnd());
				laneLength = lastQLane.getLaneData().getStartsAtMeterFromLinkEnd() - nextMetersFromLinkEnd;
				lastQLane.setEndsAtMetersFromLinkEnd(nextMetersFromLinkEnd);
			}
			//there are no subsequent lanes
			else {
				laneLength = lastQLane.getLaneData().getStartsAtMeterFromLinkEnd();
				lastQLane.setEndsAtMetersFromLinkEnd(0.0);
				this.toNodeQueueLanes.add(lastQLane);
			}
			lastQLane.setLaneLength(laneLength);
		}

		//fill toLinks
		while (!laneStack.isEmpty()){
			QLane qLane = laneStack.pop();
			qLane.calculateCapacities();
			if (qLane.getToLanes() == null || (qLane.getToLanes().isEmpty())) {
				for (Id toLinkId : qLane.getLaneData().getToLinkIds()){
					qLane.addDestinationLink(toLinkId);
				}
			}
			else {
				for (QLane subsequentLane : qLane.getToLanes()){
					for (Id toLinkId : subsequentLane.getDestinationLinkIds()){
						qLane.addDestinationLink(toLinkId);
					}
				}
			}
			qLane.finishInitialization();
		}

		Collections.sort(this.queueLanes, QLinkLanesImpl.fromLinkEndComparator);
	}

	public List<QLane> getToNodeQueueLanes() {
		return this.toNodeQueueLanes;
	}

	@Override
	void activateLink() {
		if (!this.active) {
			this.qsimEngine.activateLink(this);
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
	void addFromIntersection(final QVehicle veh) {
		double now = this.getQSimEngine().getMobsim().getSimTimer().getTimeOfDay();
		activateLink();
		this.originalLane.addToVehicleQueue(veh, now);
		veh.setCurrentLink(this.getLink());
		this.getQSimEngine().getMobsim().getEventsManager().processEvent(
				new LinkEnterEventImpl(now, veh.getDriver().getId(),
						this.getLink().getId()));
	}

	@Override
	void clearVehicles() {
		double now = this.getQSimEngine().getMobsim().getSimTimer().getTimeOfDay();
		this.parkedVehicles.clear();
		for (QVehicle veh : this.waitingList) {
			this.getQSimEngine().getMobsim().getEventsManager().processEvent(
					new AgentStuckEventImpl(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
		}
		this.getQSimEngine().getMobsim().getAgentCounter().decLiving(this.waitingList.size());
		this.getQSimEngine().getMobsim().getAgentCounter().incLost(this.waitingList.size());
		this.waitingList.clear();

		for (QLane lane : this.queueLanes){
			lane.clearVehicles(now);
		}
	}

	@Override
	public void addParkedVehicle(QVehicle vehicle) {
		this.parkedVehicles.put(vehicle.getId(), vehicle);
		vehicle.setCurrentLink(this.link);
	}

	/*package*/ QVehicle getParkedVehicle(Id vehicleId) {
		return this.parkedVehicles.get(vehicleId);
	}

	@Override
	public QVehicle removeParkedVehicle(Id vehicleId) {
		return this.parkedVehicles.remove(vehicleId);
	}

	@Override
	public void addDepartingVehicle(QVehicle vehicle) {
		this.waitingList.add(vehicle);
		this.activateLink();
	}

	@Override
	protected boolean moveLink(double now) {
		boolean activeLane = false;
		boolean otherLaneActive = false;
		boolean activeWaitBuffer = false;
		for (QLane lane : this.queueLanes){
			if (lane.moveLane(now)){
				otherLaneActive = true;
			}
			if (lane.isActive()){
				activeLane = true;
			}
		}
		activeWaitBuffer = this.moveWaitToBuffer(now);
		this.active = activeLane || otherLaneActive || activeWaitBuffer || (!this.waitingList.isEmpty());
		return this.active;
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
		while (this.originalLane.hasBufferSpace()) {
			QVehicle veh = this.waitingList.poll();
			if (veh == null) {
				return movedAtLeastOne;
			}
			movedAtLeastOne = true;
			this.getQSimEngine().getMobsim().getEventsManager().processEvent(
					new AgentWait2LinkEventImpl(now, veh.getDriver().getId(), this.getLink().getId()));
			boolean handled = this.originalLane.transitQueueLaneFeature.handleMoveWaitToBuffer(now, veh);
			if (!handled) {
				this.originalLane.addToBuffer(veh, now);
			}
		}
		return movedAtLeastOne;
	}


	@Override
	boolean bufferIsEmpty() {
		//if there is only one lane...
		if (this.toNodeQueueLanes == null){
			return this.originalLane.bufferIsEmpty();
		}
		//otherwise we have to do a bit more work
		for (QLane lane : this.toNodeQueueLanes){
			if (!lane.bufferIsEmpty()){
				return false;
			}
		}
		return true;
	}

	@Override
	boolean hasSpace() {
		return this.originalLane.hasSpace();
	}

	@Override
	public void recalcTimeVariantAttributes(double time) {
		for (QLane ql : this.queueLanes){
			ql.recalcTimeVariantAttributes(time);
		}
	}

	@Override
	public QVehicle getVehicle(Id vehicleId) {
		QVehicle ret = getParkedVehicle(vehicleId);
		if (ret != null) {
			return ret;
		}
		for (QVehicle veh : this.waitingList) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		for (QLane lane : this.queueLanes){
			ret = lane.getVehicle(vehicleId);
			if (ret != null) {
				return ret;
			}
		}
		return ret;
	}

	@Override
	public final Collection<QVehicle> getAllVehicles() {
		Collection<QVehicle> ret = getAllNonParkedVehicles() ;
		ret.addAll(this.parkedVehicles.values());
		return ret ;
	}

	@Override
	public final Collection<QVehicle> getAllNonParkedVehicles() {
		Collection<QVehicle> ret = new ArrayList<QVehicle>(this.waitingList);
		for  (QLane lane : this.queueLanes){
			ret.addAll(lane.getAllVehicles());
		}
		return ret;
	}

	/**
	 * @return the total space capacity available on that link (includes the space on lanes if available)
	 */
	@Override
	public double getSpaceCap() {
		double total = 0.0;
		for (QLane ql : this.getQueueLanes()) {
			total += ql.getStorageCapacity();
		}
		return total;
	}

	/**
	 * One should think about the need for this method
	 * because it is only called by one testcase
	 * @return
	 */
	int vehOnLinkCount() {
		int count = 0;
		for (QLane ql : this.queueLanes){
			count += ql.vehOnLinkCount();
		}
		return count;
	}

	@Override
	public Link getLink() {
		return this.link;
	}

	@Override
	public QNode getToQueueNode() {
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
	public double getSimulatedFlowCapacity() {
		return this.originalLane.getSimulatedFlowCapacity();
	}

	@Override
	QSimEngineImpl getQSimEngine(){
		return this.qsimEngine;
	}

	@Override
	public QSim getMobsim() {
		return this.qsimEngine.getMobsim();
	}

	@Override
	void setQSimEngine(NetsimEngine qsimEngine) {
		// yyyy does it make sense to have this setter?  Seems that this should be immutable after construction. kai, aug'10
		this.qsimEngine = (QSimEngineImpl) qsimEngine;
		// yyyy this cast is not so bad because this is not meant to be pluggable (QLinkImpl together with some other engine).
		// But it would still be better to do it correctly.  kai, aug'10

	}

	/**
	 * @return the QLanes of this QueueLink
	 */
	public List<QLane> getQueueLanes(){
		return this.queueLanes;
	}

	@Override
	public VisData getVisData() {
		return this.visdata;
	}

	public QLane getOriginalLane(){
		return this.originalLane;
	}

	@Override
	public LinkedList<QVehicle> getVehQueue() {
		LinkedList<QVehicle> ll = this.originalLane.getVehQueue();
		for (QLane l : this.getToNodeQueueLanes()){
			ll.addAll(l.getVehQueue());
		}
		return ll;
	}



	/**
	 * Inner class to capsulate visualization methods
	 * @author dgrether
	 *
	 */
	class VisDataImpl implements VisData {

		@Override
		public Collection<AgentSnapshotInfo> getVehiclePositions( final Collection<AgentSnapshotInfo> positions) {

			AgentSnapshotInfoBuilder agentSnapshotInfoBuilder = QLinkLanesImpl.this.getQSimEngine().getAgentSnapshotInfoBuilder();

			for (QLane lane : QLinkLanesImpl.this.getQueueLanes()) {
				lane.visdata.getVehiclePositions(positions);
			}
			int cnt2 = 0;
			// treat vehicles from waiting list:
			agentSnapshotInfoBuilder.positionVehiclesFromWaitingList(positions, QLinkLanesImpl.this.link, cnt2,
					QLinkLanesImpl.this.waitingList, null);
			cnt2 = QLinkLanesImpl.this.waitingList.size();
			agentSnapshotInfoBuilder.positionAgentsInActivities(positions, QLinkLanesImpl.this.link,
					QLinkLanesImpl.this.agentsInActivities.values(), cnt2);

			return positions;
		}
	}

	@Override
	public void registerAgentOnLink(MobsimAgent planAgent) {
		agentsInActivities.put(planAgent.getId(), planAgent);
	}

	@Override
	public void unregisterAgentOnLink(MobsimAgent planAgent) {
		agentsInActivities.remove(planAgent.getId());
	}


	@Override
	double getBufferLastMovedTime() {
		throw new UnsupportedOperationException("Method should not be called on this instance");
	}


	@Override
	QVehicle getFirstFromBuffer() {
		throw new UnsupportedOperationException("Method should not be called on this instance");
	}


	@Override
	boolean hasGreenForToLink(Id toLinkId) {
		throw new UnsupportedOperationException("Method should not be called on this instance");
	}


	@Override
	QVehicle popFirstFromBuffer() {
		throw new UnsupportedOperationException("Method should not be called on this instance");
	}


	/**
	 * this method is there so that QLane and QLink can be addressed via the same syntax
	 */
	@Override
	QLinkInternalI getQLink() {
		throw new UnsupportedOperationException() ;
	}

	@Override
	double getInverseSimulatedFlowCapacity() {
		throw new UnsupportedOperationException() ;
	}


	@Override
	double getStorageCapacity() {
		throw new UnsupportedOperationException() ;
	}


	@Override
	int getBufferStorage() {
		throw new UnsupportedOperationException() ;
	}


	@Override
	double getLength() {
		throw new UnsupportedOperationException() ;
	}


	@Override
	void letAgentDepartWithVehicle(MobsimDriverAgent agent, QVehicle vehicle, double now) {
		vehicle.setDriver(agent);
//		NetworkRoute route = (NetworkRoute) agent.getCurrentLeg().getRoute();
		if ( agent.getDestinationLinkId().equals(link.getId()) && (agent.chooseNextLinkId() == null)) {
			// yyyy this should be handled at person level, not vehicle level.  kai, feb'10

			agent.endLegAndAssumeControl(now);
			this.addParkedVehicle(vehicle);
		} else {
			this.addDepartingVehicle(vehicle);
		}
	}


}
