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
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.Wait2LinkEvent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.signalsystems.mobsim.SignalizeableItem;
import org.matsim.signalsystems.model.SignalGroupState;
import org.matsim.vis.snapshotwriters.VisData;

//Copy and paste from QLinkImpl will be removed when QueueWithBuffer becomes interchangeable in QLinkImpl 
public class BiPedQLinkImpl extends AbstractQLink implements SignalizeableItem {

	// static variables (no problem with memory)
	final static Logger log = Logger.getLogger(BiPedQLinkImpl.class);
	/**
	 * Reference to the QueueNode which is at the end of each QueueLink instance
	 */
	private final QNode toQueueNode;

	private boolean active = false;


	final double length;

	public BiPedQueueWithBuffer road ;
	
	/** 
	 * This constructor allows inserting a custom vehicle queue proper, e.g. to implement passing.
	 * 
	 */
	public BiPedQLinkImpl(final Link link2, QNetwork network, final QNode toNode, VehicleQ<QVehicle> q, double delay) {
		super(link2, network) ;
		this.length = this.getLink().getLength();
		
		this.road = new BiPedQueueWithBuffer(network, this, q, delay);
		this.toQueueNode = toNode;


	}

	/* 
	 *  There are two "active" functionalities (see isActive()).  It probably still works, but it does not look like
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
			this.road.doSimStep(now);
		} else {
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
					new Wait2LinkEvent(now, veh.getDriver().getId(), this.getLink().getId(), veh.getId())
					);

			
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

	



//	public void setRevQB(BiPedQLinkImpl ql) {
//		// TODO Auto-generated method stub
//		
//	}

	//own methods
	/*package*/ BiPedQueueWithBuffer getRoad() {
		return this.road;
	}
	
	
	
	
	
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	//methods below can be removed without warning
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
	@Override
	public VisData getVisData() {
		throw new UnsupportedOperationException();
	}
}
