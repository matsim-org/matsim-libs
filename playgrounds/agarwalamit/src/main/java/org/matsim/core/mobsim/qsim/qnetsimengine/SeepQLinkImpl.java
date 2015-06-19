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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.lanes.vis.VisLaneModelBuilder;
import org.matsim.lanes.vis.VisLinkWLanes;
import org.matsim.signals.mobsim.SignalizeableItem;
import org.matsim.vehicles.Vehicle;
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
public final class SeepQLinkImpl extends AbstractQLink implements SignalizeableItem {

	public interface LaneFactory {

		/**
		 * If this QLinkImpl is passed an instance of this factory upon construction,
		 * it will call back this factory within the constructor (!) to obtain a road and pass
		 * itself to the creation method.
		 */
		public QLaneI createLane(SeepQLinkImpl qLinkImpl);

	}

	// static variables (no problem with memory)
	final static Logger log = Logger.getLogger(SeepQLinkImpl.class);
//	private static final Comparator<QVehicle> VEHICLE_EXIT_COMPARATOR = new QVehicleEarliestLinkExitTimeComparator();

	/**
	 * Reference to the QueueNode which is at the end of each QueueLink instance
	 */
	private final QNode toQueueNode;

	private final VisData visdata;

	public final QLaneI road;

	/**
	 * Initializes a QueueLink with one QueueLane.
	 */
	public SeepQLinkImpl(final Link link2, QNetwork network, final QNode toNode) {
		this(link2, network, toNode, new FIFOVehicleQ());
	}

	/** 
	 * This constructor allows inserting a custom vehicle queue proper, e.g. to implement passing.
	 * 
	 */
	public SeepQLinkImpl(final Link link2, QNetwork network, final QNode toNode, final VehicleQ<QVehicle> vehicleQueue) {
		super(link2, network) ;
		//--
		QueueWithBuffer.Builder builder = new QueueWithBuffer.Builder(this) ;
		builder.setVehicleQueue(vehicleQueue);
		this.road = builder.build() ;
		//--
		this.toQueueNode = toNode;
		this.visdata = this.new VisDataImpl() ; // instantiating this here and not earlier so we can cache some things
	  super.transitQLink = new TransitQLink(this.road);
	}
	
	/** 
	 * This constructor allows inserting a custom vehicle queue proper, e.g. to implement passing.
	 * 
	 */
	public SeepQLinkImpl(final Link link2, QNetwork network, final QNode toNode, final LaneFactory roadFactory) {
		super(link2, network) ;
        // The next line must must by contract stay within the constructor,
		// so that the caller can use references to the created roads to wire them together,
		// if it must.
		this.road = roadFactory.createLane(this); 
		this.toQueueNode = toNode;
		this.visdata = this.new VisDataImpl() ; // instantiating this here and not earlier so we can cache some things
	  super.transitQLink = new TransitQLink(this.road);
	}



