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
package org.matsim.core.mobsim.queuesim;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentStuckEvent;
import org.matsim.core.events.AgentWait2LinkEvent;
import org.matsim.core.events.LaneEnterEvent;
import org.matsim.core.events.LaneLeaveEvent;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;
import org.matsim.lanes.basic.BasicLane;
import org.matsim.signalsystems.basic.BasicSignalGroupDefinition;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.vis.netvis.DrawableAgentI;
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
public class QueueLane {

	private static final Logger log = Logger.getLogger(QueueLane.class);

	private static int spaceCapWarningCount = 0;

	private static final Comparator<QueueVehicle> VEHICLE_EXIT_COMPARATOR = new QueueVehicleEarliestLinkExitTimeComparator();

	/**
	 * All vehicles from parkingList move to the waitingList as soon as their time
	 * has come. They are then filled into the vehQueue, depending on free space
	 * in the vehQueue
	 */
	/*package*/ final Queue<QueueVehicle> waitingList = new LinkedList<QueueVehicle>();

	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	/*package*/ final LinkedList<QueueVehicle> vehQueue = new LinkedList<QueueVehicle>();

	/**
	 * A list containing all transit vehicles that are at a stop but not
	 * blocking other traffic on the lane.
	 */
	/*package*/ final Queue<QueueVehicle> transitVehicleStopQueue = new PriorityQueue<QueueVehicle>(5, VEHICLE_EXIT_COMPARATOR);

	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	/*package*/ final Queue<QueueVehicle> buffer = new LinkedList<QueueVehicle>();

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
	/*package*/ double bufferLastMovedTime = Time.UNDEFINED_TIME;

	/*package*/ QueueLink queueLink;
	/**
	 * This collection contains all Lanes downstream, if null it is the last lane
	 * within a QueueLink.
	 */
	private List<QueueLane> toLanes = null;

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
	private final List<Link> destinationLinks = new ArrayList<Link>();

	private SortedMap<Id, BasicSignalGroupDefinition> signalGroups;

	private BasicLane laneData;
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

