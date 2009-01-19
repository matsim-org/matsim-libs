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

package org.matsim.mobsim.queuesim;

import java.util.Collection;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.network.Link;

/**
 * @author dstrippgen
 * @author dgrether
 * @author mrieser
 *
 * Queue Model Link implementation
 */
public class QueueLink {

	final private static Logger log = Logger.getLogger(QueueLink.class);

//	private static int spaceCapWarningCount = 0;

	private QueueLane originalLane;
	
//	private VisData visdata = this.new VisDataImpl();

//	private boolean active = false;

	private final Link link;

	/*package*/ final QueueNetwork queueNetwork;

	/*package*/ final QueueNode toQueueNode;

	// ////////////////////////////////////////////////////////////////////
	// constructors
	// ////////////////////////////////////////////////////////////////////
	public QueueLink(final Link l, final QueueNetwork queueNetwork, final QueueNode toNode) {
		this.link = l;
		this.queueNetwork = queueNetwork;
		this.toQueueNode = toNode;

		// yy: I am really not so happy about these indirect constructors with
		// long argument lists. But be it if other people
		// like them. kai, nov06
		/*
		 * moved capacity calculation to two methods, to be able to call it from
		 * outside e.g. for reducing cap in case of an incident
		 */
//		initFlowCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME);
//		recalcCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME);
		this.originalLane = new QueueLane(this);
	}

	public Link getLink() {
		return this.link;
	}

	protected QueueNode getToQueueNode() {
		return this.toQueueNode;
	}

//	public void recalcTimeVariantAttributes(final double now) {
//		initFlowCapacity(now);
//		recalcCapacity(now);
//	}


	// ////////////////////////////////////////////////////////////////////
	// Is called after link has been read completely
	// ////////////////////////////////////////////////////////////////////
	public void finishInit() {
		this.originalLane.finishInit();
//		this.buffercap_accumulate = (this.flowCapFraction == 0.0 ? 0.0 : 1.0);
//		this.active = false;
	}

//	/*package*/ boolean updateActiveStatus() {
//		/*
//		 * Leave link active as long as there are vehicles on the link (ignore
//		 * buffer because the buffer gets emptied by nodes and not links) and leave
//		 * link active until buffercap has accumulated (so a newly arriving vehicle
//		 * is not delayed).
//		 */
//		this.active = (this.buffercap_accumulate < 1.0) || (this.vehQueue.size() != 0) || (this.waitingList.size() != 0);
//		return this.active;
//	}

	public void activateLink() {
		if (!this.originalLane.isActive()) {
			this.originalLane.activateLane();
			this.queueNetwork.addActiveLink(this);
//			this.active = true;
		}
	}

	/**
	 * Adds a vehicle to the link, called by
	 * {@link QueueNode#moveVehicleOverNode(QueueVehicle, double)}.
	 *
	 * @param veh
	 *          the vehicle
	 */
	public void add(final QueueVehicle veh) {
		activateLink();
		veh.getDriver().setCurrentLink(this.link);
		this.originalLane.add(veh);
	}
	
	public void clearVehicles() {
		this.originalLane.clearVehicles();
	}

	
	public void addParking(QueueVehicle vehicle) {
		this.originalLane.addParking(vehicle);
	}

	protected boolean moveLinkWaitFirst(double time) {
		return this.originalLane.moveLinkWaitFirst(time);
	}

	protected boolean moveLink(double time) {
		return this.originalLane.moveLink(time);
	}

	protected boolean bufferIsEmpty() {
		return this.originalLane.bufferIsEmpty();
	}

	public QueueVehicle getFirstFromBuffer() {
		return this.originalLane.getFirstFromBuffer();
	}

	QueueVehicle popFirstFromBuffer() {
		return this.originalLane.popFirstFromBuffer();
	}

	public boolean hasSpace() {
		return this.originalLane.hasSpace();
	}

	public void recalcTimeVariantAttributes(double time) {
		this.originalLane.recalcTimeVariantAttributes(time);
	}


	public QueueVehicle getVehicle(Id agentId) {
		return this.originalLane.getVehicle(agentId);
	}


	public Collection<QueueVehicle> getAllVehicles() {
		return this.originalLane.getAllVehicles();
	}
	

	public double getSpaceCap() {
		return this.originalLane.getSpaceCap();
	}

	public Queue<QueueVehicle> getVehiclesInBuffer() {
		return this.originalLane.getVehiclesInBuffer();
	}
	
	public PriorityQueue<QueueVehicle> getVehiclesOnParkingList() {
		return this.originalLane.getVehiclesOnParkingList();
	}
	

	protected int vehOnLinkCount() {
		return this.originalLane.vehOnLinkCount();
	}


	public void addActiveLane(QueueLane queueLane) {
		// TODO Auto-generated method stub
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
	
	// ////////////////////////////////////////////////////////////////////
	// getter / setter
	// ////////////////////////////////////////////////////////////////////

	public VisData getVisData() {
		return this.originalLane.visdata;
	}



	
}
