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
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.lanes.vis.VisLaneModelBuilder;
import org.matsim.lanes.vis.VisLinkWLanes;
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
public final class QLinkImpl extends AbstractQLink implements SignalizeableItem {
	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(QLinkImpl.class);

	public interface LaneFactory {

		/**
		 * If this QLinkImpl is passed an instance of this factory upon construction,
		 * it will call back this factory within the constructor (!) to obtain a road and pass
		 * itself to the creation method.
		 */
		public QLaneI createLane(QLinkImpl qLinkImpl);

	}

	/**
	 * Reference to the QueueNode which is at the end of each QueueLink instance
	 */
	private final QNode toQNode;

	private final VisData visdata;

	public final QLaneI qlane;

	/**
	 * Initializes a QueueLink with one QueueLane.
	 */
	public QLinkImpl(final Link link2, QNetwork network, final QNode toNode) {
		this(link2, network, toNode, new FIFOVehicleQ());
	}

	/** 
	 * This constructor allows inserting a custom vehicle queue proper, e.g. to implement passing.
	 * 
	 */
	public QLinkImpl(final Link link2, QNetwork network, final QNode toNode, final VehicleQ<QVehicle> vehicleQueue) {
		// yy get rid of this c'tor (since the one with queueWithBuffer is more flexible)?
		super(link2, network) ;
		//--
		QueueWithBuffer.Builder builder = new QueueWithBuffer.Builder(this) ;
		builder.setVehicleQueue(vehicleQueue);
		this.qlane = builder.build() ;
		//--
		this.toQNode = toNode;
		this.visdata = this.new VisDataImpl() ; // instantiating this here and not earlier so we can cache some things
	  super.transitQLink = new TransitQLink(this.qlane);
	}
	
	public QLinkImpl( final Link link2, QNetwork network, final QNode toNode, final QLaneI queueWithBuffer ) {
		super(link2, network) ;
		this.qlane = queueWithBuffer ;
		this.toQNode = toNode;
		this.visdata = this.new VisDataImpl() ; // instantiating this here and not earlier so we can cache some things
	  super.transitQLink = new TransitQLink(this.qlane);
	}
	
	/** 
	 * This constructor allows inserting a custom vehicle queue proper, e.g. to implement passing.
	 * 
	 */
	public QLinkImpl(final Link link2, QNetwork network, final QNode toNode, final LaneFactory roadFactory) {
		super(link2, network) ;
        // The next line must must by contract stay within the constructor,
		// so that the caller can use references to the created roads to wire them together,
		// if it must.
		this.qlane = roadFactory.createLane(this); 
		this.toQNode = toNode;
		this.visdata = this.new VisDataImpl() ; // instantiating this here and not earlier so we can cache some things
	  super.transitQLink = new TransitQLink(this.qlane);
	}



	/**
	 * Adds a vehicle to the link (i.e. the "queue").
	 *
	 * @param veh
	 *          the vehicle
	 */
	@Override
	final void addFromUpstream(final QVehicle veh) {
		qlane.addFromUpstream(veh);
		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay() ;
		this.network.simEngine.getMobsim().getEventsManager().processEvent(
				new LinkEnterEvent(now, veh.getId(), this.link.getId()));

	}

	@Override
	void clearVehicles() {
		super.clearVehicles();

		qlane.clearVehicles();
	}

	@Override
	boolean doSimStep(double now) {
		qlane.updateRemainingFlowCapacity();

		if ( this.insertingWaitingVehiclesBeforeDrivingVehicles ) {
			this.moveWaitToRoad(now);
			this.transitQLink.handleTransitVehiclesInStopQueue(now);
			qlane.doSimStep(now);
		} else {
			this.transitQLink.handleTransitVehiclesInStopQueue(now);
			qlane.doSimStep(now);
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
		while (qlane.isAcceptingFromWait() ) {
			QVehicle veh = this.waitingList.poll();
			if (veh == null) {
				return;
			}

			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new VehicleEntersTrafficEvent(now, veh.getDriver().getId(), this.getLink().getId(), veh.getId(), veh.getDriver().getMode(), 1.0));

			if ( this.transitQLink.addTransitToStopQueue(now, veh, this.getLink().getId()) ) {
				continue ;
			}

			if ( veh.getDriver().isWantingToArriveOnCurrentLink() ) {
				// If the driver wants to stop (again) on this link, give them a special treatment.
				// addFromWait doesn't work here, because after that, they cannot stop anymore.
				qlane.addTransitSlightlyUpstreamOfStop(veh) ;
				continue;
			}

			qlane.addFromWait(veh, now);
		}
	}

