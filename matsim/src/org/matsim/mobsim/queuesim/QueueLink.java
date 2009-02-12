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
import org.matsim.basic.lightsignalsystems.BasicLane;
import org.matsim.basic.lightsignalsystems.BasicLightSignalGroupDefinition;
import org.matsim.basic.v01.Id;
import org.matsim.events.LinkEnterEvent;
import org.matsim.lightsignalsystems.CalculateAngle;
import org.matsim.network.Link;
import org.matsim.utils.misc.Time;

/**
 * @author dstrippgen
 * @author dgrether
 * @author mrieser
 *
 * Queue Model Link implementation
 */
public class QueueLink {

	final private static Logger log = Logger.getLogger(QueueLink.class);

//	protected static boolean simulateAllLanes = true;
	
	private QueueLane originalLane;
	
	private List<QueueLane> queueLanes;
	
	private final Link link;

	/*package*/ final QueueNetwork queueNetwork;

	/*package*/ final QueueNode toQueueNode;
	
	private List<QueueLane> nodePseudoLinksList;
	
	private boolean active = false;

	private List<QueueLane> simLanes = new ArrayList<QueueLane>();

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
		this.originalLane = new QueueLane(this, true);
		//TODO reconsider this
//		this.originalLane.setOriginalLane(true);
		this.queueLanes = new ArrayList<QueueLane>();
		this.queueLanes.add(this.originalLane);
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

//	public void recalcTimeVariantAttributes(final double now) {
//		initFlowCapacity(now);
//		recalcCapacity(now);
//	}
		/*was once reconfigure*/
	/*package*/ void createLanes(List<BasicLane> lanes) {
//		this.queueLanes = new ArrayList<QueueLane>();
		if (this.getLink().getLength() < 60){
			log.warn("Link " + this.getLink().getId() + " is signalized by traffic light, but its length is less than 60m. This is not recommended." +
				"Recommended minimum link length is 45m for the signal lane and at least additional 15m space to store 2 vehicles at the original link.");
		}
		boolean firstNodeLinkInitialized = false;
//		double averageSimulatedFlowCapacityPerLane_Veh_s = this.getSimulatedFlowCapacity() / this.getLink().getLanes(Time.UNDEFINED_TIME);
		
		for (BasicLane signalLane : lanes) {
//			double freeSpeed_m_s = this.getLink().getFreespeed(Time.UNDEFINED_TIME);
			QueueLane lane;
			if(!firstNodeLinkInitialized){
//				newNodePseudoLink = new PseudoLink(this, false, signalLane.getId());
				lane = new QueueLane(this, signalLane, false);
//				this.originalLink.getToLinks().add(newNodePseudoLink);
				this.originalLane.addToLane(lane);

				this.setToLinks(lane, signalLane.getToLinkIds());

//				int numberOfLanes = signalLane.getNumberOfRepresentedLanes();
//				newNodePseudoLink.recalculatePseudoLinkProperties(0, signalLane.getLength(), numberOfLanes, 
//						freeSpeed_m_s, averageSimulatedFlowCapacityPerLane_Veh_s, this.effectiveCelleSize);
				lane.recalculateProperties(0.0, signalLane.getLength(), signalLane.getNumberOfRepresentedLanes());
//				this.originalLink.recalculatePseudoLinkProperties(signalLane.getLength(), 
//						this.getLink().getLength() - signalLane.getLength(), this.getLink().getLanesAsInt(Time.UNDEFINED_TIME), 
//						this.getLink().getFreespeed(Time.UNDEFINED_TIME), averageSimulatedFlowCapacityPerLane_Veh_s ,this.effectiveCelleSize);
				this.originalLane.recalculateProperties(signalLane.getLength(), this.getLink().getLength() - signalLane.getLength(),
						this.getLink().getLanes(Time.UNDEFINED_TIME));
				this.queueLanes.add(lane);
				firstNodeLinkInitialized = true;
			} 
			else {
				// Now we have the original link and one extension pseudo Link
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
  				// New NodePseudoLink will start at originalLink
//					newNodePseudoLink = new PseudoLink(this, false, signalLane.getId());
					lane = new QueueLane(this, signalLane, false);
					this.originalLane.addToLane(lane);

					this.setToLinks(lane, signalLane.getToLinkIds());
					
					// Only need to fix properties of new link. Original link hasn't changed
//					newNodePseudoLink.recalculatePseudoLinkProperties(0, signalLane.getLength(), numberOfLanes_,
//							freeSpeed_m_s, averageSimulatedFlowCapacityPerLane_Veh_s ,this.effectiveCelleSize);
					lane.recalculateProperties(0.0, signalLane.getLength(), signalLane.getNumberOfRepresentedLanes());
					this.queueLanes.add(lane);
			}
		}
		findLayout();
		addUTurn();
		resortPseudoLinks();
	}
	
	/**
	 * TODO dg remove the nodepseudolinkslist!
	 * Is doing something strange, check it
	 * @return
	 */
	protected List<QueueLane> getNodeQueueLanes() {
		if ((this.nodePseudoLinksList == null) && (this.queueLanes.size() == 1)){
			return this.queueLanes;
		}
		return this.nodePseudoLinksList;
	}
	
	public void addLightSignalGroupDefinition(BasicLightSignalGroupDefinition basicLightSignalGroupDefinition) {
		for (QueueLane lane : this.nodePseudoLinksList) {
			lane.addLightSignalGroupDefinition(basicLightSignalGroupDefinition);
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

	private void resortPseudoLinks(){
		this.nodePseudoLinksList = new ArrayList<QueueLane>();
		for (QueueLane pseudoLink : this.queueLanes) {
			if (pseudoLink.getMeterFromLinkEnd() == 0.0){
				this.nodePseudoLinksList.add(pseudoLink);
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
//		return this.originalLane.bufferIsEmpty();
		//TODO dg don't understand this code
		for (QueueLane lane : this.queueLanes){
			lane.setThisTimeStepGreen(true);
		}
		//if there is only one link...
		if (this.nodePseudoLinksList == null){
			return this.originalLane.bufferIsEmpty();
		}
		
		for (QueueLane lane : this.nodePseudoLinksList){
			if (!lane.bufferIsEmpty()){
				return false;
			}
		}
		return true;
	}


	/**
	 * TODO this method is not really useful anymore -> remove
	 * @return
	 */
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
		this.simLanes.add(queueLane);
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
