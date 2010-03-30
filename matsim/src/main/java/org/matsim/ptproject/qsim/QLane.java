/* *********************************************************************** *
 * project: org.matsim.*
 * QueueLane
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.events.AgentWait2LinkEventImpl;
import org.matsim.core.events.LaneEnterEventImpl;
import org.matsim.core.events.LaneLeaveEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.lanes.Lane;
import org.matsim.pt.qsim.TransitQLaneFeature;
import org.matsim.signalsystems.systems.SignalGroupDefinition;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.PositionInfo;


/**
 * A QueueLane has no own active state and only offers isActive() for a
 * stateless check for activation, a QueueLink is active as long as at least one
 * of its QueueLanes is active.
 *
 *
 * @author dgrether based on prior QueueLink implementations of
 * @author dstrippgen
 * @author aneumann
 * @author mrieser
 */
public class QLane implements QBufferItem {

  private static final Logger log = Logger.getLogger(QLane.class);

	private static int spaceCapWarningCount = 0;

	/**
	 * All vehicles from parkingList move to the waitingList as soon as their time
	 * has come. They are then filled into the vehQueue, depending on free space
	 * in the vehQueue
	 */
	/*package*/ final Queue<QVehicle> waitingList = new LinkedList<QVehicle>();

	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	/*package*/ private final LinkedList<QVehicle> vehQueue = new LinkedList<QVehicle>();

	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	/*package*/ final Queue<QVehicle> buffer = new LinkedList<QVehicle>();

	private double storageCapacity;

	private double usedStorageCapacity;

	/**
	 * The number of vehicles able to leave the buffer in one time step (usually 1s).
	 */
	private double simulatedFlowCapacity; // previously called timeCap

	/*package*/ double inverseSimulatedFlowCapacity; // optimization, cache 1.0 / simulatedFlowCapacity

	private int bufferStorageCapacity; // optimization, cache Math.ceil(simulatedFlowCap)

	private double flowCapFraction; // optimization, cache simulatedFlowCap - (int)simulatedFlowCap

	/**
	 * The (flow) capacity available in one time step to move vehicles into the
	 * buffer. This value is updated each time step by a call to
	 * {@link #updateBufferCapacity(double)}.
	 */
	private double bufferCap = 0.0;

	/**
	 * Stores the accumulated fractional parts of the flow capacity. See also
	 * flowCapFraction.
	 */
	private double buffercap_accumulate = 1.0;

	/** the last timestep the front-most vehicle in the buffer was moved. Used for detecting dead-locks. */
	private double bufferLastMovedTime = Time.UNDEFINED_TIME;

	private final QLink queueLink;
	/**
	 * This collection contains all Lanes downstream, if null it is the last lane
	 * within a QueueLink.
	 */
	private List<QLane> toLanes = null;

	/*package*/ VisData visdata = this.new VisDataImpl();

	/**
	 * This flag indicates whether the QueueLane is
	 * constructed by the original QueueLink of the network (true)
	 * or to represent a Lane configured in a signal system definition.
	 */
	private final boolean originalLane;

	private double length = Double.NaN;

	private double freespeedTravelTime = Double.NaN;

	private double meterFromLinkEnd = Double.NaN;

	private int visualizerLane;

	/**
	 * Contains all Link instances which are reachable from this lane
	 */
	private final Set<Id> destinationLinkIds = new LinkedHashSet<Id>();

	private SortedMap<Id, SignalGroupDefinition> signalGroups;

	private final Lane laneData;
	/**
	 * This id is only set, if there is no laneData for the Lane, i.e. it is the original lane
	 */
	private Id laneId = null;

	private boolean thisTimeStepGreen = true;
	/**
	 * LaneEvents should only be fired if there is more than one QueueLane on a QueueLink
	 * because the LaneEvents are identical with LinkEnter/LeaveEvents otherwise.
	 */
	private boolean fireLaneEvents = false;

	private final TransitQLaneFeature transitQueueLaneFeature;

	/*package*/ QLane(final QLink ql, Lane laneData) {
		this.queueLink = ql;
		this.transitQueueLaneFeature = new TransitQLaneFeature(this.getQueueLink());
		this.originalLane = (laneData == null) ? true : false;
		this.laneData = laneData;
		this.length = ql.getLink().getLength();
		this.freespeedTravelTime = this.length / ql.getLink().getFreespeed();
		this.meterFromLinkEnd = 0.0;

		if (this.originalLane){
			this.laneId = new  IdImpl(this.queueLink.getLink().getId().toString() + ".ol");
		}

		/*
		 * moved capacity calculation to two methods, to be able to call it from
		 * outside e.g. for reducing cap in case of an incident
		 */
		this.calculateCapacities();
	}

