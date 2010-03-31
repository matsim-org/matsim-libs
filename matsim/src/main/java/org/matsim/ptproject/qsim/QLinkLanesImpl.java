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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.lanes.Lane;
import org.matsim.signalsystems.CalculateAngle;
import org.matsim.signalsystems.systems.SignalGroupDefinition;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.PositionInfo;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo.AgentState;

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
public class QLinkLanesImpl implements QLink {

	final private static Logger log = Logger.getLogger(QLinkLanesImpl.class);

	final private static QLane.FromLinkEndComparator fromLinkEndComparator = new QLane.FromLinkEndComparator();

	/**
	 * The Link instance containing the data
	 */
	private final Link link;
	/**
	 * Reference to the QueueNode which is at the end of each QueueLink instance
	 */
	private final QNode toQueueNode;
	/**
	 * The QueueLane instance which always exists.
	 */
	private QLane originalLane;
	/**
	 * A List holding all QueueLane instances of the QueueLink
	 */
	private List<QLane> queueLanes;

	/** set to <code>true</code> if there is more than one lane (the originalLane). */
	private boolean hasLanes = false;

	/**
	 * If more than one QueueLane exists this list holds all QueueLanes connected to
	 * the (To)QueueNode of the QueueLink
	 */
	private List<QLane> toNodeQueueLanes = null;

	private boolean active = false;

	private final Map<Id, QVehicle> parkedVehicles = new LinkedHashMap<Id, QVehicle>(10);

	private final Map<Id, PersonAgentI> agentsInActivities = new LinkedHashMap<Id, PersonAgentI>();

	/*package*/ VisData visdata = this.new VisDataImpl();

	private QSimEngine qsimEngine = null;

	/**
	 * Initializes a QueueLink with one QueueLane.
	 * @param link2
	 * @param queueNetwork
	 * @param toNode
	 * @see QLink#createLanes(List)
	 */
	public QLinkLanesImpl(final Link link2, QSimEngine engine, final QNode toNode) {
		this.link = link2;
		this.toQueueNode = toNode;
		this.qsimEngine = engine;
		this.originalLane = new QLane(this, null);
		this.queueLanes = new ArrayList<QLane>();
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
			if (this.originalLane.getLaneId().equals(signalLane.getId())){
			  throw new IllegalStateException("Lane definition has same id as auto generated original lane on link " + this.link.getId());
			}
			
			QLane lane = null;
			lane = new QLane(this, signalLane);
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
		Collections.sort(this.queueLanes, QLinkLanesImpl.fromLinkEndComparator);
		findLayout();
		addUTurn();
	}

	public List<QLane> getToNodeQueueLanes() {
		if ((this.toNodeQueueLanes == null) && (this.queueLanes.size() == 1)){
			return this.queueLanes;
		}
		return this.toNodeQueueLanes;
	}

	protected void addSignalGroupDefinition(SignalGroupDefinition signalGroupDefinition) {
		for (QLane lane : this.toNodeQueueLanes) {
			lane.addSignalGroupDefinition(signalGroupDefinition);
		}
	}


	/**
	 * Helper setting the Ids of the toLinks for the QueueLane given as parameter.
	 * @param lane
	 * @param toLinkIds
	 */
	private void setToLinks(QLane lane, List<Id> toLinkIds) {
		for (Id linkId : toLinkIds) {
			QLink link = this.getQSimEngine().getQSim().getQNetwork().getQueueLink(linkId);
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
		for (QLane lane : this.queueLanes) {
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
				for (QLane l : this.toNodeQueueLanes) {
					if ((l.getVisualizerLane() == 1) && (l.getMeterFromLinkEnd() == 0)){
						l.addDestinationLink(outLink.getId());
						this.originalLane.addDestinationLink(outLink.getId());
					}
				}
			}
		}
	}

	private void initToNodeQueueLanes() {
		this.toNodeQueueLanes = new ArrayList<QLane>();
		for (QLane l : this.queueLanes) {
			if (l.getMeterFromLinkEnd() == 0.0) {
				this.toNodeQueueLanes.add(l);
			}
		}
	}

	public void activateLink() {
		if (!this.active) {
			this.qsimEngine.activateLink(this);
			this.active = true;
		}
	}

	/**
	 * Adds a vehicle to the link, called by
	 * {@link QNode#moveVehicleOverNode(QVehicle, QLane, double)}.
	 *
	 * @param veh
	 *          the vehicle
	 */
	public void addFromIntersection(final QVehicle veh) {
		double now = QSimTimer.getTime();
		activateLink();
		this.originalLane.addToVehicleQueue(veh, now);
		veh.setCurrentLink(this.getLink());
		QSim.getEvents().processEvent(
				new LinkEnterEventImpl(now, veh.getDriver().getPerson().getId(),
						this.getLink().getId()));
	}