	/**
	 * Adds a vehicle to the link (i.e. the "queue").
	 *
	 * @param veh
	 *          the vehicle
	 */
	@Override
	final void addFromUpstream(final QVehicle veh) {
		road.addFromUpstream(veh);
		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay() ;
		this.network.simEngine.getMobsim().getEventsManager().processEvent(
				new LinkEnterEvent(now, veh.getDriver().getId(), this.link.getId(), veh.getId()));

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
			this.transitQLink.handleTransitVehiclesInStopQueue(now);
			road.doSimStep(now);
		} else {
			this.transitQLink.handleTransitVehiclesInStopQueue(now);
			road.doSimStep(now);
			this.moveWaitToRoad(now);
		}
		this.active = this.checkForActivity();
		return active;
	}


	/**
	 * Move as many waiting cars to the link as it is possible
	 *
	 * @param now
	 *          the current time
	 */
	private void moveWaitToRoad(final double now) {
		while (road.isAcceptingFromWait() ) {
			QVehicle veh = this.waitingList.poll();
			if (veh == null) {
				return;
			}

			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new Wait2LinkEvent(now, veh.getDriver().getId(), this.getLink().getId(), veh.getId())
					);

			if ( this.transitQLink.addTransitToStopQueue(now, veh, this.getLink().getId()) ) {
				continue ;
			}

			if (veh.getDriver().isWantingToArriveOnCurrentLink( )) {
				// If the driver wants to stop on this link, give them a special treatment.
				// addFromWait doesn't work here, because after that, they cannot stop anymore.
				road.addTransitSlightlyUpstreamOfStop(veh) ;
				continue;
			}

			road.addFromWait(veh, now);
		}
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

		road.changeUnscaledFlowCapacityPerSecond(((LinkImpl)this.link).getFlowCapacity(now), now);
		road.changeEffectiveNumberOfLanes(this.link.getNumberOfLanes(now), now);
		
		road.recalcTimeVariantAttributes(now);
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
		return this.road.getVehicle( vehicleId ) ;
	}

	@Override
	public Collection<MobsimVehicle> getAllNonParkedVehicles(){
		Collection<MobsimVehicle> vehicles = new ArrayList<>();
		vehicles.addAll(this.transitQLink.getTransitVehicleStopQueue());
		vehicles.addAll(this.waitingList);
		vehicles.addAll( road.getAllVehicles() ) ;
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

	private boolean checkForActivity() {
		/*
		 * Leave Link active as long as there are vehicles on the link (ignore
		 * buffer because the buffer gets emptied by nodes and not links) and leave
		 * link active until buffercap has accumulated (so a newly arriving vehicle
		 * is not delayed).
		 */
		return road.isActive()  || !this.waitingList.isEmpty() || !this.transitQLink.getTransitVehicleStopQueue().isEmpty() ;
	}

	@Override
	QVehicle popFirstVehicle() {
		return road.popFirstVehicle();
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
	public boolean hasGreenForToLink(Id<Link> toLinkId){
		return road.hasGreenForToLink(toLinkId);
	}

	@Override
	public void setSignalStateAllTurningMoves(SignalGroupState state) {
		((SignalizeableItem) road).setSignalStateAllTurningMoves(state);
	}

	@Override
	public void setSignalStateForTurningMove(SignalGroupState state, Id<Link> toLinkId) {
		((SignalizeableItem) road).setSignalStateForTurningMove(state, toLinkId);
	}

	@Override
	public void setSignalized(boolean isSignalized) {
		((SignalizeableItem) road).setSignalized(isSignalized);
	}

	/**
	 * Inner class to encapsulate visualization methods
	 *
	 * @author dgrether
	 */
	class VisDataImpl implements VisData {

		private VisLaneModelBuilder visModelBuilder = null;
		private VisLinkWLanes visLink = null;

		private VisDataImpl() {
			double nodeOffset = SeepQLinkImpl.this.network.simEngine.getMobsim().getScenario().getConfig().qsim().getNodeOffset(); 
			if (nodeOffset != 0.0) {
				nodeOffset = nodeOffset +2.0; // +2.0: eventually we need a bit space for the signal
				visModelBuilder = new VisLaneModelBuilder();
				CoordinateTransformation transformation = new IdentityTransformation();
				visLink = visModelBuilder.createVisLinkLanes(transformation, SeepQLinkImpl.this, nodeOffset, null);
				SnapshotLinkWidthCalculator linkWidthCalculator = SeepQLinkImpl.this.network.getLinkWidthCalculator();
				visModelBuilder.recalculatePositions(visLink, linkWidthCalculator);
			}
		}

		@Override
		public Collection<AgentSnapshotInfo> addAgentSnapshotInfo( Collection<AgentSnapshotInfo> positions) {
			AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder = SeepQLinkImpl.this.network.simEngine.getAgentSnapshotInfoBuilder();

			VisData roadVisData = road.getVisData() ;
			if (visLink != null) {
				((QueueWithBuffer.VisDataImpl)roadVisData).setVisInfo(visLink.getLinkStartCoord(), visLink.getLinkEndCoord(), visLink.getEuklideanDistance()) ;
				// yyyy not so great but an elegant solution needs more thinking about visualizer structure. kai, jun'13
			}

			positions = roadVisData.addAgentSnapshotInfo(positions) ;

			int cnt2 = 10 ; // a counter according to which non-moving items can be "spread out" in the visualization
			// initialize a bit away from the lane

			// treat vehicles from transit stops
			cnt2 = snapshotInfoBuilder.positionVehiclesFromTransitStop(positions, link, transitQLink.getTransitVehicleStopQueue(), cnt2 );

			// treat vehicles from waiting list:
			cnt2 = snapshotInfoBuilder.positionVehiclesFromWaitingList(positions, SeepQLinkImpl.this.link, cnt2,
					SeepQLinkImpl.this.waitingList);

			cnt2 = snapshotInfoBuilder.positionAgentsInActivities(positions, SeepQLinkImpl.this.link,
					SeepQLinkImpl.this.getAdditionalAgentsOnLink(), cnt2);

			return positions;
		}

	}

}
