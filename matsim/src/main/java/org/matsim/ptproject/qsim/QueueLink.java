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

package org.matsim.ptproject.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.misc.Time;
import org.matsim.lanes.Lane;
import org.matsim.signalsystems.CalculateAngle;
import org.matsim.signalsystems.systems.SignalGroupDefinition;
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
 *agent
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
	
	private QueueSimEngine simEngine = null;
	
	/**
	 * Initializes a QueueLink with one QueueLane.
	 * @param link2
	 * @param queueNetwork
	 * @param toNode
	 * @see QueueLink#createLanes(List)
	 */
	public QueueLink(final Link link2, final QueueNetwork queueNetwork, final QueueNode toNode) {
		this.link = link2;
		this.queueNetwork = queueNetwork;
		this.toQueueNode = toNode;

		this.originalLane = new QueueLane(this, null);
		this.queueLanes = new ArrayList<QueueLane>();
		this.queueLanes.add(this.originalLane);
	}


	/**
	 * Initialize the QueueLink with more than one QueueLane
	 * @param map
	 */
	/*package*/ void createLanes(Map<Id, Lane> map) {
		this.hasLanes = true;
		boolean firstNodeLinkInitialized = false;
		
		for (Lane signalLane : map.values()) {
			if (signalLane.getLength() > this.link.getLength()) {
				throw new IllegalStateException("Link Id " + this.link.getId() + " is shorter than Lane Id " + signalLane.getId() + " on this link!");
			}
			QueueLane lane = null;
			lane = new QueueLane(this, signalLane);
			lane.setMetersFromLinkEnd(0.0);
			lane.setLaneLength(signalLane.getLength());
			lane.calculateCapacities();

			this.originalLane.addToLane(lane);
			this.setToLinks(lane, signalLane.getToLinkIds());
			lane.setFireLaneEvents(true);
			this.queueLanes.add(lane);

			if(!firstNodeLinkInitialized){
				this.originalLane.setMetersFromLinkEnd(signalLane.getLength());
				double originalLaneEnd = this.getLink().getLength() - signalLane.getLength();
				this.originalLane.setLaneLength(originalLaneEnd);
				this.originalLane.calculateCapacities();
				firstNodeLinkInitialized = true;
				this.originalLane.setFireLaneEvents(true);
			} 
			else if (signalLane.getLength() != this.originalLane.getMeterFromLinkEnd()){
					String message = "The lanes on link id " + this.getLink().getId() + " have "
						+ "different length. Currently this feature is not supported. To " +
								"avoid this exception set all lanes to the same lenght in lane definition file";
					log.error(message);
					throw new IllegalStateException(message);
			}
		}
		initToNodeQueueLanes();
		Collections.sort(this.queueLanes, QueueLink.fromLinkEndComparator);
		findLayout();
		addUTurn();
	}

	public List<QueueLane> getToNodeQueueLanes() {
		if ((this.toNodeQueueLanes == null) && (this.queueLanes.size() == 1)){
			return this.queueLanes;
		}
		return this.toNodeQueueLanes;
	}
	
	protected void addSignalGroupDefinition(SignalGroupDefinition signalGroupDefinition) {
		for (QueueLane lane : this.toNodeQueueLanes) {
			lane.addSignalGroupDefinition(signalGroupDefinition);
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
			lane.addDestinationLink(link.getLink().getId());
			this.originalLane.addDestinationLink(link.getLink().getId());
		}
	}
	
	private void findLayout(){
		SortedMap<Double, Link> result = CalculateAngle.getOutLinksSortedByAngle(this.getLink());
		for (QueueLane lane : this.queueLanes) {
			int laneNumber = 1;
			for (Link l : result.values()) {
				if (lane.getDestinationLinkIds().contains(l.getId())){
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
				for (QueueLane l : this.toNodeQueueLanes) {
					if ((l.getVisualizerLane() == 1) && (l.getMeterFromLinkEnd() == 0)){
						l.addDestinationLink(outLink.getId());
						this.originalLane.addDestinationLink(outLink.getId());
					}
				}
			}
		}
	}

	private void initToNodeQueueLanes() {
		this.toNodeQueueLanes = new ArrayList<QueueLane>();
		for (QueueLane l : this.queueLanes) {
			if (l.getMeterFromLinkEnd() == 0.0) {
				this.toNodeQueueLanes.add(l);
			}
		}
	}

	/** Is called after link has been read completely */
	public void finishInit() {
		this.active = false;
	}

	public void setSimEngine(final QueueSimEngine simEngine) {
		this.simEngine = simEngine;
	}
	
	public void activateLink() {
		if (!this.active) {
			this.simEngine.activateLink(this);
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
				new LinkEnterEventImpl(now, veh.getDriver().getPerson().getId(),
						this.getLink().getId()));
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
//		QueueSimulation.getEvents().processEvent(
//				new AgentArrivalEventImpl(now, veh.getDriver().getPerson(),
//						this.getLink(), veh.getDriver().getCurrentLeg()));
		// Need to inform the veh that it now reached its destination.
//		veh.getDriver().legEnds(now);
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
		for (QueueLane ql : this.queueLanes){
			ql.recalcTimeVariantAttributes(time);
		}
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

	/**
	 * @return the total space capacity available on that link (includes the space on lanes if available)
	 */
	public double getSpaceCap() {
		double total = this.originalLane.getStorageCapacity();
		if (this.hasLanes) {
			for (QueueLane ql : this.getToNodeQueueLanes()) {
				total += ql.getStorageCapacity();
			}
		}
		return total;
	}

	public Queue<QueueVehicle> getVehiclesInBuffer() {
		return this.originalLane.getVehiclesInBuffer();
	}

	/**
	 * One should think about the need for this method
	 * because it is only called by one testcase
	 * @return
	 */
	protected int vehOnLinkCount() {
		int count = 0;
		for (QueueLane ql : this.queueLanes){
			count += ql.vehOnLinkCount();
		}
		return count;
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
	
	/**
	 * @return the QueueLanes of this QueueLink
	 */
	public List<QueueLane> getQueueLanes(){
		return this.queueLanes;
	}
	
	public VisData getVisData() {
		return this.visdata;
	}
	
	public QueueLane getOriginalLane(){
		return this.originalLane;
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

		public double getDisplayableTimeCapValue(double time) {
			return originalLane.visdata.getDisplayableTimeCapValue(time);
		}

		public Collection<PositionInfo> getVehiclePositions(double time, final Collection<PositionInfo> positions) {
			for (QueueLane lane : QueueLink.this.getQueueLanes()) {
				lane.visdata.getVehiclePositions(time, positions);
			}
//			originalLane.visdata.getVehiclePositions(positions);

			int cnt = parkedVehicles.size();
			if (cnt > 0) {
				String snapshotStyle = Gbl.getConfig().getQSimConfigGroup().getSnapshotStyle();
				int nLanes = Math.round((float)Math.max(getLink().getNumberOfLanes(Time.UNDEFINED_TIME),1.0d));
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
				int cnt2 = 0 ;
				for (QueueVehicle veh : parkedVehicles.values()) {
					PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), getLink(), cnt2 ) ;
//							distFromFromNode, lane, 0.0, PositionInfo.VehicleState.Parking, null);
//					PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), getLink(),
//							distFromFromNode, lane, 0.0, PositionInfo.VehicleState.Parking, null);
					positions.add(position);
					distFromFromNode -= cellSize; cnt2++ ;
				}
			}
			return positions;
		}

	}
	
}