	public Id getLaneId(){
		if (this.laneData != null){
			return this.laneData.getId();
		}
		else if (this.originalLane){
			return this.laneId;
		}
		else {
			throw new IllegalStateException("Currently a lane must have a LaneData instance or be the original lane");
		}
	}

	protected void addSignalGroupDefinition(final SignalGroupDefinition signalGroupDefinition) {
		for (Id laneId : signalGroupDefinition.getLaneIds()) {
			if (this.laneData.getId().equals(laneId)) {
				if (this.signalGroups == null) {
					this.signalGroups = new TreeMap<Id, SignalGroupDefinition>();
				}
				this.signalGroups.put(signalGroupDefinition.getId(), signalGroupDefinition);
			}
		}
	}

	private void calculateFlowCapacity(final double time) {
		this.simulatedFlowCapacity = ((LinkImpl)this.queueLink.getLink()).getFlowCapacity(time);
		if (this.laneData != null) {
			/*
			 * Without lanes a Link has a flow capacity that describes the flow on a certain number of
			 * lanes. If lanes are given the following is assumed:
			 *
			 * Flow of a Lane is given by the flow of the link divided by the number of lanes represented by the link.
			 *
			 * A Lane may represent one or more lanes in reality. This is given by the attribute numberOfRepresentedLanes
			 * of the Lane definition. The flow of a lane is scaled by this number.
			 *
			 */
			double queueLinksNumberOfRepresentedLanes = this.queueLink.getLink().getNumberOfLanes(time);
			this.simulatedFlowCapacity = this.simulatedFlowCapacity/queueLinksNumberOfRepresentedLanes
				* this.laneData.getNumberOfRepresentedLanes();
		}
		// we need the flow capcity per sim-tick and multiplied with flowCapFactor
		this.simulatedFlowCapacity = this.simulatedFlowCapacity * QSimTimer.getSimTickTime() * this.getQueueLink().getQSimEngine().getQSim().getScenario().getConfig().getQSimConfigGroup().getFlowCapFactor();
		this.inverseSimulatedFlowCapacity = 1.0 / this.simulatedFlowCapacity;
		this.flowCapFraction = this.simulatedFlowCapacity - (int) this.simulatedFlowCapacity;
	}


	private void calculateStorageCapacity(final double time) {
		double storageCapFactor = this.getQueueLink().getQSimEngine().getQSim().getScenario().getConfig().getQSimConfigGroup().getStorageCapFactor();
		this.bufferStorageCapacity = (int) Math.ceil(this.simulatedFlowCapacity);

		double numberOfLanes = this.queueLink.getLink().getNumberOfLanes(time);
		if (this.laneData != null) {
			numberOfLanes = this.laneData.getNumberOfRepresentedLanes();
		}
		// first guess at storageCapacity:
		this.storageCapacity = (this.length * numberOfLanes)
				/ ((NetworkImpl) ((QLinkLanesImpl)this.queueLink).getQSimEngine().getQSim().getScenario().getNetwork()).getEffectiveCellSize() * storageCapFactor;

		// storage capacity needs to be at least enough to handle the cap_per_time_step:
		this.storageCapacity = Math.max(this.storageCapacity, this.bufferStorageCapacity);

		/*
		 * If speed on link is relatively slow, then we need MORE cells than the
		 * above spaceCap to handle the flowCap. Example: Assume freeSpeedTravelTime
		 * (aka freeTravelDuration) is 2 seconds. Than I need the spaceCap TWO times
		 * the flowCap to handle the flowCap.
		 */
		double tempStorageCapacity = this.freespeedTravelTime * this.simulatedFlowCapacity;
		if (this.storageCapacity < tempStorageCapacity) {
	    if (spaceCapWarningCount <= 10) {
	    	if (!this.originalLane  || (this.toLanes != null)) {
	    		log.warn("Lane " + this.getLaneId() + " on Link " + this.queueLink.getLink().getId() + " too small: enlarge storage capcity from: " + this.storageCapacity + " Vehicles to: " + tempStorageCapacity + " Vehicles.  This is not fatal, but modifies the traffic flow dynamics.");
	    	}
	    	else {
	    		log.warn("Link " + this.queueLink.getLink().getId() + " too small: enlarge storage capcity from: " + this.storageCapacity + " Vehicles to: " + tempStorageCapacity + " Vehicles.  This is not fatal, but modifies the traffic flow dynamics.");
	    	}
        if (spaceCapWarningCount == 10) {
            log.warn("Additional warnings of this type are suppressed.");
        }
        spaceCapWarningCount++;
	    }
	    this.storageCapacity = tempStorageCapacity;
		}
	}


