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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.basic.signalsystems.BasicLane;
import org.matsim.basic.signalsystems.BasicSignalGroupDefinition;
import org.matsim.events.LinkEnterEvent;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.signalsystems.CalculateAngle;
import org.matsim.utils.misc.Time;

/**
 * @author dstrippgen
 * @author dgrether
 * @author mrieser
 *
 * A QueueLink can consist of one or more QueueLanes, which may have the following layout
 * (Dashes stand for the borders of the QueueLink, equal signs (=) depict one lane, 
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
 *
 * Queue Model Link implementation with the following properties:
 * <ul>
 *   <li>The queue behavior itself is simulated by one or more instances of QueueLane</li>
 *   <li>Each QueueLink has at least one QueueLane. All QueueVehicles added to the QueueLink
 *   are placed on this always existing instance.
 *    </li>
 *   <li>All QueueLane instances which are connected to the ToQueueNode are 
 *   held in the attribute toNodeQueueLanes</li>
 *   <li>...</li>
 * </ul>
 */
public class QueueLink {

	final private static Logger log = Logger.getLogger(QueueLink.class);
		
	/**
	 * The Link instance containing the data
	 */
	private final Link link;
	/**
	 * Reference to the QueueNetwork instance this link belongs to.
	 */
	private final QueueNetwork queueNetwork;
	/**
	 * Reference to the QueueNode which is at the end of each QueueLink instance
	 */
	private final QueueNode toQueueNode;
	/**
	 * The QueueLane instance which always exists.
	 */
	private QueueLane originalLane;
	/**
	 * A List holding all QueueLane instances of the QueueLink
	 */
	private List<QueueLane> queueLanes;
	/**
	 * If more than one QueueLane exists this list holds all QueueLanes connected to 
	 * the (To)QueueNode of the QueueLink
	 */
	private List<QueueLane> toNodeQueueLanes = null;
	
	private boolean active = false;

	private List<QueueLane> simLanes = new ArrayList<QueueLane>();

	/**
	 * Initializes a QueueLink with one QueueLane.
	 * @param l
	 * @param queueNetwork
	 * @param toNode
	 * @see QueueLink#createLanes(List)
	 */
	public QueueLink(final Link l, final QueueNetwork queueNetwork, final QueueNode toNode) {
		this.link = l;
		this.queueNetwork = queueNetwork;
		this.toQueueNode = toNode;

		this.originalLane = new QueueLane(this, true);
		this.queueLanes = new ArrayList<QueueLane>();
		this.queueLanes.add(this.originalLane);
	}


	/**
	 * Initialize the QueueLink with more than one QueueLane
	 * @param lanes
	 */
	/*package*/ void createLanes(List<BasicLane> lanes) {
		//TODO remove this assumption/warning
		if (this.getLink().getLength() < 60.0){
			log.warn("Link " + this.getLink().getId() + " is signalized by traffic light, but its length is less than 60m. This is not recommended." +
				"Recommended minimum lane length is 45m for the signal lane and at least additional 15m space to store 2 vehicles at the original link.");
		}
		boolean firstNodeLinkInitialized = false;
		
		for (BasicLane signalLane : lanes) {
			QueueLane lane;
			if(!firstNodeLinkInitialized){
				lane = new QueueLane(this, signalLane, false);
				this.originalLane.addToLane(lane);

				this.setToLinks(lane, signalLane.getToLinkIds());

				lane.recalculateProperties(0.0, signalLane.getLength(), signalLane.getNumberOfRepresentedLanes());
				this.originalLane.recalculateProperties(signalLane.getLength(), this.getLink().getLength() - signalLane.getLength(),
						this.getLink().getLanes(Time.UNDEFINED_TIME));
				this.queueLanes.add(lane);
				firstNodeLinkInitialized = true;
			} 
			else {
				// Now we have the original QueueLane and one additional QueueLane
				// therefore add additional extension links for the rest of the outLinks
				// Check, if the new extension link is not in proximity of an old one's staring point
				if (signalLane.getLength() - this.originalLane.getMeterFromLinkEnd() > 15.0) {
					String message = "Not Implemented yet: Every PseudoNode in 15m proximity of an old PseudoNode will be ajected to the old one";
					log.error(message);
					throw new RuntimeException(message);
					// Insert new one...
					// Fix Pointer...
					// Adjust SC, SQ...
					// Generate new NodePseudoLink
					// Fix Pointer...
					// Adjust SC, SQ...
				}
  				// New QueueLane will start at originalLink
					lane = new QueueLane(this, signalLane, false);
					this.originalLane.addToLane(lane);
					this.setToLinks(lane, signalLane.getToLinkIds());
					
					// Only need to fix properties of new QueueLane. Original QueueLane hasn't changed
					lane.recalculateProperties(0.0, signalLane.getLength(), signalLane.getNumberOfRepresentedLanes());
					this.queueLanes.add(lane);
			}
		}
		findLayout();
		addUTurn();
		resortQueueLanes();
	}
	/**
	 * 
	 * @return
	 */
	protected List<QueueLane> getToNodeQueueLanes() {
		if ((this.toNodeQueueLanes == null) && (this.queueLanes.size() == 1)){
			return this.queueLanes;
		}
		return this.toNodeQueueLanes;
	}
	