	public void clearVehicles() {
		this.parkedVehicles.clear();
		for (QLane lane : this.queueLanes){
			lane.clearVehicles();
		}
	}

	public void addParkedVehicle(QVehicle vehicle) {
		this.parkedVehicles.put(vehicle.getId(), vehicle);
		vehicle.setCurrentLink(this.link);
	}

	/*package*/ QVehicle getParkedVehicle(Id vehicleId) {
		return this.parkedVehicles.get(vehicleId);
	}

	public QVehicle removeParkedVehicle(Id vehicleId) {
		return this.parkedVehicles.remove(vehicleId);
	}

	public void addDepartingVehicle(QVehicle vehicle) {
		this.originalLane.waitingList.add(vehicle);
		this.activateLink();
	}

	public boolean moveLink(double now) {
		boolean ret = false;
		if (this.hasLanes) { // performance optimization: "if" is faster then "for(queueLanes)" with only one lane
			for (QLane lane : this.queueLanes){
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

	public boolean bufferIsEmpty() {
		//if there is only one lane...
		if (this.toNodeQueueLanes == null){
			return this.originalLane.bufferIsEmpty();
		}
		//otherwise we have to do a bit more work
		for (QLane lane : this.toNodeQueueLanes){
			if (!lane.bufferIsEmpty()){
				return false;
			}
		}
		return true;
	}

	public boolean hasSpace() {
		return this.originalLane.hasSpace();
	}

	public void recalcTimeVariantAttributes(double time) {
		for (QLane ql : this.queueLanes){
			ql.recalcTimeVariantAttributes(time);
		}
	}

	public QVehicle getVehicle(Id vehicleId) {
		QVehicle ret = getParkedVehicle(vehicleId);
		if (ret != null) {
			return ret;
		}
		for (QLane lane : this.queueLanes){
			ret = lane.getVehicle(vehicleId);
			if (ret != null) {
				return ret;
			}
		}
		return ret;
	}

	public Collection<QVehicle> getAllVehicles() {
		Collection<QVehicle> ret = new ArrayList<QVehicle>(this.parkedVehicles.values());
		for  (QLane lane : this.queueLanes){
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
			for (QLane ql : this.getToNodeQueueLanes()) {
				total += ql.getStorageCapacity();
			}
		}
		return total;
	}

	/**
	 * One should think about the need for this method
	 * because it is only called by one testcase
	 * @return
	 */
	protected int vehOnLinkCount() {
		int count = 0;
		for (QLane ql : this.queueLanes){
			count += ql.vehOnLinkCount();
		}
		return count;
	}

	public Link getLink() {
		return this.link;
	}

	public QNode getToQueueNode() {
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

	public QSimEngine getQSimEngine(){
    return this.qsimEngine;
  }
	
	/**
	 * @return the QLanes of this QueueLink
	 */
	public List<QLane> getQueueLanes(){
		return this.queueLanes;
	}

	public VisData getVisData() {
		return this.visdata;
	}

	public QLane getOriginalLane(){
		return this.originalLane;
	}

  @Override
  public LinkedList<QVehicle> getVehQueue() {
    LinkedList<QVehicle> ll = this.originalLane.getVehQueue();
    for (QLane l : this.getToNodeQueueLanes()){
      ll.addAll(l.getVehQueue());
    }
    return ll;
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

		public Collection<AgentSnapshotInfo> getVehiclePositions(final Collection<AgentSnapshotInfo> positions) {
		  for (QLane lane : QLinkLanesImpl.this.getQueueLanes()) {
		    lane.visdata.getVehiclePositions(positions);
		  }
		  int cnt = parkedVehicles.size();
		  if (cnt > 0) {
		    int cnt2 = 0 ;
		    Collection<PersonAgentI> agentsInActivities = QLinkLanesImpl.this.agentsInActivities.values();
		    for (PersonAgentI pa : agentsInActivities) {
		      PositionInfo agInfo = new PositionInfo( pa.getPerson().getId(), getLink(), cnt2 ) ;
		      agInfo.setAgentState( AgentState.PERSON_AT_ACTIVITY ) ;
		      positions.add(agInfo) ;
		      cnt2++ ;
		    }
		    // add the parked vehicles
		  }
		  return positions;
		}
	}
	
	@Override
	public void addAgentInActivity(PersonAgentI agent) {
		agentsInActivities.put(agent.getPerson().getId(), agent);
	}

	@Override
	public void removeAgentInActivity(PersonAgentI agent) {
		agentsInActivities.remove(agent.getPerson().getId());
	}


  @Override
  public double getBufferLastMovedTime() {
    throw new UnsupportedOperationException();
  }


  @Override
  public QVehicle popFirstFromBuffer() {
    throw new UnsupportedOperationException();
  }

}
