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

package org.matsim.core.mobsim.queuesim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.basic.network.BasicLane;
import org.matsim.core.basic.signalsystems.BasicSignalGroupDefinition;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueLane.AgentOnLink;
import org.matsim.core.utils.misc.Time;
import org.matsim.signalsystems.CalculateAngle;
import org.matsim.vis.snapshots.writers.PositionInfo;

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
 *
 * Queue Model Link implementation with the following properties:
 * <ul>
 *   <li>The queue behavior itself is simulated by one or more instances of QueueLane</li>
 *   <li>Each QueueLink has at least one QueueLane. All QueueVehicles added to the QueueLink
 *   are placed on this always existing instance.
 *    </li>
 *   <li>All QueueLane instances which are connected to the ToQueueNode are 
 *   held in the attribute toNodeQueueLanes</li>
 *   <li>QueueLink is active as long as at least one 
 * of its QueueLanes is active.</li>
 * </ul>
 */
public class QueueLink {

	final private static Logger log = Logger.getLogger(QueueLink.class);

	final private static QueueLane.FromLinkEndComparator fromLinkEndComparator = new QueueLane.FromLinkEndComparator();
	
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

	/** set to <code>true</code> if there is more than one lane (the originalLane). */
	private boolean hasLanes = false;

	/**
	 * If more than one QueueLane exists this list holds all QueueLanes connected to 
	 * the (To)QueueNode of the QueueLink
	 */
	private List<QueueLane> toNodeQueueLanes = null;
	
	private boolean active = false;

	private final Map<Id, QueueVehicle> parkedVehicles = new LinkedHashMap<Id, QueueVehicle>(10);

	/*package*/ VisData visdata = this.new VisDataImpl();
	
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
		this.hasLanes = true;
		//TODO dg remove this assumption/warning
		if (this.getLink().getLength() < 60.0){
			log.warn("Link " + this.getLink().getId() + " is signalized by traffic light, but its length is less than 60m. This is not recommended." +
				"Recommended minimum lane length is 45m for the signal lane and at least additional 15m space to store 2 vehicles at the original link.");
		}
		boolean firstNodeLinkInitialized = false;
		