	public void recalcTimeVariantAttributes(final double now) {
		this.freespeedTravelTime = this.length / this.queueLink.getLink().getFreespeed(now);
		calculateFlowCapacity(now);
		calculateStorageCapacity(now);
	}

	void setLaneLength(final double laneLengthMeters) {
		this.length = laneLengthMeters;
		this.freespeedTravelTime = this.length / this.queueLink.getLink().getFreespeed();
	}


	void calculateCapacities() {
		calculateFlowCapacity(Time.UNDEFINED_TIME);
		calculateStorageCapacity(Time.UNDEFINED_TIME);
		this.buffercap_accumulate = (this.flowCapFraction == 0.0 ? 0.0 : 1.0);
	}

	void setMetersFromLinkEnd(final double meters) {
		this.meterFromLinkEnd = meters;
	}

	public double getMeterFromLinkEnd(){
		return this.meterFromLinkEnd;
	}

	/**
	 * updated the status of the QueueLane's signal system
	 */
	protected void updateGreenState(double time){
		if (this.signalGroups == null) {
			log.fatal("This should never happen, since every lane link at a signalized intersection" +
					" should have at least one signal(group). Please check integrity of traffic light data on link " +
					this.queueLink.getLink().getId() + " lane " + this.laneData.getId() + ". Allowing to move anyway.");
			this.setThisTimeStepGreen(true);
			return;
		}
		//else everything normal...
		for (SignalGroupDefinition signalGroup : this.signalGroups.values()) {
			this.setThisTimeStepGreen(signalGroup.isGreen(time));
		}
	}

	public boolean bufferIsEmpty() {
		return this.buffer.isEmpty();
	}

	public boolean isThisTimeStepGreen(){
		return this.thisTimeStepGreen ;
	}

	protected void setThisTimeStepGreen(final boolean b) {
		this.thisTimeStepGreen = b;
	}

	/**
	 * Move as many waiting cars to the link as it is possible
	 *
	 * @param now
	 *          the current time
	 */
	private void moveWaitToBuffer(final double now) {
		while (hasBufferSpace()) {
			QVehicle veh = this.waitingList.poll();
			if (veh == null) {
				return;
			}

			QSim.getEvents().processEvent(
			new AgentWait2LinkEventImpl(now, veh.getDriver().getPerson().getId(), this.queueLink.getLink().getId(), veh.getDriver().getCurrentLeg().getMode()));
			boolean handled = this.transitQueueLaneFeature.handleMoveWaitToBuffer(now, veh);

			if (!handled) {
				addToBuffer(veh, now);
			}
		}
	}

	/**
	 * Move vehicles from link to buffer, according to buffer capacity and
	 * departure time of vehicle. Also removes vehicles from lane if the vehicle
	 * arrived at its destination.
	 *
	 * @param now
	 *          The current time.
	 */
	protected void moveLaneToBuffer(final double now) {
		QVehicle veh;

		this.transitQueueLaneFeature.beforeMoveLaneToBuffer(now);

		// handle regular traffic
		while ((veh = this.vehQueue.peek()) != null) {
			//we have an original QueueLink behaviour
			if ((veh.getEarliestLinkExitTime() > now) && this.originalLane && (this.meterFromLinkEnd == 0.0)){
				return;
			}
			//this is the aneumann PseudoLink behaviour
			else if (Math.floor(veh.getEarliestLinkExitTime()) > now){
				return;
			}

			DriverAgent driver = veh.getDriver();

			boolean handled = this.transitQueueLaneFeature.handleMoveLaneToBuffer(now, veh, driver);

			if (!handled) {
				// Check if veh has reached destination:
				if ((driver.getDestinationLinkId().equals(this.queueLink.getLink().getId())) && (driver.chooseNextLinkId() == null)) {
					driver.legEnds(now);
					this.queueLink.addParkedVehicle(veh);
					// remove _after_ processing the arrival to keep link active
					this.vehQueue.poll();
					this.usedStorageCapacity -= veh.getSizeInEquivalents();
					continue;
				}

				/* is there still room left in the buffer, or is it overcrowded from the
				 * last time steps? */
				if (!hasBufferSpace()) {
					return;
				}

				addToBuffer(veh, now);
				this.vehQueue.poll();
				this.usedStorageCapacity -= veh.getSizeInEquivalents();
			}
		} // end while
	}


	private boolean isActive() {
		/*
		 * Leave Lane active as long as there are vehicles on the link (ignore
		 * buffer because the buffer gets emptied by nodes and not links) and leave
		 * link active until buffercap has accumulated (so a newly arriving vehicle
		 * is not delayed).
		 */
		boolean active = (this.buffercap_accumulate < 1.0) || (!this.vehQueue.isEmpty())
		  || (!this.waitingList.isEmpty()) || (this.toLanes != null && !this.bufferIsEmpty()) || this.transitQueueLaneFeature.isFeatureActive();
		return active;
	}


