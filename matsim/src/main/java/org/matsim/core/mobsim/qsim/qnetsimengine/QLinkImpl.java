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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
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
		this.length = this.getLink().getLength();
		this.road = new QueueWithBuffer(this, vehicleQueue);
		this.toQueueNode = toNode;
		this.visdata = this.new VisDataImpl() ; // instantiating this here so we can cache some things


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
		road.addFromUpstream(veh);
	}

	@Override
	void clearVehicles() {
		super.clearVehicles();

		road.clearVehicles();
	}

	@Override
	boolean doSimStep(double now) {
		road.updateRemainingFlowCapacity();

		if ( this.insertingWaitingVehiclesBeforeDrivingVehicles ) {
			this.moveWaitToRoad(now);
			this.handleTransitVehiclesInStopQueue(now);
			road.moveLaneToBuffer(now);
		} else {
			this.handleTransitVehiclesInStopQueue(now);
			road.moveLaneToBuffer(now);
			this.moveWaitToRoad(now);
		}
		// moveLaneToBuffer moves vehicles from lane to buffer.  Includes possible vehicle arrival.  Which, I think, would only be triggered
		// if this is the original lane.

		// moveWaitToBuffer moves waiting (i.e. just departed) vehicles into the buffer.

		this.active = this.isHavingActivity();
		return active;
	}


	/**
	 * Move as many waiting cars to the link as it is possible
	 *
	 * @param now
	 *          the current time
	 */
	private void moveWaitToRoad(final double now) {
		while (road.hasFlowCapacityLeftAndBufferSpace()) {
			QVehicle veh = this.waitingList.poll();
			if (veh == null) {
				return;
			}

			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentWait2LinkEvent(now, veh.getDriver().getId(), this.getLink().getId(), veh.getId()));

			if ( this.addTransitToStopQueue(now, veh) ) {
				continue ;
			}

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
					// (see also moveLaneToBuffer() )
					
					// The situation may have changed a bit: it used to be that agents that had the next activity on the same link would
					// essentially first enter the vehicle and nearly make a departure, and then turn around.  (This was since we originally 
					// essentially had driver-vehicle-units.)  Thus a pt vehicle that wanted to make a real departure needed some workaround.
					// I don't think this is the case any more; now the mobsim agent finds out that the next activity is on the same link 
					// before entering the vehicle.  Thus, this work around may no longer be necessary.  Tests, however, still fail;
					// the reason is that pt vehicles without passengers return "false" in the addTransitToStopQueue method above and
					// then need to be caught here.  Seems to me that this could be fixed--???  kai, jun'13
					trDriver.endLegAndComputeNextState(now);
					this.addParkedVehicle(veh);
					this.network.simEngine.internalInterface.arrangeNextAgentState(trDriver) ;
					this.makeVehicleAvailableToNextDriver(veh, now);

//					// remove _after_ processing the arrival to keep link active
//					road.removeVehicleFromQueue(now, veh) ;
//					// yyyy this is actually quite weird.  why should we remove a vehicle from the road queue if it is not on the road in the
//					// first place???  Possibly, this works only because it is never (or rarely) triggered??? kai, jun'13
					
					continue;
				}
			}

			road.addFromWait(veh, now);
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
						transitVehicleStopQueue.add(veh);
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
		return road.isNotOfferingVehicle();
	}

	@Override
	boolean isAcceptingFromUpstream() {
		return road.isAcceptingFromUpstream();
	}

	@Override
	public void recalcTimeVariantAttributes(double now) {
		road.recalcTimeVariantAttributes(now);
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
		road.getAllVehicles(vehicles);
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
		return road.isActive()  || !this.waitingList.isEmpty() || !this.transitVehicleStopQueue.isEmpty() ;
	}

	@Override
	QVehicle popFirstVehicle() {
		return road.popFirstVehicle();
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
		return road.hasGreenForToLink(toLinkId);
	}

	@Override
	public void setSignalStateAllTurningMoves(SignalGroupState state) {
		road.setSignalStateAllTurningMoves(state);
	}

	@Override
	public void setSignalStateForTurningMove(SignalGroupState state, Id toLinkId) {
		road.setSignalStateForTurningMove(state, toLinkId);
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
						QLinkImpl.this.road.getStorageCapacity(), QLinkImpl.this.road.bufferStorageCapacity); 
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
//			double travelTime = Double.POSITIVE_INFINITY ;
//			if ( QLinkImpl.this.road.linkEnterTimeMap.get(veh) != null ) {
//				// (otherwise the vehicle has never entered from an intersection)
//				travelTime = now - QLinkImpl.this.road.linkEnterTimeMap.get(veh);
//			}
			double remainingTravelTime = veh.getEarliestLinkExitTime() - now ;
			lastDistanceFromFromNode = snapshotInfoBuilder.calculateDistanceOnVectorFromFromNode2(link.getLength(), spacing, 
					lastDistanceFromFromNode, now, freespeedTraveltime, remainingTravelTime);
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

}
