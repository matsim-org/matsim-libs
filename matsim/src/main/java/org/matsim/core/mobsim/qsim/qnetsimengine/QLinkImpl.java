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
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.Hole;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.lanes.vis.VisLaneModelBuilder;
import org.matsim.lanes.vis.VisLinkWLanes;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.signalsystems.mobsim.DefaultSignalizeableItem;
import org.matsim.signalsystems.mobsim.SignalizeableItem;
import org.matsim.signalsystems.model.SignalGroupState;
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
 */
public class QLinkImpl extends AbstractQLink implements SignalizeableItem {

	// static variables (no problem with memory)
	final static Logger log = Logger.getLogger(QLinkImpl.class);
	/**
	 * Reference to the QueueNode which is at the end of each QueueLink instance
	 */
	private final QNode toQueueNode;

	private boolean active = false;

	private final VisData visdata;

	final double length;

	public QueueWithBuffer road ;

	/**
	 * Initializes a QueueLink with one QueueLane.
	 * @param link2
	 * @param queueNetwork
	 * @param toNode
	 */
	public QLinkImpl(final Link link2, QNetwork network, final QNode toNode) {
		this(link2, network, toNode, new FIFOVehicleQ());
	}

	/** 
	 * This constructor allows inserting a custom vehicle queue proper, e.g. to implement passing.
	 * 
	 */
	public QLinkImpl(final Link link2, QNetwork network, final QNode toNode, final VehicleQ<QVehicle> vehicleQueue) {
		super(link2, network) ;
		this.road = new QueueWithBuffer(0.0, 1.0, true, 0.0, new LinkedList<Hole>(),
				Double.NaN, Time.UNDEFINED_TIME, new ConcurrentHashMap<QVehicle, Double>(), new LinkedList<QVehicle>(),
				null, this);
		this.toQueueNode = toNode;
		this.road.vehQueue = vehicleQueue;
		this.length = this.getLink().getLength();
		this.road.freespeedTravelTime = this.length / this.getLink().getFreespeed();
		if (Double.isNaN(this.road.freespeedTravelTime)) {
			throw new IllegalStateException("Double.NaN is not a valid freespeed travel time for a link. Please check the attributes length and freespeed!");
		}
		this.road.calculateCapacities();
		this.visdata = this.new VisDataImpl() ; // instantiating this here so we can cache some things

		if ( QueueWithBuffer.HOLES ) {
			for ( int ii=0 ; ii<this.road.storageCapacity; ii++ ) {
				Hole hole = new Hole() ;	
				hole.setEarliestLinkExitTime( Double.NEGATIVE_INFINITY ) ;
				this.road.holes.add(hole) ;
			}
			// yyyyyy this does, once more, not work with variable vehicle sizes.  kai, may'13
		}

	}

	/* 
	 * yyyyyy There are two "active" functionalities (see isActive()).  It probably still works, but it does not look like
	 * it is intended this way.  kai, nov'11
	 */
	@Override
	void activateLink() {
		if (!this.active) {
			netElementActivator.activateLink(this);
			this.active = true;
		}
	}

	/**
	 * Adds a vehicle to the link (i.e. the "queue"), called by
	 * {@link QNode#moveVehicleOverNode(QVehicle, QueueLane, double)}.
	 *
	 * @param veh
	 *          the vehicle
	 */
	@Override
	final void addFromUpstream(final QVehicle veh) {
		double now = network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		activateLink();
		this.road.linkEnterTimeMap.put(veh, now);
		this.road.usedStorageCapacity += veh.getSizeInEquivalents();
		double vehicleTravelTime = this.length / veh.getMaximumVelocity();
		double earliestExitTime = now + Math.max(this.road.freespeedTravelTime, vehicleTravelTime);
		earliestExitTime = Math.floor(earliestExitTime);
		veh.setEarliestLinkExitTime(earliestExitTime);
		veh.setCurrentLink(this.getLink());
		this.road.vehQueue.add(veh);
		this.network.simEngine.getMobsim().getEventsManager().processEvent(
				new LinkEnterEvent(now, veh.getDriver().getId(),
						this.getLink().getId(), veh.getId()));
		if ( QueueWithBuffer.HOLES ) {
			road.holes.poll();
		}
	}