	/** called from framework, do everything related to link movement here
	 *
	 * @param now current time step
	 * @return
	 */
	protected boolean moveLane(final double now) {
		updateBufferCapacity();

		// move vehicles from lane to buffer.  Includes possible vehicle arrival.  Which, I think, would only be triggered
		// if this is the original lane.
		moveLaneToBuffer(now);

		// move vehicles from buffer to next lane.  This is, if I see this correctly, only relevant if this lane has "to-lanes".
		// A lane can only have to-lanes if it is not an originalLane; in fact, if there are no original lanes at all.
		// Something like
		// if ( this.toLanes != null ) {
		//    moveBufferToNextLane( now ) ;
		// }
		// might be easier to read?  In fact, I think even more could be done in terms of readability.  kai, nov'09
		moveBufferToNextLane(now);

		if (this.originalLane){
			// move vehicles from waitingQueue into buffer if possible
			moveWaitToBuffer(now);
		}
		return this.isActive();
	}

	private void moveBufferToNextLane(final double now) {
		// because of the "this.toLanes != null", this method in my does something only when there are downstream
		// lanes on the same link.  kai, nov'09
		boolean moveOn = true;
		while (moveOn && !this.bufferIsEmpty() && (this.toLanes != null)) {
			QVehicle veh = this.buffer.peek();
			Id nextLinkId = veh.getDriver().chooseNextLinkId();
			QLane toQueueLane = null;
			for (QLane l : this.toLanes) {
				if (l.getDestinationLinkIds().contains(nextLinkId)) {
					toQueueLane = l;
				}
			}
			if (toQueueLane != null) {
				if (toQueueLane.hasSpace()) {
					this.buffer.poll();
					QSim.getEvents().processEvent(
					new LaneLeaveEventImpl(now, veh.getDriver().getPerson().getId(), this.queueLink.getLink().getId(), this.getLaneId()));
					toQueueLane.add(veh, now);
				}
				else {
					moveOn = false;
				}
			}
			else {
				StringBuilder b = new StringBuilder();
				b.append("Person Id: ");
				b.append(veh.getDriver().getPerson().getId());
				b.append(" is on Lane Id ");
				b.append(this.laneId);
				b.append(" on Link Id ");
				b.append(this.queueLink.getLink().getId());
				b.append(" and wants to go on to Link Id ");
				b.append(nextLinkId);
				b.append(" but there is no Lane leading to that Link!");
				log.error(b.toString());
				throw new IllegalStateException(b.toString());
			}
		} // end while
	}

	private void updateBufferCapacity() {
		this.bufferCap = this.simulatedFlowCapacity;
		if (this.thisTimeStepGreen  && (this.buffercap_accumulate < 1.0)) {
			this.buffercap_accumulate += this.flowCapFraction;
		}
	}

	/**
	 * Adds a vehicle to the lane.
	 *
	 * @param veh
	 * @param now the current time
	 */
	/*package*/ void add(final QVehicle veh, final double now) {
		this.vehQueue.add(veh);
		this.usedStorageCapacity += veh.getSizeInEquivalents();
		if (this.isFireLaneEvents()) {
			QSim.getEvents().processEvent(new LaneEnterEventImpl(now, veh.getDriver().getPerson().getId(), this.queueLink.getLink().getId(), this.getLaneId()));
		}
		double departureTime;
		if (this.originalLane) {
			/* It's the original lane,
			 * so we need to start with a 'clean' freeSpeedTravelTime */
			departureTime = (now + this.freespeedTravelTime);
		} else {
			/* It's not the original lane,
			 * so there is a fractional rest we add to this link's freeSpeedTravelTime */
			departureTime = now + this.freespeedTravelTime
			+ veh.getEarliestLinkExitTime() - Math.floor(veh.getEarliestLinkExitTime());
		}
		if (this.meterFromLinkEnd == 0.0) {
			/* It's a QueueLane that is directly connected to a QueueNode,
			 * so we have to floor the freeLinkTravelTime in order the get the same
			 * results compared to the old mobSim */
			departureTime = Math.floor(departureTime);
		}
		if (this.originalLane) {
			veh.setLinkEnterTime(now);
		}
		veh.setEarliestLinkExitTime(departureTime);
	}

	private void addToBuffer(final QVehicle veh, final double now) {
		if (this.bufferCap >= 1.0) {
			this.bufferCap--;
		}
		else if (this.buffercap_accumulate >= 1.0) {
			this.buffercap_accumulate--;
		}
		else {
			throw new IllegalStateException("Buffer of link " + this.queueLink.getLink().getId() + " has no space left!");
		}
		this.buffer.add(veh);
		if (this.buffer.size() == 1) {
			this.bufferLastMovedTime = now;
		}
		this.queueLink.getToQueueNode().activateNode();
	}

