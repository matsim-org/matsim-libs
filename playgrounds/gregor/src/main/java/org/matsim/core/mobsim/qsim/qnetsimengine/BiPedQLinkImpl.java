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
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.comparators.QVehicleEarliestLinkExitTimeComparator;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.lanes.vis.VisLaneModelBuilder;
import org.matsim.lanes.vis.VisLinkWLanes;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.signalsystems.mobsim.SignalizeableItem;
import org.matsim.signalsystems.model.SignalGroupState;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.vis.snapshotwriters.VisData;

//Copy and paste from QLinkImpl will be removed when QueueWithBuffer becomes interchangeable in QLinkImpl 
public class BiPedQLinkImpl extends AbstractQLink implements SignalizeableItem {

	// static variables (no problem with memory)
	final static Logger log = Logger.getLogger(BiPedQLinkImpl.class);
	private static final Comparator<QVehicle> VEHICLE_EXIT_COMPARATOR = new QVehicleEarliestLinkExitTimeComparator();
	
	/**
	 * Reference to the QueueNode which is at the end of each QueueLink instance
	 */
	private final QNode toQueueNode;

	private boolean active = false;

	private final VisData visdata;

	final double length;

	public BiPedQueueWithBuffer road ;
	
	/**
	 * A list containing all transit vehicles that are at a stop but not
	 * blocking other traffic on the lane.
	 */
	private final Queue<QVehicle> transitVehicleStopQueue = new PriorityQueue<QVehicle>(5, VEHICLE_EXIT_COMPARATOR);


	/**
	 * Initializes a QueueLink with one QueueLane.
	 * @param link2
	 * @param queueNetwork
	 * @param toNode
	 */
	public BiPedQLinkImpl(final Link link2, QNetwork network, final QNode toNode) {
		this(link2, network, toNode, new FIFOVehicleQ());
	}

	/** 
	 * This constructor allows inserting a custom vehicle queue proper, e.g. to implement passing.
	 * 
	 */
	public BiPedQLinkImpl(final Link link2, QNetwork network, final QNode toNode, final VehicleQ<QVehicle> vehicleQueue) {
		super(link2, network) ;
		this.length = this.getLink().getLength();
		this.road = new BiPedQueueWithBuffer(network,this, vehicleQueue);
		this.toQueueNode = toNode;
		this.visdata = this.new VisDataImpl() ; // instantiating this here and not earlier so we can cache some things


	}

	/* 
	 * yyyyyy There are two "active" functionalities (see isActive()).  It probably still works, but it does not look like
	 * it is intended this way.  kai, nov'11
	 */
	@Override
	void activateLink() {
		if (!this.active) {
			this.netElementActivator.activateLink(this);
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
		this.road.addFromUpstream(veh);
	}

	@Override
	void clearVehicles() {
		super.clearVehicles();

		this.road.clearVehicles();
	}

	@Override
	boolean doSimStep(double now) {
		this.road.updateRemainingFlowCapacity();

		if ( this.insertingWaitingVehiclesBeforeDrivingVehicles ) {
			this.moveWaitToRoad(now);
			this.handleTransitVehiclesInStopQueue(now);
			this.road.doSimStep(now);
		} else {
			this.handleTransitVehiclesInStopQueue(now);
			this.road.doSimStep(now);
			this.moveWaitToRoad(now);
		}
		// moveLaneToBuffer moves vehicles from lane to buffer.  Includes possible vehicle arrival.  Which, I think, would only be triggered
		// if this is the original lane.

		// moveWaitToBuffer moves waiting (i.e. just departed) vehicles into the buffer.

		this.active = this.isHavingActivity();
		return this.active;
	}


	/**
	 * Move as many waiting cars to the link as it is possible
	 *
	 * @param now
	 *          the current time
	 */
	private void moveWaitToRoad(final double now) {
		while (this.road.isAcceptingFromWait() ) {
			QVehicle veh = this.waitingList.poll();
			if (veh == null) {
				return;
			}
			
//			System.out.flush() ;
//			log.warn("veh: " + veh.getId() + " with driver " + veh.getDriver().getId() ) ;
//			System.err.flush() ;

			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentWait2LinkEvent(now, veh.getDriver().getId(), this.getLink().getId(), veh.getId())
					);

			if ( this.addTransitToStopQueue(now, veh) ) {
				continue ;
			}
			
			// * a newly departing transit vehicle comes via wait, as all other vehicles.
			// * if it has a stop on its first link, AND there is a passenger waiting, the vehicle is now in the transit stop queue
			// and all is well.
			// * if it did, however, not take a passenger on board, it is now here.  From here, it will depart like a normal vehicle,
			// which is too far downstream to be able to stop a second time on this link.
			// need to do something against that:
			if (veh.getDriver() instanceof TransitDriverAgent) {
				if ( veh.getDriver().chooseNextLinkId().equals(veh.getDriver().getCurrentLinkId())) {

// this was the old logic.  It is now jun'13, maybe delete after jun'14 (or even earlier).			
//					if ( veh.getDriver().chooseNextLinkId() == null || veh.getDriver().chooseNextLinkId().equals(veh.getDriver().getCurrentLinkId())) {
//					veh.getDriver().endLegAndComputeNextState(now);
//					this.addParkedVehicle(veh);
//					this.network.simEngine.internalInterface.arrangeNextAgentState(veh.getDriver()) ;
//					this.makeVehicleAvailableToNextDriver(veh, now);
					
					this.road.addTransitSlightlyUpstreamOfStop(veh) ;
					continue;
				}
			}

			this.road.addFromWait(veh, now);
		}
	}