	@Override
	void clearVehicles() {
		super.clearVehicles();

		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();

		for (QVehicle veh : this.road.vehQueue) {
			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			this.network.simEngine.getMobsim().getAgentCounter().incLost();
			this.network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		this.road.vehQueue.clear();
		this.road.linkEnterTimeMap.clear();

		for (QVehicle veh : this.road.buffer) {
			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			this.network.simEngine.getMobsim().getAgentCounter().incLost();
			this.network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		this.road.buffer.clear();
		this.road.usedBufferStorageCapacity = 0;
	}


	@Override
	boolean doSimStep(double now) {
		road.updateRemainingFlowCapacity();

		if ( this.insertingWaitingVehiclesBeforeDrivingVehicles ) {
			moveWaitToBuffer(now);
			road.moveLaneToBuffer(now);
		} else {
			road.moveLaneToBuffer(now);
			moveWaitToBuffer(now);
		}
		// moveLaneToBuffer moves vehicles from lane to buffer.  Includes possible vehicle arrival.  Which, I think, would only be triggered
		// if this is the original lane.

		// moveWaitToBuffer moves waiting (i.e. just departed) vehicles into the buffer.

		this.active = this.isActive();
		return active;
	}


	/**
	 * Move as many waiting cars to the link as it is possible
	 *
	 * @param now
	 *          the current time
	 */
	private void moveWaitToBuffer(final double now) {
		while (road.hasFlowCapacityLeftAndBufferSpace()) {
			QVehicle veh = this.waitingList.poll();
			if (veh == null) {
				return;
			}

			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentWait2LinkEvent(now, veh.getDriver().getId(), this.getLink().getId(), veh.getId()));
			boolean handled = this.addTransitToBuffer(now, veh);

			if (!handled) {


				if (veh.getDriver() instanceof TransitDriverAgent) {
					TransitDriverAgent trDriver = (TransitDriverAgent) veh.getDriver();
					Id nextLinkId = trDriver.chooseNextLinkId();
					if (nextLinkId == null || nextLinkId.equals(trDriver.getCurrentLinkId())) {
						// special case: transit drivers can specify the next link being the current link
						// this can happen when a transit-lines route leads over exactly one link
						// normally, vehicles would not even drive on that link, but transit vehicles must
						// "drive" on that link in order to handle the stops on that link
						// so allow them to return some non-null link id in chooseNextLink() in order to be
						// placed on the link, and here we'll remove them again if needed...
						// ugly hack, but I didn't find a nicer solution sadly... mrieser, 5mar2011
						trDriver.endLegAndComputeNextState(now);
						this.addParkedVehicle(veh);
						this.network.simEngine.internalInterface.arrangeNextAgentState(trDriver) ;
						this.makeVehicleAvailableToNextDriver(veh, now);
						// remove _after_ processing the arrival to keep link active
						this.road.vehQueue.poll();
						this.road.usedStorageCapacity -= veh.getSizeInEquivalents();
						if ( QueueWithBuffer.HOLES ) {
							Hole hole = new Hole() ;
							hole.setEarliestLinkExitTime( now + this.link.getLength()*3600./15./1000. ) ;
							road.holes.add( hole ) ;
						}
						continue;
					}
				}

				road.addToBuffer(veh, now);
				//				this.linkEnterTimeMap.put(veh, now);
				// (yyyyyy really??  kai, jan'11)
			}
		}
	}

	/**
	 * This method
	 * moves transit vehicles from the stop queue directly to the front of the
	 * "queue" of the QLink. An advantage is that this will observe flow
	 * capacity restrictions. 
	 */
	void moveTransitToQueue(final double now) {
		QVehicle veh;
		// handle transit traffic in stop queue
		List<QVehicle> departingTransitVehicles = null;
		while ((veh = transitVehicleStopQueue.peek()) != null) {
			// there is a transit vehicle.
			if (veh.getEarliestLinkExitTime() > now) {
				break;
			}
			if (departingTransitVehicles == null) {
				departingTransitVehicles = new LinkedList<QVehicle>();
			}
			departingTransitVehicles.add(transitVehicleStopQueue.poll());
		}
		if (departingTransitVehicles != null) {
			// add all departing transit vehicles at the front of the vehQueue
			ListIterator<QVehicle> iter = departingTransitVehicles.listIterator(departingTransitVehicles.size());
			while (iter.hasPrevious()) {
				this.road.vehQueue.addFirst(iter.previous());
			}
		}
	}


	boolean handleTransitStop(final double now, final QVehicle veh,
			final MobsimDriverAgent driver) {
		boolean handled = false;
		// handle transit driver if necessary
		if (driver instanceof TransitDriverAgent) {
			TransitDriverAgent transitDriver = (TransitDriverAgent) veh.getDriver();
			TransitStopFacility stop = transitDriver.getNextTransitStop();
			if ((stop != null) && (stop.getLinkId().equals(getLink().getId()))) {
				double delay = transitDriver.handleTransitStop(stop, now);
				if (delay > 0.0) {

					veh.setEarliestLinkExitTime(now + delay);
					// (if the vehicle is not removed from the queue in the following lines, then this will effectively block the lane

					if (!stop.getIsBlockingLane()) {
						this.road.vehQueue.poll(); // remove the bus from the queue
						transitVehicleStopQueue.add(veh); // and add it to the stop queue
					}
				}
				/* start over: either this veh is still first in line,
				 * but has another stop on this link, or on another link, then it is moved on
				 */
				handled = true;
			}
		}
		return handled;
	}

	@Override
	boolean isNotOfferingVehicle() {
		return this.road.buffer.isEmpty();
	}

	@Override
	boolean isAcceptingFromUpstream() {
		double now = network.simEngine.getMobsim().getSimTimer().getTimeOfDay() ;

		boolean storageOk = this.road.usedStorageCapacity < this.road.storageCapacity ;
		if ( !QueueWithBuffer.HOLES ) {
			return storageOk ;
		}
		// continue only if HOLES
		if ( !storageOk ) {
			return false ;
		}
		// at this point, storage is ok, so start checking holes:
		QItem hole = road.holes.peek();
		if ( hole==null ) { // no holes available at all; in theory, this should not happen since covered by !storageOk
			//			log.warn( " !hasSpace since no holes available ") ;
			return false ;
		}
		if ( hole.getEarliestLinkExitTime() > now ) {
			//			log.warn( " !hasSpace since all hole arrival times lie in future ") ;
			return false ;
		}
		return true ;
	}


	@Override
	public void recalcTimeVariantAttributes(double now) {
		this.road.freespeedTravelTime = this.length / this.getLink().getFreespeed(now);
		road.calculateFlowCapacity(now);
		road.calculateStorageCapacity(now);
	}

	@Override
	QVehicle getVehicle(Id vehicleId) {
		QVehicle ret = super.getVehicle(vehicleId);
		if (ret != null) {
			return ret;
		}
		for (QVehicle veh : this.road.vehQueue) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		for (QVehicle veh : this.road.buffer) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		for (QVehicle veh : this.waitingList) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		return null;
	}

	@Override
	public Collection<MobsimVehicle> getAllNonParkedVehicles(){
		Collection<MobsimVehicle> vehicles = new ArrayList<MobsimVehicle>();
		vehicles.addAll(this.transitVehicleStopQueue);
		vehicles.addAll(this.waitingList);
		vehicles.addAll(this.road.vehQueue);
		vehicles.addAll(this.road.buffer);
		return vehicles;
	}

	/**
	 * @return the total space capacity available on that link (includes the space on lanes if available)
	 */
	double getSpaceCap() {
		return this.road.storageCapacity;
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
		return this.road.flowCapacityPerTimeStep;
	}

	@Override
	public VisData getVisData() {
		return this.visdata;
	}

	private boolean isActive() {
		/*
		 * Leave Link active as long as there are vehicles on the link (ignore
		 * buffer because the buffer gets emptied by nodes and not links) and leave
		 * link active until buffercap has accumulated (so a newly arriving vehicle
		 * is not delayed).
		 */
		boolean active = (this.road.flowcap_accumulate < 1.0) || (!this.road.vehQueue.isEmpty()) 
				|| (!this.waitingList.isEmpty() || (!this.transitVehicleStopQueue.isEmpty()));
		return active;
	}

	@Override
	QVehicle popFirstVehicle() {
		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		QVehicle veh = this.road.buffer.poll();
		this.road.usedBufferStorageCapacity = this.road.usedBufferStorageCapacity - veh.getSizeInEquivalents();
		this.road.bufferLastMovedTime = now; // just in case there is another vehicle in the buffer that is now the new front-most
		this.road.linkEnterTimeMap.remove(veh);
		this.network.simEngine.getMobsim().getEventsManager().processEvent(new LinkLeaveEvent(now, veh.getDriver().getId(), this.getLink().getId(), veh.getId()));
		return veh;
	}

	@Override
	QVehicle getFirstVehicle() {
		return this.road.buffer.peek();
	}

	@Override
	double getLastMovementTimeOfFirstVehicle() {
		return this.road.bufferLastMovedTime;
	}

	@Override
	public boolean hasGreenForToLink(Id toLinkId){
		if (this.road.qSignalizedItem != null){
			return this.road.qSignalizedItem.isLinkGreenForToLink(toLinkId);
		}
		return true; //the lane is not signalized and thus always green
	}

	@Override
	public void setSignalStateAllTurningMoves(SignalGroupState state) {
		this.road.qSignalizedItem.setSignalStateAllTurningMoves(state);

		this.road.thisTimeStepGreen  = this.road.qSignalizedItem.isLinkGreen();
		// (this is only for capacity accumulation)
	}

	@Override
	public void setSignalStateForTurningMove(SignalGroupState state, Id toLinkId) {
		if (!this.getToNode().getNode().getOutLinks().containsKey(toLinkId)){
			throw new IllegalArgumentException("ToLink " + toLinkId + " is not reachable from QLink Id " + this.getLink().getId());
		}
		this.road.qSignalizedItem.setSignalStateForTurningMove(state, toLinkId);

		this.road.thisTimeStepGreen = this.road.qSignalizedItem.isLinkGreen();
		// (this is only for capacity accumulation.  As soon as at least one turning relation is green, the "link" is considered
		// green).
	}

	@Override
	public void setSignalized(boolean isSignalized) {
		this.road.qSignalizedItem  = new DefaultSignalizeableItem(this.getLink().getToNode().getOutLinks().keySet());
	}


	/**
	 * Inner class to encapsulate visualization methods
	 *
	 * @author dgrether
	 */
	class VisDataImpl implements VisData {

		private VisLaneModelBuilder laneModelBuilder = null;
		private VisLinkWLanes otfLink = null;

		private VisDataImpl() {
			double nodeOffset = QLinkImpl.this.network.simEngine.getMobsim().getScenario().getConfig().otfVis().getNodeOffset(); 
			if (nodeOffset != 0.0) {
				nodeOffset = nodeOffset +2.0; // +2.0: eventually we need a bit space for the signal
				laneModelBuilder = new VisLaneModelBuilder();
				CoordinateTransformation transformation = new IdentityTransformation();
				otfLink = laneModelBuilder.createOTFLinkWLanes(transformation, QLinkImpl.this, nodeOffset, null);
				SnapshotLinkWidthCalculator linkWidthCalculator = QLinkImpl.this.network.getLinkWidthCalculator();
				laneModelBuilder.recalculatePositions(otfLink, linkWidthCalculator);
			}
		}

		@Override
		public Collection<AgentSnapshotInfo> getAgentSnapshotInfo( final Collection<AgentSnapshotInfo> positions) {
			AgentSnapshotInfoBuilder snapshotInfoBuilder = QLinkImpl.this.network.simEngine.getAgentSnapshotInfoBuilder();

			double numberOfVehiclesDriving = QLinkImpl.this.road.buffer.size() + QLinkImpl.this.road.vehQueue.size();
			if (numberOfVehiclesDriving > 0) {
				double now = QLinkImpl.this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
				Link link = QLinkImpl.this.getLink();
				double spacing = snapshotInfoBuilder.calculateVehicleSpacing(link.getLength(), numberOfVehiclesDriving,
						QLinkImpl.this.road.storageCapacity, QLinkImpl.this.road.bufferStorageCapacity); 
				double freespeedTraveltime = link.getLength() / link.getFreespeed(now);

				double lastDistanceFromFromNode = Double.NaN;
				for (QVehicle veh : QLinkImpl.this.road.buffer){
					lastDistanceFromFromNode = createAndAddVehiclePositionAndReturnDistance(positions, snapshotInfoBuilder, now,
							lastDistanceFromFromNode, link, spacing, freespeedTraveltime, veh);
				}
				for (QVehicle veh : QLinkImpl.this.road.vehQueue) {
					lastDistanceFromFromNode = createAndAddVehiclePositionAndReturnDistance(positions, snapshotInfoBuilder, now,
							lastDistanceFromFromNode, link, spacing, freespeedTraveltime, veh);
				}
			}


			int cnt2 = 10 ; // a counter according to which non-moving items can be "spread out" in the visualization
			// initialize a bit away from the lane

			// treat vehicles from transit stops
			cnt2 = snapshotInfoBuilder.positionVehiclesFromTransitStop(positions, link, transitVehicleStopQueue, cnt2 );

			// treat vehicles from waiting list:
			cnt2 = snapshotInfoBuilder.positionVehiclesFromWaitingList(positions, QLinkImpl.this.link, cnt2,
					QLinkImpl.this.waitingList);

			cnt2 = snapshotInfoBuilder.positionAgentsInActivities(positions, QLinkImpl.this.link,
					QLinkImpl.this.getAdditionalAgentsOnLink(), cnt2);

			// return:
			return positions;
		}

		private double createAndAddVehiclePositionAndReturnDistance(final Collection<AgentSnapshotInfo> positions,
				AgentSnapshotInfoBuilder snapshotInfoBuilder, double now, double lastDistanceFromFromNode, Link link,
				double spacing, double freespeedTraveltime, QVehicle veh)
		{
			double travelTime = Double.POSITIVE_INFINITY ;
			if ( QLinkImpl.this.road.linkEnterTimeMap.get(veh) != null ) {
				// (otherwise the vehicle has never entered from an intersection)
				travelTime = now - QLinkImpl.this.road.linkEnterTimeMap.get(veh);
			}
			lastDistanceFromFromNode = snapshotInfoBuilder.calculateDistanceOnVectorFromFromNode(link.getLength(), spacing, 
					lastDistanceFromFromNode, now, freespeedTraveltime, travelTime);
			Integer lane = snapshotInfoBuilder.guessLane(veh, NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link));
			double speedValue = snapshotInfoBuilder.calcSpeedValueBetweenZeroAndOne(veh, 
					QLinkImpl.this.road.inverseFlowCapacityPerTimeStep, now, link.getFreespeed());
			if (this.otfLink != null){
				snapshotInfoBuilder.createAndAddVehiclePosition(positions, this.otfLink.getLinkStartCoord(), this.otfLink.getLinkEndCoord(), 
						QLinkImpl.this.length, this.otfLink.getEuklideanDistance(), veh, 
						lastDistanceFromFromNode, lane, speedValue);
			}
			else {
				snapshotInfoBuilder.createAndAddVehiclePosition(positions, link.getFromNode().getCoord(), link.getToNode().getCoord(), 
						link.getLength(), ((LinkImpl)link).getEuklideanDistance() , veh, lastDistanceFromFromNode, lane, speedValue);
			}
			return lastDistanceFromFromNode;
		}
	}

	static class Hole extends QItem {
		private double earliestLinkEndTime ;

		@Override
		public double getEarliestLinkExitTime() {
			return earliestLinkEndTime;
		}

		@Override
		public void setEarliestLinkExitTime(double earliestLinkEndTime) {
			this.earliestLinkEndTime = earliestLinkEndTime;
		}
	}

}