	protected void addSignalGroupDefinition(BasicSignalGroupDefinition basicSignalGroupDefinition) {
		for (QueueLane lane : this.toNodeQueueLanes) {
			lane.addLightSignalGroupDefinition(basicSignalGroupDefinition);
		}				
	}
	

	/**
	 * Helper setting the Ids of the toLinks for the QueueLane given as parameter.
	 * @param lane
	 * @param toLinkIds
	 */
	private void setToLinks(QueueLane lane, List<Id> toLinkIds) {
		for (Id linkId : toLinkIds) {
			QueueLink link = this.queueNetwork.getQueueLink(linkId);
			if (link == null) {
				String message = "Cannot find Link with Id: " + linkId + " in network. ";
				log.error(message);
				throw new IllegalStateException(message);
			}
			lane.addDestinationLink(link.getLink());
			this.originalLane.addDestinationLink(link.getLink());
		}
	}
	
	private void findLayout(){
		SortedMap<Double, Link> result = CalculateAngle.getOutLinksSortedByAngle(this.getLink());
		for (QueueLane lane : this.queueLanes) {
			int laneNumber = 1;
			for (Link link : result.values()) {
				if (lane.getDestinationLinks().contains(link)){
					lane.setVisualizerLane(laneNumber);;
					break;
				}
				laneNumber++;
			}
		}
	}

	private void addUTurn() {
		for (Link outLink : this.getLink().getToNode().getOutLinks().values()) {
			if((outLink.getToNode().equals(this.getLink().getFromNode()))){
				QueueLane tempPseudoLink = null;
				for (QueueLane pseudoLink : this.queueLanes) {
					if( (tempPseudoLink == null) ||
							((pseudoLink.getVisualizerLane() == 1) && (pseudoLink.getMeterFromLinkEnd() == 0))) {
						tempPseudoLink = pseudoLink;
						tempPseudoLink.addDestinationLink(outLink);
						this.originalLane.addDestinationLink(outLink);
					}
				}
			}
		}
	}

	private void resortQueueLanes(){
		this.toNodeQueueLanes = new ArrayList<QueueLane>();
		for (QueueLane pseudoLink : this.queueLanes) {
			if (pseudoLink.getMeterFromLinkEnd() == 0.0){
				this.toNodeQueueLanes.add(pseudoLink);
			}
		}
		Collections.sort(this.queueLanes);
	}
	
	

	// ////////////////////////////////////////////////////////////////////
	// Is called after link has been read completely
	// ////////////////////////////////////////////////////////////////////
	public void finishInit() {
		for (QueueLane lane : this.queueLanes){
//			this.originalLane.finishInit();
			lane.finishInit();
		}
//		this.buffercap_accumulate = (this.flowCapFraction == 0.0 ? 0.0 : 1.0);
		this.active = false;
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
//		if (!simulateAllLanes){
//			if (!this.originalLane.isActive()) {
//				this.originalLane.activateLane();
		if (!this.active){
			this.queueNetwork.addActiveLink(this);
			this.active = true;
		}
//			}			
//		}
	}

	/**
	 * Adds a vehicle to the link, called by
	 * {@link QueueNode#moveVehicleOverNode(QueueVehicle, double)}.
	 *
	 * @param veh
	 *          the vehicle
	 */
	public void add(final QueueVehicle veh) {
		double now = SimulationTimer.getTime();
		activateLink();
		this.originalLane.add(veh, now);
		veh.getDriver().setCurrentLink(this.getLink());
		QueueSimulation.getEvents().processEvent(
				new LinkEnterEvent(now, veh.getDriver().getPerson(),
						this.getLink(), veh.getCurrentLeg()));
	}
	
	public void clearVehicles() {
		for (QueueLane lane : this.queueLanes){
			lane.clearVehicles();
		}
	}

	
	public void addParking(QueueVehicle vehicle) {
		this.originalLane.addParking(vehicle);
		this.queueNetwork.setLinkActivation(
				vehicle.getDepartureTime_s(), this);
	}

	//TODO dg adapt this to multi lane queues
	protected boolean moveLinkWaitFirst(double time) {
		return this.originalLane.moveLinkWaitFirst(time);
	}

	protected boolean moveLink(double now) {
		boolean ret = false;	
		for (QueueLane lane : this.queueLanes){
			if (lane.moveLane(now)){
				ret = true;
			}
		}
		this.active = ret;
		return ret;
//		return this.originalLane.moveLink(time);
	}

	protected boolean bufferIsEmpty() {
		//TODO dg refactore concept of setThisTimeStepGreen, too complicated -> tooooo much debugging time in case of errors
		for (QueueLane lane : this.queueLanes){
			lane.setThisTimeStepGreen(true);
		}
		//if there is only one link...
		if (this.toNodeQueueLanes == null){
			return this.originalLane.bufferIsEmpty();
		}
		//otherwise we have to do a bit more work
		for (QueueLane lane : this.toNodeQueueLanes){
			if (!lane.bufferIsEmpty()){
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 * @return
	 */
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
		this.simLanes.add(queueLane);
	}
	
	public Link getLink() {
		return this.link;
	}
	
	protected QueueNetwork getQueueNetwork() {
		return this.queueNetwork;
	}

	protected QueueNode getToQueueNode() {
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
	
	public VisData getVisData() {
		return this.originalLane.visdata;
	}

	

	
}