	private final boolean addTransitToStopQueue(final double now, final QVehicle veh) {
		if (veh.getDriver() instanceof TransitDriverAgent) {
			TransitDriverAgent driver = (TransitDriverAgent) veh.getDriver();
			while (true) {
				TransitStopFacility stop = driver.getNextTransitStop();
				if ((stop != null) && (stop.getLinkId().equals(getLink().getId()))) {
					double delay = driver.handleTransitStop(stop, now);
					if (delay > 0.0) {
						// yy removing this condition makes at least one test fail.  I still think we could discuss doing this. kai, jun'13
						
						veh.setEarliestLinkExitTime(now + delay);
						// add it to the stop queue: vehicle that is not yet on the road will never block
						this.transitVehicleStopQueue.add(veh);
						return true;
					}
				} else {
					return false;
				}
			}
		}
		return false;
	}

	/**
	 * This method moves transit vehicles from the stop queue directly to the front of the
	 * "queue" of the QLink. An advantage is that this will observe flow
	 * capacity restrictions. 
	 */
	private void handleTransitVehiclesInStopQueue(final double now) {
		QVehicle veh;
		// handle transit traffic in stop queue
		List<QVehicle> departingTransitVehicles = null;
		while ((veh = this.transitVehicleStopQueue.peek()) != null) {
			// there is a transit vehicle.
			if (veh.getEarliestLinkExitTime() > now) {
				break;
			}
			if (departingTransitVehicles == null) {
				departingTransitVehicles = new LinkedList<QVehicle>();
			}
			departingTransitVehicles.add(this.transitVehicleStopQueue.poll());
		}
		if (departingTransitVehicles != null) {
			// add all departing transit vehicles at the front of the vehQueue
			ListIterator<QVehicle> iter = departingTransitVehicles.listIterator(departingTransitVehicles.size());
			while (iter.hasPrevious()) {
				this.road.addTransitSlightlyUpstreamOfStop(iter.previous()) ;
			}
		}
	}

	boolean handleTransitStop(final double now, final QVehicle veh, final MobsimDriverAgent driver) {
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
//						this.road.vehQueue.poll(); // remove the bus from the queue
						this.road.removeVehicleFromQueue(now) ;
						this.transitVehicleStopQueue.add(veh); // and add it to the stop queue
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
		return this.road.isNotOfferingVehicle();
	}

	@Override
	boolean isAcceptingFromUpstream() {
		return this.road.isAcceptingFromUpstream();
	}

	@Override
	public void recalcTimeVariantAttributes(double now) {
		this.road.recalcTimeVariantAttributes(now);
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
		return this.road.getVehicle( vehicleId ) ;
	}

	@Override
	public Collection<MobsimVehicle> getAllNonParkedVehicles(){
		Collection<MobsimVehicle> vehicles = new ArrayList<MobsimVehicle>();
		vehicles.addAll(this.transitVehicleStopQueue);
		vehicles.addAll(this.waitingList);
		vehicles.addAll( this.road.getAllVehicles() ) ;
		return vehicles;
	}