	/**
	 * @return <code>true</code> if there are less vehicles in buffer than the flowCapacity's ceil
	 */
	private boolean hasBufferSpace() {
		return ((this.buffer.size() < this.bufferStorageCapacity) && ((this.bufferCap >= 1.0)
				|| (this.buffercap_accumulate >= 1.0)));
	}

	public QVehicle popFirstFromBuffer() {
		double now = QSimTimer.getTime();
		QVehicle veh = this.buffer.poll();
		this.bufferLastMovedTime = now; // just in case there is another vehicle in the buffer that is now the new front-most
		if (this.isFireLaneEvents()) {
			QSim.getEvents().processEvent(new LaneLeaveEventImpl(now, veh.getDriver().getPerson().getId(), this.queueLink.getLink().getId(), this.getLaneId()));
		}
		QSim.getEvents().processEvent(new LinkLeaveEventImpl(now, veh.getDriver().getPerson().getId(), this.queueLink.getLink().getId()));
		return veh;
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
		return this.simulatedFlowCapacity;
	}

	QVehicle getFirstFromBuffer() {
		return this.buffer.peek();
	}

	public Queue<QVehicle> getVehiclesInBuffer() {
		return this.buffer;
	}

	protected boolean isOriginalLane() {
		return this.originalLane;
	}

	/**
	 * @return <code>true</code> if there are less vehicles in buffer + vehQueue (=
	 *         the whole link), than there is space for vehicles.
	 */
	public boolean hasSpace() {
		return this.usedStorageCapacity < getStorageCapacity();
	}

	protected int vehOnLinkCount() {
		return this.vehQueue.size();
	}

	/**
	 * @return Returns the maximum number of vehicles that can be placed on the
	 *         link at a time.
	 */
	/*package*/ double getStorageCapacity() {
		return this.storageCapacity;
	}

	void clearVehicles() {
		double now = QSimTimer.getTime();

		for (QVehicle veh : this.waitingList) {
			QSim.getEvents().processEvent(
					new AgentStuckEventImpl(now, veh.getDriver().getPerson().getId(), veh.getCurrentLink().getId(), veh.getDriver().getCurrentLeg().getMode()));
		}
		Simulation.decLiving(this.waitingList.size());
		Simulation.incLost(this.waitingList.size());
		this.waitingList.clear();

		for (QVehicle veh : this.vehQueue) {
			QSim.getEvents().processEvent(
					new AgentStuckEventImpl(now, veh.getDriver().getPerson().getId(), veh.getCurrentLink().getId(), veh.getDriver().getCurrentLeg().getMode()));
		}
		Simulation.decLiving(this.vehQueue.size());
		Simulation.incLost(this.vehQueue.size());
		this.vehQueue.clear();

		for (QVehicle veh : this.buffer) {
			QSim.getEvents().processEvent(
					new AgentStuckEventImpl(now, veh.getDriver().getPerson().getId(), veh.getCurrentLink().getId(), veh.getDriver().getCurrentLeg().getMode()));
		}
		Simulation.decLiving(this.buffer.size());
		Simulation.incLost(this.buffer.size());
		this.buffer.clear();
	}

	// search for vehicleId..
	/*package*/ QVehicle getVehicle(final Id id) {
		for (QVehicle veh : this.vehQueue) {
			if (veh.getId().equals(id))
				return veh;
		}
		for (QVehicle veh : this.buffer) {
			if (veh.getId().equals(id))
				return veh;
		}
		for (QVehicle veh : this.waitingList) {
			if (veh.getId().equals(id))
				return veh;
		}
		return null;
	}

	/**
	 * @return Returns a collection of all vehicles (driving, parking, in buffer,
	 *         ...) on the link.
	 */
	public Collection<QVehicle> getAllVehicles() {
		Collection<QVehicle> vehicles = new ArrayList<QVehicle>();
		vehicles.addAll(this.transitQueueLaneFeature.getFeatureVehicles());
		vehicles.addAll(this.waitingList);
		vehicles.addAll(this.vehQueue);
		vehicles.addAll(this.buffer);
		return vehicles;
	}



	protected boolean isFireLaneEvents() {
		return this.fireLaneEvents;
	}

	protected void setFireLaneEvents(final boolean fireLaneEvents) {
		this.fireLaneEvents = fireLaneEvents;
	}
	
  protected void addToLane(final QLane lane) {
    if (this.toLanes == null) {
      this.toLanes = new LinkedList<QLane>();
    }
    this.toLanes.add(lane);
  }