		for (BasicLane signalLane : lanes) {
			QueueLane lane = null;
			if(!firstNodeLinkInitialized){
				lane = new QueueLane(this, signalLane, false);
				this.originalLane.addToLane(lane);

				this.setToLinks(lane, signalLane.getToLinkIds());

				lane.recalculateProperties(0.0, signalLane.getLength(), signalLane.getNumberOfRepresentedLanes());
				this.originalLane.recalculateProperties(signalLane.getLength(), this.getLink().getLength() - signalLane.getLength(),
						this.getLink().getNumberOfLanes(Time.UNDEFINED_TIME));
				firstNodeLinkInitialized = true;
				this.originalLane.setFireLaneEvents(true);
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
			}
			lane.setFireLaneEvents(true);
			this.queueLanes.add(lane);
		}
		findLayout();
		addUTurn();
		resortQueueLanes();
	}

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
			QueueLink link = this.getQueueNetwork().getQueueLink(linkId);
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
					lane.setVisualizerLane(laneNumber);
					break;
				}
				laneNumber++;
			}
		}
	}

	private void addUTurn() {
		for (Link outLink : this.getLink().getToNode().getOutLinks().values()) {
			if ((outLink.getToNode().equals(this.getLink().getFromNode()))) {
				QueueLane tempPseudoLink = null;
				for (QueueLane pseudoLink : this.queueLanes) {
					if ((tempPseudoLink == null) ||
							((pseudoLink.getVisualizerLane() == 1) && (pseudoLink.getMeterFromLinkEnd() == 0))) {
						tempPseudoLink = pseudoLink;
						tempPseudoLink.addDestinationLink(outLink);
						this.originalLane.addDestinationLink(outLink);
					}
				}
			}
		}
	}

	private void resortQueueLanes() {
		this.toNodeQueueLanes = new ArrayList<QueueLane>();
		for (QueueLane pseudoLink : this.queueLanes) {
			if (pseudoLink.getMeterFromLinkEnd() == 0.0) {
				this.toNodeQueueLanes.add(pseudoLink);
			}
		}
		Collections.sort(this.queueLanes, QueueLink.fromLinkEndComparator);
	}

	/** Is called after link has been read completely */
	public void finishInit() {
		for (QueueLane lane : this.queueLanes) {
			lane.finishInit();
		}
		this.active = false;
	}

	public void activateLink() {
		if (!this.active) {
			this.getQueueNetwork().addActiveLink(this);
			this.active = true;
		}
	}

	/**
	 * Adds a vehicle to the link, called by
	 * {@link QueueNode#moveVehicleOverNode(QueueVehicle, QueueLane, double)}.
	 *
	 * @param veh
	 *          the vehicle
	 */
	public void add(final QueueVehicle veh) {
		double now = SimulationTimer.getTime();
		activateLink();
		this.originalLane.add(veh, now);
		veh.setCurrentLink(this.getLink());
		QueueSimulation.getEvents().processEvent(
				new LinkEnterEvent(now, veh.getDriver().getPerson(),
						this.getLink()));
	}
	
	/*package*/ void clearVehicles() {
		this.parkedVehicles.clear();
		for (QueueLane lane : this.queueLanes){
			lane.clearVehicles();
		}
	}

	public void addParkedVehicle(QueueVehicle vehicle) {
		this.parkedVehicles.put(vehicle.getId(), vehicle);
		vehicle.setCurrentLink(this.link);
	}

	/*package*/ QueueVehicle getParkedVehicle(Id vehicleId) {
		return this.parkedVehicles.get(vehicleId);
	}

	/*package*/ QueueVehicle removeParkedVehicle(Id vehicleId) {
		return this.parkedVehicles.remove(vehicleId);
	}
	
	/*package*/ void addDepartingVehicle(QueueVehicle vehicle) {
		this.originalLane.waitingList.add(vehicle);
		this.activateLink();
	}

	protected boolean moveLink(double now) {
		boolean ret = false;	
		if (this.hasLanes) { // performance optimization: "if" is faster then "for(queueLanes)" with only one lane
			for (QueueLane lane : this.queueLanes){
				if (lane.moveLane(now)){
					ret = true;
				}
			}
		} else {
			ret = this.originalLane.moveLane(now);
		}
		this.active = ret;
		return ret;
	}

	/*package*/ void processVehicleArrival(final double now, final QueueVehicle veh) {
		QueueSimulation.getEvents().processEvent(
				new AgentArrivalEvent(now, veh.getDriver().getPerson(),
						this.getLink(), veh.getDriver().getCurrentLeg()));
		// Need to inform the veh that it now reached its destination.
		veh.getDriver().legEnds(now);
		addParkedVehicle(veh);
	}


	protected boolean bufferIsEmpty() {
		//if there is only one lane...
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

	/*package*/ boolean hasSpace() {
		return this.originalLane.hasSpace();
	}

	public void recalcTimeVariantAttributes(double time) {
		// TODO dg
		this.originalLane.recalcTimeVariantAttributes(time);
	}

	public QueueVehicle getVehicle(Id vehicleId) {
		QueueVehicle ret = getParkedVehicle(vehicleId);
		if (ret != null) {
			return ret;
		}
		for (QueueLane lane : this.queueLanes){
			ret = lane.getVehicle(vehicleId);
			if (ret != null) {
				return ret;
			}
		}
		return ret;
	}

	public Collection<QueueVehicle> getAllVehicles() {
		Collection<QueueVehicle> ret = new ArrayList<QueueVehicle>(this.parkedVehicles.values());
		for  (QueueLane lane : this.queueLanes){
			ret.addAll(lane.getAllVehicles());
		}
		return ret;
	}

	public double getSpaceCap() {
		return this.originalLane.getSpaceCap();
	}

	public Queue<QueueVehicle> getVehiclesInBuffer() {
		return this.originalLane.getVehiclesInBuffer();
	}

	/**
	 * TODO think about the need for this method
	 * because it is only called by one testcase
	 * @return
	 */
	protected int vehOnLinkCount() {
		return this.originalLane.vehOnLinkCount();
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
		return this.visdata;
	}
	
	/**
	 * Inner class to capsulate visualization methods
	 * @author dgrether
	 *
	 */
	class VisDataImpl implements VisData {

		public double getDisplayableSpaceCapValue() {
			return originalLane.visdata.getDisplayableSpaceCapValue();
		}

		public double getDisplayableTimeCapValue() {
			return originalLane.visdata.getDisplayableTimeCapValue();
		}

		public Collection<AgentOnLink> getDrawableCollection() {
			Collection<PositionInfo> positions = new ArrayList<PositionInfo>();
			getVehiclePositions(positions);

			List<AgentOnLink> vehs = new ArrayList<AgentOnLink>();
			for (PositionInfo pos : positions) {
				if (pos.getVehicleState() == PositionInfo.VehicleState.Driving) {
					AgentOnLink veh = new AgentOnLink();
					veh.posInLink_m = pos.getDistanceOnLink();
					vehs.add(veh);
				}
			}

			return vehs;
		}

		public Collection<PositionInfo> getVehiclePositions(final Collection<PositionInfo> positions) {
			originalLane.visdata.getVehiclePositions(positions);

			int cnt = parkedVehicles.size();
			if (cnt > 0) {
				String snapshotStyle = Gbl.getConfig().simulation().getSnapshotStyle();
				int nLanes = getLink().getLanesAsInt(Time.UNDEFINED_TIME);
				int lane = nLanes + 4;
	
				double cellSize = 7.5;
				double distFromFromNode = getLink().getLength();
				if ("queue".equals(snapshotStyle)) {
					cellSize = Math.min(7.5, getLink().getLength() / cnt);
					distFromFromNode = getLink().getLength() - cellSize / 2.0;
				} else if ("equiDist".equals(snapshotStyle)) {
					cellSize = link.getLength() / cnt;
					distFromFromNode = link.getLength() - cellSize / 2.0;
				} else {
					log.warn("The snapshotStyle \"" + snapshotStyle + "\" is not supported.");
				}
	
				// add the parked vehicles
				for (QueueVehicle veh : parkedVehicles.values()) {
					PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), getLink(),
							distFromFromNode, lane, 0.0, PositionInfo.VehicleState.Parking, null);
					positions.add(position);
					distFromFromNode -= cellSize;
				}
			}
			return positions;
		}

	}
	
}