	/*package*/ QueueLane(final QueueLink ql, final boolean isOriginalLane) {
		this.queueLink = ql;
		this.originalLane = isOriginalLane;
		this.freespeedTravelTime = ql.getLink().getFreespeedTravelTime(Time.UNDEFINED_TIME);
		this.length = ql.getLink().getLength();
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

	protected void addLightSignalGroupDefinition(final BasicSignalGroupDefinition signalGroupDefinition) {
		for (Id laneId : signalGroupDefinition.getLaneIds()) {
			if (this.laneData.getId().equals(laneId)) {
				if (this.signalGroups == null) {
					this.signalGroups = new TreeMap<Id, BasicSignalGroupDefinition>();
				}
				this.signalGroups.put(signalGroupDefinition.getId(), signalGroupDefinition);
			}
		}
	}

	private void calculateFlowCapacity(final double time) {
		this.simulatedFlowCapacity = this.queueLink.getLink().getFlowCapacity(time);
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
		this.simulatedFlowCapacity = this.simulatedFlowCapacity * SimulationTimer.getSimTickTime() * Gbl.getConfig().simulation().getFlowCapFactor();
		this.inverseSimulatedFlowCapacity = 1.0 / this.simulatedFlowCapacity;
		this.flowCapFraction = this.simulatedFlowCapacity - (int) this.simulatedFlowCapacity;
	}


	private void calculateStorageCapacity(final double time) {
		double storageCapFactor = Gbl.getConfig().simulation().getStorageCapFactor();
		this.bufferStorageCapacity = (int) Math.ceil(this.simulatedFlowCapacity);

		double numberOfLanes = this.queueLink.getLink().getNumberOfLanes(time);
		if (this.laneData != null) {
			numberOfLanes = this.laneData.getNumberOfRepresentedLanes();
		}
		// first guess at storageCapacity:
		this.storageCapacity = (this.length * numberOfLanes)
				/ this.queueLink.getQueueNetwork().getNetworkLayer().getEffectiveCellSize() * storageCapFactor;

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
		this.freespeedTravelTime = this.length / this.queueLink.getLink().getFreespeed(Time.UNDEFINED_TIME);
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
		for (BasicSignalGroupDefinition signalGroup : this.signalGroups.values()) {
			this.setThisTimeStepGreen(signalGroup.isGreen(time));
		}
	}

	protected boolean bufferIsEmpty() {
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
			QueueVehicle veh = this.waitingList.poll();
			if (veh == null) {
				return;
			}

			boolean addToBuffer = true;
			if (veh.getDriver() instanceof TransitDriverAgent) {
				TransitDriverAgent driver = (TransitDriverAgent) veh.getDriver();
				TransitStopFacility stop = driver.getNextTransitStop();
				if ((stop != null) && (stop.getLink() == this.queueLink.getLink())) {
				/* ignore delay from handleTransitStop, obviously, the transit
				 * vehicle was parked here, so everybody should have border by now */
					double delay = driver.handleTransitStop(stop, now);
					if (delay > 0.0) {
						veh.setEarliestLinkExitTime(now + delay);
						// add it to the stop queue, can do this as the waitQueue is also non-blocking anyway
						this.transitVehicleStopQueue.add(veh);
						addToBuffer = false;
					}
				}
			}

			if (addToBuffer) {
				addToBuffer(veh, now);
			}
			QueueSimulation.getEvents().processEvent(
					new AgentWait2LinkEvent(now, veh.getDriver().getPerson(), this.queueLink.getLink(), veh.getDriver().getCurrentLeg()));
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
		QueueVehicle veh;

		// handle transit traffic in stop queue
		List<QueueVehicle> departingTransitVehicles = null;
		while ((veh = this.transitVehicleStopQueue.peek()) != null) {
			// there is a transit vehicle.
			if (veh.getEarliestLinkExitTime() > now) {
				break;
			}
			if (departingTransitVehicles == null) {
				departingTransitVehicles = new LinkedList<QueueVehicle>();
			}
			departingTransitVehicles.add(this.transitVehicleStopQueue.poll());
		}
		if (departingTransitVehicles != null) {
			// add all departing transit vehicles at the front of the vehQueue
			ListIterator<QueueVehicle> iter = departingTransitVehicles.listIterator(departingTransitVehicles.size());
			while (iter.hasPrevious()) {
				this.vehQueue.addFirst(iter.previous());
			}
		}

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
			// handle transit driver if necessary
			if (driver instanceof TransitDriverAgent) {
				TransitDriverAgent transitDriver = (TransitDriverAgent) veh.getDriver();
				TransitStopFacility stop = transitDriver.getNextTransitStop();
				if ((stop != null) && (stop.getLink() == this.queueLink.getLink())) {
					double delay = transitDriver.handleTransitStop(stop, now);
					if (delay > 0.0) {
						veh.setEarliestLinkExitTime(now + delay);
						if (!stop.getIsBlockingLane()) {
							this.vehQueue.poll(); // remove the bus from the queue
							this.transitVehicleStopQueue.add(veh); // and add it to the stop queue
						}
					}
					/* start over: either this veh is still first in line,
					 * but has another stop on this link, or on another link, then it is moved on
					 */
					continue;
				}
			}

			// Check if veh has reached destination:
			if ((driver.getDestinationLink() == this.queueLink.getLink()) && (driver.chooseNextLink() == null)) {
				this.queueLink.processVehicleArrival(now, veh);
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
		} // end while
	}

	private boolean isActive() {
		/*
		 * Leave Lane active as long as there are vehicles on the link (ignore
		 * buffer because the buffer gets emptied by nodes and not links) and leave
		 * link active until buffercap has accumulated (so a newly arriving vehicle
		 * is not delayed).
		 */
		boolean active = (this.buffercap_accumulate < 1.0) || (!this.vehQueue.isEmpty()) || (!this.waitingList.isEmpty() || (!this.transitVehicleStopQueue.isEmpty()));
		return active;
	}


	/** called from framework, do everything related to link movement here
	 *
	 * @param now current time step
	 * @return
	 */
	protected boolean moveLane(final double now) {
		updateBufferCapacity();

		// move vehicles from link to buffer
		moveLaneToBuffer(now);

		moveBufferToNextLane(now);

		if (this.originalLane){
			// move vehicles from waitingQueue into buffer if possible
			moveWaitToBuffer(now);
		}
		return this.isActive();
	}

	private void moveBufferToNextLane(final double now) {
		boolean moveOn = true;
		while (moveOn && !this.bufferIsEmpty() && (this.toLanes != null)) {
			QueueVehicle veh = this.buffer.peek();
			Link nextLink = veh.getDriver().chooseNextLink();
			if (nextLink != null) {
				for (QueueLane toQueueLane : this.toLanes) {
					for (Link qLink : toQueueLane.getDestinationLinks()) {
						if (qLink.equals(nextLink)) {
							if (toQueueLane.hasSpace()) {
								this.buffer.poll();
								QueueSimulation.getEvents().processEvent(
										new LaneLeaveEvent(now, veh.getDriver().getPerson(), this.queueLink.getLink(), this.getLaneId()));
								toQueueLane.add(veh, now);
							} else
								moveOn = false;
						}
					}
				}
			}
		}
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
	/*package*/ void add(final QueueVehicle veh, final double now) {
		this.vehQueue.add(veh);
		this.usedStorageCapacity += veh.getSizeInEquivalents();
		if (this.isFireLaneEvents()){
			QueueSimulation.getEvents().processEvent(new LaneEnterEvent(now, veh.getDriver().getPerson(), this.queueLink.getLink(), this.getLaneId()));
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

	private void addToBuffer(final QueueVehicle veh, final double now) {
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

	/*package*/ QueueVehicle popFirstFromBuffer() {
		double now = SimulationTimer.getTime();
		QueueVehicle veh = this.buffer.poll();
		this.bufferLastMovedTime = now; // just in case there is another vehicle in the buffer that is now the new front-most
		if (this.isFireLaneEvents()){
			QueueSimulation.getEvents().processEvent(new LaneLeaveEvent(now, veh.getDriver().getPerson(), this.queueLink.getLink(), this.getLaneId()));
		}
		QueueSimulation.getEvents().processEvent(new LinkLeaveEvent(now, veh.getDriver().getPerson(), this.queueLink.getLink()));

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

	QueueVehicle getFirstFromBuffer() {
		return this.buffer.peek();
	}

	public Queue<QueueVehicle> getVehiclesInBuffer() {
		return this.buffer;
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
		double now = SimulationTimer.getTime();

		for (QueueVehicle veh : this.waitingList) {
			QueueSimulation.getEvents().processEvent(
					new AgentStuckEvent(now, veh.getDriver().getPerson(), veh.getCurrentLink(), veh.getDriver().getCurrentLeg()));
		}
		Simulation.decLiving(this.waitingList.size());
		Simulation.incLost(this.waitingList.size());
		this.waitingList.clear();

		for (QueueVehicle veh : this.vehQueue) {
			QueueSimulation.getEvents().processEvent(
					new AgentStuckEvent(now, veh.getDriver().getPerson(), veh.getCurrentLink(), veh.getDriver().getCurrentLeg()));
		}
		Simulation.decLiving(this.vehQueue.size());
		Simulation.incLost(this.vehQueue.size());
		this.vehQueue.clear();

		for (QueueVehicle veh : this.buffer) {
			QueueSimulation.getEvents().processEvent(
					new AgentStuckEvent(now, veh.getDriver().getPerson(), veh.getCurrentLink(), veh.getDriver().getCurrentLeg()));
		}
		Simulation.decLiving(this.buffer.size());
		Simulation.incLost(this.buffer.size());
		this.buffer.clear();
	}

	// search for vehicleId..
	/*package*/ QueueVehicle getVehicle(final Id id) {
		for (QueueVehicle veh : this.vehQueue) {
			if (veh.getId().equals(id))
				return veh;
		}
		for (QueueVehicle veh : this.buffer) {
			if (veh.getId().equals(id))
				return veh;
		}
		for (QueueVehicle veh : this.waitingList) {
			if (veh.getId().equals(id))
				return veh;
		}
		return null;
	}

	/**
	 * @return Returns a collection of all vehicles (driving, parking, in buffer,
	 *         ...) on the link.
	 */
	public Collection<QueueVehicle> getAllVehicles() {
		Collection<QueueVehicle> vehicles = new ArrayList<QueueVehicle>();

		vehicles.addAll(this.transitVehicleStopQueue);
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
			return (QueueLane.this.buffer.size() + QueueLane.this.vehQueue.size()) / QueueLane.this.storageCapacity;
		}

		/**
		 * Returns a measure for how many vehicles on the link have a travel time
		 * higher than freespeedTraveltime on a scale from 0 to 2. When more then half
		 * of the possible vehicles are delayed, the value 1 will be returned, which
		 * depicts the worst case on a (traditional) scale from 0 to 1.
		 *
		 * @return A measure for the number of vehicles being delayed on this link.
		 */
		public double getDisplayableTimeCapValue() {
			int count = QueueLane.this.buffer.size();
			double now = SimulationTimer.getTime();
			for (QueueVehicle veh : QueueLane.this.vehQueue) {
				// Check if veh has reached destination
				if (veh.getEarliestLinkExitTime() <= now) {
					count++;
				}
			}
			return count * 2.0 / QueueLane.this.storageCapacity;
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

		public Collection<PositionInfo> getVehiclePositions(
				final Collection<PositionInfo> positions) {
			String snapshotStyle = Gbl.getConfig().simulation().getSnapshotStyle();
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
		private void getVehiclePositionsEquil(final Collection<PositionInfo> positions) {
			double time = SimulationTimer.getTime();
			int cnt = QueueLane.this.buffer.size() + QueueLane.this.vehQueue.size();
			int nLanes = QueueLane.this.queueLink.getLink().getLanesAsInt(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
			if (cnt > 0) {
				double cellSize = QueueLane.this.queueLink.getLink().getLength() / cnt;
				double distFromFromNode = QueueLane.this.queueLink.getLink().getLength() - cellSize / 2.0;
				double freespeed = QueueLane.this.queueLink.getLink().getFreespeed(Time.UNDEFINED_TIME);

				// the cars in the buffer
				for (QueueVehicle veh : QueueLane.this.buffer) {
					int lane = 1 + (veh.getId().hashCode() % nLanes);
					int cmp = (int) (veh.getEarliestLinkExitTime() + QueueLane.this.inverseSimulatedFlowCapacity + 2.0);
					double speed = (time > cmp ? 0.0 : freespeed);
					PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), QueueLane.this.queueLink.getLink(),
							distFromFromNode, lane, speed, PositionInfo.VehicleState.Driving, null);
					positions.add(position);
					distFromFromNode -= cellSize;
				}

				// the cars in the drivingQueue
				for (QueueVehicle veh : QueueLane.this.vehQueue) {
					int lane = 1 + (veh.getId().hashCode() % nLanes);
					int cmp = (int) (veh.getEarliestLinkExitTime() + QueueLane.this.inverseSimulatedFlowCapacity + 2.0);
					double speed = (time > cmp ? 0.0 : freespeed);
					PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), QueueLane.this.queueLink.getLink(),
							distFromFromNode, lane, speed, PositionInfo.VehicleState.Driving, null);
					positions.add(position);
					distFromFromNode -= cellSize;
				}
			}

			// the cars in the waitingQueue
			// the actual position doesn't matter, so they're just placed next to the
			// link at the end
			cnt = QueueLane.this.waitingList.size();
			if (cnt > 0) {
				int lane = nLanes + 2;
				double cellSize = Math.min(7.5, QueueLane.this.queueLink.getLink().getLength() / cnt);
				double distFromFromNode = QueueLane.this.queueLink.getLink().getLength() - cellSize / 2.0;
				for (QueueVehicle veh : QueueLane.this.waitingList) {
					PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), QueueLane.this.queueLink.getLink(),
							distFromFromNode, lane, 0.0, PositionInfo.VehicleState.Parking, null);
					positions.add(position);
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
		private void getVehiclePositionsQueue(final Collection<PositionInfo> positions) {
			double now = SimulationTimer.getTime();
			double queueEnd = QueueLane.this.queueLink.getLink().getLength(); // the position of the start of the queue jammed vehicles build at the end of the link
			double storageCapFactor = Gbl.getConfig().simulation().getStorageCapFactor();
			double cellSize = ((NetworkLayer)QueueLane.this.queueLink.getLink().getLayer()).getEffectiveCellSize();
			double vehLen = Math.min( // the length of a vehicle in visualization
					QueueLane.this.queueLink.getLink().getLength() / (QueueLane.this.storageCapacity + QueueLane.this.bufferStorageCapacity), // all vehicles must have place on the link
					cellSize / storageCapFactor); // a vehicle should not be larger than it's actual size

			// put all cars in the buffer one after the other
			for (QueueVehicle veh : QueueLane.this.buffer) {

				int lane = 1 + (veh.getId().hashCode() % QueueLane.this.queueLink.getLink().getLanesAsInt(Time.UNDEFINED_TIME));

				int cmp = (int) (veh.getEarliestLinkExitTime() + QueueLane.this.inverseSimulatedFlowCapacity + 2.0);
				double speed = (now > cmp) ? 0.0 : QueueLane.this.queueLink.getLink().getFreespeed(Time.UNDEFINED_TIME);

				PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), QueueLane.this.queueLink.getLink(), queueEnd,
						lane, speed, PositionInfo.VehicleState.Driving, null);
				positions.add(position);
				queueEnd -= vehLen;
			}

			/*
			 * place other driving cars according the following rule:
			 * - calculate the time how long the vehicle is on the link already
			 * - calculate the position where the vehicle should be if it could drive with freespeed
			 * - if the position is already within the congestion queue, add it to the queue with slow speed
			 * - if the position is not within the queue, just place the car 	with free speed at that place
			 */
			double lastDistance = Integer.MAX_VALUE;
			for (QueueVehicle veh : QueueLane.this.vehQueue) {
				double travelTime = now - veh.getLinkEnterTime();
				double distanceOnLink = (QueueLane.this.queueLink.getLink().getFreespeedTravelTime(now) == 0.0 ? 0.0
						: ((travelTime / QueueLane.this.queueLink.getLink().getFreespeedTravelTime(now)) * QueueLane.this.queueLink.getLink().getLength()));
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
				int cmp = (int) (veh.getEarliestLinkExitTime() + QueueLane.this.inverseSimulatedFlowCapacity + 2.0);
				double speed = (now > cmp) ? 0.0 : QueueLane.this.queueLink.getLink().getFreespeed(now);
				int tmpLane ;
				try {
					tmpLane = Integer.parseInt(veh.getId().toString()) ;
				} catch ( NumberFormatException ee ) {
					tmpLane = veh.getId().hashCode() ;
				}
				int lane = 1 + (tmpLane % QueueLane.this.queueLink.getLink().getLanesAsInt(Time.UNDEFINED_TIME));
				PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), QueueLane.this.queueLink.getLink(), distanceOnLink,
						lane, speed, PositionInfo.VehicleState.Driving, null);
				positions.add(position);
				lastDistance = distanceOnLink;
			}

			/*
			 * Put the vehicles from the waiting list in positions. Their actual
			 * position doesn't matter, so they are just placed to the coordinates of
			 * the from node
			 */
			int lane = QueueLane.this.queueLink.getLink().getLanesAsInt(Time.UNDEFINED_TIME) + 1; // place them next to the link
			for (QueueVehicle veh : QueueLane.this.waitingList) {
				PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), QueueLane.this.queueLink.getLink(),
						/*positionOnLink*/cellSize, lane, 0.0, PositionInfo.VehicleState.Parking, null);
				positions.add(position);
			}

			/*
			 * Put the transit vehicles from the transit stop list in positions.
			 */
			if (QueueLane.this.transitVehicleStopQueue.size() > 0) {
				lane++; // place them one lane further away
				double vehPosition = QueueLane.this.queueLink.getLink().getLength();
				for (QueueVehicle veh : QueueLane.this.transitVehicleStopQueue) {
					PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), QueueLane.this.queueLink.getLink(),
							vehPosition, lane, 0.0, 	PositionInfo.VehicleState.Driving, null);
					positions.add(position);
					vehPosition -= veh.getSizeInEquivalents() * cellSize;
				}
			}

		}

	}

	// //////////////////////////////////////////////////////////
	// For NetStateWriter
	// /////////////////////////////////////////////////////////

	static public class AgentOnLink implements DrawableAgentI {

		public double posInLink_m;

		public double getPosInLink_m() {
			return this.posInLink_m;
		}

		public int getLane() {
			return 1;
		}
	}

	public void addToLane(final QueueLane lane) {
		if (this.toLanes == null) {
			this.toLanes = new LinkedList<QueueLane>();
		}
		this.toLanes.add(lane);
	}

	protected void addDestinationLink(final Link l) {
		this.destinationLinks.add(l);
	}

	protected List<Link> getDestinationLinks(){
		return this.destinationLinks;
	}

	public int getVisualizerLane() {
		return this.visualizerLane;
	}

	protected void setVisualizerLane(final int visualizerLane) {
		this.visualizerLane = visualizerLane;
	}

	public static class FromLinkEndComparator implements Comparator<QueueLane>, Serializable {
		private static final long serialVersionUID = 1L;
		public int compare(final QueueLane o1, final QueueLane o2) {
			if (o1.getMeterFromLinkEnd() < o2.getMeterFromLinkEnd()) {
				return -1;
			} else if (o1.getMeterFromLinkEnd() > o2.getMeterFromLinkEnd()) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	void setLaneData(final BasicLane signalLane) {
		this.laneData = signalLane;
	}
}