  protected void addDestinationLink(final Id linkId) {
    this.destinationLinkIds.add(linkId);
  }

  public Set<Id> getDestinationLinkIds(){
    return this.destinationLinkIds;
  }

  public int getVisualizerLane() {
    return this.visualizerLane;
  }

  protected void setVisualizerLane(final int visualizerLane) {
    this.visualizerLane = visualizerLane;
  }

  public SortedMap<Id, SignalGroupDefinition> getSignalGroups() {
    return this.signalGroups;
  }

  public LinkedList<QVehicle> getVehQueue() {
    return this.vehQueue;
  }

  public QLink getQueueLink() {
    return this.queueLink;
  }

  public double getLength(){
    return this.length;
  }

  @Override
  public double getBufferLastMovedTime() {
    return this.bufferLastMovedTime;
  }

	/**
	 * Inner class to capsulate visualization methods
	 * @author dgrether
	 *
	 */
	class VisDataImpl implements VisData {

		/**
		 * @return The value for coloring the link in NetVis. Actual: veh count / space capacity
		 */
		public double getDisplayableSpaceCapValue() {
			return (QLane.this.buffer.size() + QLane.this.vehQueue.size()) / QLane.this.storageCapacity;
		}

		/**
		 * Returns a measure for how many vehicles on the link have a travel time
		 * higher than freespeedTraveltime on a scale from 0 to 2. When more then half
		 * of the possible vehicles are delayed, the value 1 will be returned, which
		 * depicts the worst case on a (traditional) scale from 0 to 1.
		 *
		 * @return A measure for the number of vehicles being delayed on this link.
		 */
		public double getDisplayableTimeCapValue(double time) {
			int count = QLane.this.buffer.size();
			for (QVehicle veh : QLane.this.vehQueue) {
				// Check if veh has reached destination
				if (veh.getEarliestLinkExitTime() <= time) {
					count++;
				}
			}
			return count * 2.0 / QLane.this.storageCapacity;
		}


		public Collection<AgentSnapshotInfo> getVehiclePositions(final Collection<AgentSnapshotInfo> positions) {
//			log.warn( " entering getVehiclePositions ") ;

			String snapshotStyle = Gbl.getConfig().getQSimConfigGroup().getSnapshotStyle();
			if ("queue".equals(snapshotStyle)) {
				getVehiclePositionsQueue(positions);
			} else if ("equiDist".equals(snapshotStyle)) {
				getVehiclePositionsEquil(positions);
			} else {
				log.warn("The snapshotStyle \"" + snapshotStyle + "\" is not supported.");
			}
			return positions;
		}

		/**
		 * Calculates the positions of all vehicles on this link so that there is
		 * always the same distance between following cars. A single vehicle will be
		 * placed at the middle (0.5) of the link, two cars will be placed at
		 * positions 0.25 and 0.75, three cars at positions 0.16, 0.50, 0.83, and so
		 * on.
		 *
		 * @param positions
		 *          A collection where the calculated positions can be stored.
		 */
		private void getVehiclePositionsEquil(final Collection<AgentSnapshotInfo> positions) {
			double time = QSimTimer.getTime();
			int cnt = QLane.this.buffer.size() + QLane.this.vehQueue.size();
			int nLanes = NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, QLane.this.queueLink.getLink());
			if (cnt > 0) {
				double cellSize = QLane.this.queueLink.getLink().getLength() / cnt;
				double distFromFromNode = QLane.this.queueLink.getLink().getLength() - cellSize / 2.0;
				double freespeed = QLane.this.queueLink.getLink().getFreespeed();

				// the cars in the buffer
				for (QVehicle veh : QLane.this.buffer) {
					int lane = 1 + (veh.getId().hashCode() % nLanes);
					int cmp = (int) (veh.getEarliestLinkExitTime() + QLane.this.inverseSimulatedFlowCapacity + 2.0);
					double speed = (time > cmp ? 0.0 : freespeed);
					Collection<PersonAgentI> peopleInVehicle = getPeopleInVehicle(veh);
					for (PersonAgentI person : peopleInVehicle) {
						PositionInfo position = new PositionInfo(person.getPerson().getId(), QLane.this.queueLink.getLink(),
								distFromFromNode, lane, speed, AgentSnapshotInfo.AgentState.PERSON_DRIVING_CAR);
						positions.add(position);
					}
					distFromFromNode -= cellSize;
				}

				// the cars in the drivingQueue
				for (QVehicle veh : QLane.this.vehQueue) {
					int lane = 1 + (veh.getId().hashCode() % nLanes);
					int cmp = (int) (veh.getEarliestLinkExitTime() + QLane.this.inverseSimulatedFlowCapacity + 2.0);
					double speed = (time > cmp ? 0.0 : freespeed);
					Collection<PersonAgentI> peopleInVehicle = getPeopleInVehicle(veh);
					for (PersonAgentI person : peopleInVehicle) {
						PositionInfo position = new PositionInfo(person.getPerson().getId(), QLane.this.queueLink.getLink(),
								distFromFromNode, lane, speed, AgentSnapshotInfo.AgentState.PERSON_DRIVING_CAR);
						positions.add(position);
					}
					distFromFromNode -= cellSize;
				}
			}