	@Override
	boolean isNotOfferingVehicle() {
		return qlane.isNotOfferingVehicle();
	}

	@Override
	boolean isAcceptingFromUpstream() {
		return qlane.isAcceptingFromUpstream();
	}

	@Override
	public void recalcTimeVariantAttributes(double now) {

		qlane.changeUnscaledFlowCapacityPerSecond(((LinkImpl)this.link).getFlowCapacity(now), now);
		qlane.changeEffectiveNumberOfLanes(this.link.getNumberOfLanes(now), now);
		
		qlane.recalcTimeVariantAttributes(now);
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
		return this.qlane.getVehicle( vehicleId ) ;
	}

	@Override
	public Collection<MobsimVehicle> getAllNonParkedVehicles(){
		Collection<MobsimVehicle> vehicles = new ArrayList<>();
		vehicles.addAll(this.transitQLink.getTransitVehicleStopQueue());
		vehicles.addAll(this.waitingList);
		vehicles.addAll( qlane.getAllVehicles() ) ;
		return vehicles;
	}

	/**
	 * @return the total space capacity available on that link (includes the space on lanes if available)
	 */
	double getSpaceCap() {
		return this.qlane.getStorageCapacity();
	}

	@Override
	public Link getLink() {
		return this.link;
	}

	@Override
	public QNode getToNode() {
		return this.toQNode;
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
		return this.qlane.getSimulatedFlowCapacity() ;
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
		return qlane.isActive()  || !this.waitingList.isEmpty() || !this.transitQLink.getTransitVehicleStopQueue().isEmpty() ;
	}

	@Override
	public void setSignalStateAllTurningMoves(SignalGroupState state) {
		((SignalizeableItem) qlane).setSignalStateAllTurningMoves(state);
	}

	@Override
	public void setSignalStateForTurningMove(SignalGroupState state, Id<Link> toLinkId) {
		((SignalizeableItem) qlane).setSignalStateForTurningMove(state, toLinkId);
	}

	@Override
	public void setSignalized(boolean isSignalized) {
		((SignalizeableItem) qlane).setSignalized(isSignalized);
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
			double nodeOffset = QLinkImpl.this.network.simEngine.getMobsim().getScenario().getConfig().qsim().getNodeOffset(); 
			if (nodeOffset != 0.0) {
				nodeOffset = nodeOffset +2.0; // +2.0: eventually we need a bit space for the signal
				visModelBuilder = new VisLaneModelBuilder();
				CoordinateTransformation transformation = new IdentityTransformation();
				visLink = visModelBuilder.createVisLinkLanes(transformation, QLinkImpl.this, nodeOffset, null);
				SnapshotLinkWidthCalculator linkWidthCalculator = QLinkImpl.this.network.getLinkWidthCalculatorForVis();
				visModelBuilder.recalculatePositions(visLink, linkWidthCalculator);
			}
		}

		@Override
		public Collection<AgentSnapshotInfo> addAgentSnapshotInfo( Collection<AgentSnapshotInfo> positions) {
			AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder = QLinkImpl.this.network.simEngine.getAgentSnapshotInfoBuilder();

			VisData roadVisData = qlane.getVisData() ;
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
			cnt2 = snapshotInfoBuilder.positionVehiclesFromWaitingList(positions, QLinkImpl.this.link, cnt2,
					QLinkImpl.this.waitingList);

			cnt2 = snapshotInfoBuilder.positionAgentsInActivities(positions, QLinkImpl.this.link,
					QLinkImpl.this.getAdditionalAgentsOnLink(), cnt2);

			return positions;
		}

	}

	@Override
	List<QLaneI> getToNodeQueueLanes() {
		List<QLaneI> list = new ArrayList<>() ;
		list.add( this.qlane ) ;
		return list ;
	}

}