	/**
	 * @return the total space capacity available on that link (includes the space on lanes if available)
	 */
	double getSpaceCap() {
		return this.road.getStorageCapacity();
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
		return this.road.getSimulatedFlowCapacity() ;
	}

	@Override
	public VisData getVisData() {
		return this.visdata;
	}

	private boolean isHavingActivity() {
		/*
		 * Leave Link active as long as there are vehicles on the link (ignore
		 * buffer because the buffer gets emptied by nodes and not links) and leave
		 * link active until buffercap has accumulated (so a newly arriving vehicle
		 * is not delayed).
		 */
		return this.road.isActive()  || !this.waitingList.isEmpty() || !this.transitVehicleStopQueue.isEmpty() ;
	}

	@Override
	QVehicle popFirstVehicle() {
		return this.road.popFirstVehicle();
	}

	@Override
	QVehicle getFirstVehicle() {
		return this.road.getFirstVehicle() ;
	}

	@Override
	double getLastMovementTimeOfFirstVehicle() {
		return this.road.getLastMovementTimeOfFirstVehicle();
	}

	@Override
	public boolean hasGreenForToLink(Id toLinkId){
		return this.road.hasGreenForToLink(toLinkId);
	}

	@Override
	public void setSignalStateAllTurningMoves(SignalGroupState state) {
		this.road.setSignalStateAllTurningMoves(state);
	}

	@Override
	public void setSignalStateForTurningMove(SignalGroupState state, Id toLinkId) {
		this.road.setSignalStateForTurningMove(state, toLinkId);
	}

	@Override
	public void setSignalized(boolean isSignalized) {
		this.road.setSignalized(isSignalized);
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
			double nodeOffset = BiPedQLinkImpl.this.network.simEngine.getMobsim().getScenario().getConfig().getQSimConfigGroup().getNodeOffset(); 
			if (nodeOffset != 0.0) {
				nodeOffset = nodeOffset +2.0; // +2.0: eventually we need a bit space for the signal
				this.laneModelBuilder = new VisLaneModelBuilder();
				CoordinateTransformation transformation = new IdentityTransformation();
				this.otfLink = this.laneModelBuilder.createOTFLinkWLanes(transformation, BiPedQLinkImpl.this, nodeOffset, null);
				SnapshotLinkWidthCalculator linkWidthCalculator = BiPedQLinkImpl.this.network.getLinkWidthCalculator();
				this.laneModelBuilder.recalculatePositions(this.otfLink, linkWidthCalculator);
			}
		}

		@Override
		public Collection<AgentSnapshotInfo> getAgentSnapshotInfo( Collection<AgentSnapshotInfo> positions) {
			AgentSnapshotInfoBuilder snapshotInfoBuilder = BiPedQLinkImpl.this.network.simEngine.getAgentSnapshotInfoBuilder();
			
			VisData roadVisData = BiPedQLinkImpl.this.road.getVisData() ;

			((QueueWithBuffer.VisDataImpl)roadVisData).setOtfLink( this.otfLink ) ;
			// yyyy not so great but an elegant solution needs more thinking about visualizer structure. kai, jun'13

			positions = roadVisData.getAgentSnapshotInfo(positions) ;

			int cnt2 = 10 ; // a counter according to which non-moving items can be "spread out" in the visualization
			// initialize a bit away from the lane

			// treat vehicles from transit stops
			cnt2 = snapshotInfoBuilder.positionVehiclesFromTransitStop(positions, BiPedQLinkImpl.this.link, BiPedQLinkImpl.this.transitVehicleStopQueue, cnt2 );

			// treat vehicles from waiting list:
			cnt2 = snapshotInfoBuilder.positionVehiclesFromWaitingList(positions, BiPedQLinkImpl.this.link, cnt2,
					BiPedQLinkImpl.this.waitingList);

			cnt2 = snapshotInfoBuilder.positionAgentsInActivities(positions, BiPedQLinkImpl.this.link,
					BiPedQLinkImpl.this.getAdditionalAgentsOnLink(), cnt2);

			// return:
			return positions;
		}

	}

}