			// the cars in the waitingQueue
			// the actual position doesn't matter, so they're just placed next to the
			// link at the end
			cnt = QLane.this.waitingList.size();
			if (cnt > 0) {
				int lane = nLanes + 2;
				double cellSize = Math.min(7.5, QLane.this.queueLink.getLink().getLength() / cnt);
				double distFromFromNode = QLane.this.queueLink.getLink().getLength() - cellSize / 2.0;
				for (QVehicle veh : QLane.this.waitingList) {
					Collection<PersonAgentI> peopleInVehicle = getPeopleInVehicle(veh);
					for (PersonAgentI person : peopleInVehicle) {
						PositionInfo position = new PositionInfo(person.getPerson().getId(), QLane.this.queueLink.getLink(),
								distFromFromNode, lane, 0.0, AgentSnapshotInfo.AgentState.PERSON_AT_ACTIVITY);
						positions.add(position);
					}
					distFromFromNode -= cellSize;
				}
			}

		}

		/**
		 * Calculates the positions of all vehicles on this link according to the
		 * queue-logic: Vehicles are placed on the link according to the ratio between
		 * the free-travel time and the time the vehicles are already on the link. If
		 * they could have left the link already (based on the time), the vehicles
		 * start to build a traffic-jam (queue) at the end of the link.
		 *
		 * @param positions
		 *          A collection where the calculated positions can be stored.
		 */
		private void getVehiclePositionsQueue(final Collection<AgentSnapshotInfo> positions) {
			double now = QSimTimer.getTime();
			Link link = QLane.this.queueLink.getLink();
			double queueEnd = getInitialQueueEnd();
			double storageCapFactor = Gbl.getConfig().getQSimConfigGroup().getStorageCapFactor();
			double cellSize = ((NetworkImpl)QLane.this.queueLink.getQSimEngine().getQSim().getQNetwork().getNetworkLayer()).getEffectiveCellSize();
			double vehLen = calculateVehicleLength(link, storageCapFactor, cellSize);
			queueEnd = positionVehiclesFromBuffer(positions, now, queueEnd, link, vehLen);
			positionOtherDrivingVehicles(positions, now, queueEnd, link, vehLen);
			int lane = positionVehiclesFromWaitingList(positions, link, cellSize);
			QLane.this.transitQueueLaneFeature.positionVehiclesFromTransitStop(positions, cellSize, lane);
		}

		private double calculateVehicleLength(Link link,
				double storageCapFactor, double cellSize) {
			double vehLen = Math.min( // the length of a vehicle in visualization
					link.getLength() / (QLane.this.storageCapacity + QLane.this.bufferStorageCapacity), // all vehicles must have place on the link
					cellSize / storageCapFactor); // a vehicle should not be larger than it's actual size
			return vehLen;
		}

		private double getInitialQueueEnd() {
			double queueEnd = QLane.this.queueLink.getLink().getLength(); // the position of the start of the queue jammed vehicles build at the end of the link
			if ((QLane.this.signalGroups != null) ){
				queueEnd -= 20.0;
			}
			return queueEnd;
		}

		/**
		 *  put all cars in the buffer one after the other
		 */
		private double positionVehiclesFromBuffer(
				final Collection<AgentSnapshotInfo> positions, double now,
				double queueEnd, Link link, double vehLen) {
			for (QVehicle veh : QLane.this.buffer) {

				int lane = 1 + (veh.getId().hashCode() % NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, QLane.this.queueLink.getLink()));

				int cmp = (int) (veh.getEarliestLinkExitTime() + QLane.this.inverseSimulatedFlowCapacity + 2.0);
				double speed = (now > cmp) ? 0.0 : link.getFreespeed();
				Collection<PersonAgentI> peopleInVehicle = getPeopleInVehicle(veh);
				for (PersonAgentI person : peopleInVehicle) {
					PositionInfo position = new PositionInfo(OTFDefaultLinkHandler.LINK_SCALE, person.getPerson().getId(), link, queueEnd,
							lane, speed, AgentSnapshotInfo.AgentState.PERSON_DRIVING_CAR);
					positions.add(position);
				}
				queueEnd -= vehLen;
			}
			return queueEnd;
		}

		/**
		 * place other driving cars according the following rule:
		 * - calculate the time how long the vehicle is on the link already
		 * - calculate the position where the vehicle should be if it could drive with freespeed
		 * - if the position is already within the congestion queue, add it to the queue with slow speed
		 * - if the position is not within the queue, just place the car 	with free speed at that place
		 */
		private void positionOtherDrivingVehicles(
				final Collection<AgentSnapshotInfo> positions, double now,
				double queueEnd, Link link, double vehLen) {
			double lastDistance = Integer.MAX_VALUE;
			double ttfs = link.getLength() / link.getFreespeed(now);
			for (QVehicle veh : QLane.this.vehQueue) {
				double travelTime = now - veh.getLinkEnterTime();
				double distanceOnLink = (ttfs == 0.0 ? 0.0
						: ((travelTime / ttfs) * link.getLength()));
				if (distanceOnLink > queueEnd) { // vehicle is already in queue
					distanceOnLink = queueEnd;
					queueEnd -= vehLen;
				}
				if (distanceOnLink >= lastDistance) {
					/*
					 * we have a queue, so it should not be possible that one vehicles
					 * overtakes another. additionally, if two vehicles entered at the same
					 * time, they would be drawn on top of each other. we don't allow this,
					 * so in this case we put one after the other. Theoretically, this could
					 * lead to vehicles placed at negative distance when a lot of vehicles
					 * all enter at the same time on an empty link. not sure what to do
					 * about this yet... just setting them to 0 currently.
					 */
					distanceOnLink = lastDistance - vehLen;
					if (distanceOnLink < 0)
						distanceOnLink = 0.0;
				}
				int cmp = (int) (veh.getEarliestLinkExitTime() + QLane.this.inverseSimulatedFlowCapacity + 2.0);
				double speed = (now > cmp) ? 0.0 : link.getFreespeed(now);
				int tmpLane ;
				try {
					tmpLane = Integer.parseInt(veh.getId().toString()) ;
				} catch ( NumberFormatException ee ) {
					tmpLane = veh.getId().hashCode() ;
				}
				int lane = 1 + (tmpLane % NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link));

				Collection<PersonAgentI> peopleInVehicle = getPeopleInVehicle(veh);
				for (PersonAgentI passenger : peopleInVehicle) {
					PositionInfo passengerPosition = new PositionInfo(OTFDefaultLinkHandler.LINK_SCALE, passenger.getPerson().getId(), link, distanceOnLink,
							lane, speed, AgentSnapshotInfo.AgentState.PERSON_DRIVING_CAR);
					positions.add(passengerPosition);
				}

				lastDistance = distanceOnLink;
			}
		}

		private Collection<PersonAgentI> getPeopleInVehicle(QVehicle vehicle) {
			Collection<PersonAgentI> passengers = QLane.this.transitQueueLaneFeature.getPassengers(vehicle);
			if (passengers.isEmpty()) {
				return Collections.singletonList((PersonAgentI) vehicle.getDriver());
			} else {
				ArrayList<PersonAgentI> people = new ArrayList<PersonAgentI>();
				people.add(vehicle.getDriver());
				people.addAll(passengers);
				return people;
			}
		}

		/**
		 * Put the vehicles from the waiting list in positions. Their actual
		 * position doesn't matter, so they are just placed to the coordinates of
		 * the from node
		 */
		private int positionVehiclesFromWaitingList(
				final Collection<AgentSnapshotInfo> positions, Link link,
				double cellSize) {
			int lane = NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link) + 1; // place them next to the link
			for (QVehicle veh : QLane.this.waitingList) {
				Collection<PersonAgentI> peopleInVehicle = getPeopleInVehicle(veh);
				for (PersonAgentI person : peopleInVehicle) {
					PositionInfo position = new PositionInfo(OTFDefaultLinkHandler.LINK_SCALE, person.getPerson().getId(), QLane.this.queueLink.getLink(),
							/*positionOnLink*/cellSize, lane, 0.0, AgentSnapshotInfo.AgentState.PERSON_AT_ACTIVITY);
					positions.add(position);
				}
			}
			return lane;
		}

	};

	public static class FromLinkEndComparator implements Comparator<QLane>, Serializable {
    private static final long serialVersionUID = 1L;
    public int compare(final QLane o1, final QLane o2) {
      if (o1.getMeterFromLinkEnd() < o2.getMeterFromLinkEnd()) {
        return -1;
      } else if (o1.getMeterFromLinkEnd() > o2.getMeterFromLinkEnd()) {
        return 1;
      } else {
        return 0;
      }
    }
  };
	



}
