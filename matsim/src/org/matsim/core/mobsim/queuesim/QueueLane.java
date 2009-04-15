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
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.basic.network.BasicLane;
import org.matsim.core.basic.signalsystems.BasicSignalGroupDefinition;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.AgentStuckEvent;
import org.matsim.core.events.AgentWait2LinkEvent;
import org.matsim.core.events.LaneEnterEvent;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;
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

	/**
	 * parking list includes all vehicles that do not have yet reached their start
	 * time, but will start at this link at some time
	 */
	/*package*/ final PriorityQueue<QueueVehicle> parkingList = new PriorityQueue<QueueVehicle>(
			10, new QueueVehicleDepartureTimeComparator());

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
	/*package*/ final Queue<QueueVehicle> vehQueue = new LinkedList<QueueVehicle>();

	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	/*package*/ final Queue<QueueVehicle> buffer = new LinkedList<QueueVehicle>();

	private double storageCapacity;

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

	private double length_m = Double.NaN;

	private double freespeedTravelTime = Double.NaN;

	private double meterFromLinkEnd = Double.NaN;

	private int visualizerLane;

	/**
	 * Contains all Link instances which are reachable from this lane
	 */
	private final List<Link> destinationLinks = new ArrayList<Link>();

	private SortedMap<Id, BasicSignalGroupDefinition> signalGroups;

	private BasicLane laneData;

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
		this.length_m = ql.getLink().getLength();
		this.meterFromLinkEnd = 0.0;
		/*
		 * moved capacity calculation to two methods, to be able to call it from
		 * outside e.g. for reducing cap in case of an incident
		 */
		initFlowCapacity(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
		recalcCapacity(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
	}


	/**
	 * Call recalculateProperties(...) if you use this constructor, otherwise the QueueLane
	 * is not initialized correctly!
	 * TODO consider unifying the Constructor and the method
	 * @param ql
	 * @param laneData
	 * @param isOriginalLane
	 */
	/*package*/ QueueLane(final QueueLink ql, final BasicLane laneData, final boolean isOriginalLane) {
		this.queueLink = ql;
		this.laneData = laneData;
		this.originalLane = isOriginalLane;
	}

	public Id getLaneId(){
		if (this.laneData != null){
			return this.laneData.getId();
		}
		else if (this.originalLane){
			//TODO dg cache this id somewhere, but where?
			return new IdImpl(this.queueLink.getLink().getId().toString() + ".ol");
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

	private void initFlowCapacity(final double time) {
		// network.capperiod is in hours, we need it per sim-tick and multiplied with flowCapFactor
		double flowCapFactor = Gbl.getConfig().simulation().getFlowCapFactor();

		// multiplying capacity from file by simTickCapFactor **and** flowCapFactor:
		this.simulatedFlowCapacity = this.queueLink.getLink().getFlowCapacity(time)
				* SimulationTimer.getSimTickTime() * flowCapFactor;
	}


	private void recalcCapacity(final double time) {
		// network.capperiod is in hours, we need it per sim-tick and multiplied with flowCapFactor
		double storageCapFactor = Gbl.getConfig().simulation().getStorageCapFactor();

		this.inverseSimulatedFlowCapacity = 1.0 / this.simulatedFlowCapacity;
		this.bufferStorageCapacity = (int) Math.ceil(this.simulatedFlowCapacity);
		this.flowCapFraction = this.simulatedFlowCapacity - (int) this.simulatedFlowCapacity;

		// first guess at storageCapacity:
		this.storageCapacity = (this.length_m * this.queueLink.getLink().getNumberOfLanes(time))
				/ ((NetworkLayer) this.queueLink.getLink().getLayer()).getEffectiveCellSize() * storageCapFactor;

		// storage capacity needs to be at least enough to handle the cap_per_time_step:
		this.storageCapacity = Math.max(this.storageCapacity, this.bufferStorageCapacity);

		/*
		 * If speed on link is relatively slow, then we need MORE cells than the
		 * above spaceCap to handle the flowCap. Example: Assume freeSpeedTravelTime
		 * (aka freeTravelDuration) is 2 seconds. Than I need the spaceCap TWO times
		 * the flowCap to handle the flowCap.
		 */
		if (this.storageCapacity < this.freespeedTravelTime * this.simulatedFlowCapacity) {
			double tempStorageCapacity = this.freespeedTravelTime * this.simulatedFlowCapacity;
	    if (spaceCapWarningCount <= 10) {
	        log.warn("Link " + this.queueLink.getLink().getId() + " too small: enlarge storage capcity from: " + this.storageCapacity + " Vehicles to: " + tempStorageCapacity + " Vehicles.  This is not fatal, but modifies the traffic flow dynamics.");
	        if (spaceCapWarningCount == 10) {
	            log.warn("Additional warnings of this type are suppressed.");
	        }
	        spaceCapWarningCount++;
	    }
	    this.storageCapacity = tempStorageCapacity;
		}
	}


	public void recalcTimeVariantAttributes(final double now) {
		this.freespeedTravelTime = this.length_m / this.queueLink.getLink().getFreespeed(now);
		initFlowCapacity(now);
		recalcCapacity(now);
	}

	/*package*/ void recalculateProperties(final double meterFromLinkEnd_m, final double laneLength_m, final double numberOfRepresentedLanes) {
		SimulationConfigGroup config = Gbl.getConfig().simulation();

		/*variable was given as parameter in original but the method was called everywhere with the expression below,
		 * TODO Check if this is correct! dg[jan09]*/
		double averageSimulatedFlowCapacityPerLane_Veh_s = this.queueLink.getSimulatedFlowCapacity() / this.queueLink.getLink().getNumberOfLanes(Time.UNDEFINED_TIME);

		if(laneLength_m < 15){
			log.warn("Length of one of link " + this.queueLink.getLink().getId() + " sublinks is less than 15m." +
					" Will enlarge length to 15m, since I need at least additional 15m space to store 2 vehicles" +
					" at the original link.");
			this.length_m = 15.0;
		} else {
			this.length_m = laneLength_m;
		}

		this.meterFromLinkEnd  = meterFromLinkEnd_m;
		this.freespeedTravelTime = this.length_m / this.queueLink.getLink().getFreespeed(Time.UNDEFINED_TIME);

		this.simulatedFlowCapacity = numberOfRepresentedLanes * averageSimulatedFlowCapacityPerLane_Veh_s
				* SimulationTimer.getSimTickTime() * config.getFlowCapFactor();

		this.bufferStorageCapacity = (int) Math.ceil(this.simulatedFlowCapacity);
		this.flowCapFraction = this.simulatedFlowCapacity - (int) this.simulatedFlowCapacity;
		this.storageCapacity = (this.length_m * numberOfRepresentedLanes) /
								this.queueLink.getQueueNetwork().getNetworkLayer().getEffectiveCellSize() * config.getStorageCapFactor();
		this.storageCapacity = Math.max(this.storageCapacity, this.bufferStorageCapacity);

		this.buffercap_accumulate = (this.flowCapFraction == 0.0 ? 0.0 : 1.0);

		if (this.storageCapacity < this.freespeedTravelTime * this.simulatedFlowCapacity) {
			this.storageCapacity = this.freespeedTravelTime * this.simulatedFlowCapacity;
		}
	}

	protected double getMeterFromLinkEnd(){
		return this.meterFromLinkEnd;
	}

	// ////////////////////////////////////////////////////////////////////
	// Is called after link has been read completely
	// ////////////////////////////////////////////////////////////////////
	/*package*/ void finishInit() {
		this.buffercap_accumulate = (this.flowCapFraction == 0.0 ? 0.0 : 1.0);
	}

	/**
	 * updated the status of the QueueLane's signal system
	 */
	protected void updateGreenState(){
		if (this.signalGroups == null) {
			log.fatal("This should never happen, since every lane link at a signalized intersection" +
					" should have at least one signal(group). Please check integrity of traffic light data on link " +
					this.queueLink.getLink().getId() + " lane " + this.laneData.getId() + ". Allowing to move anyway.");
			this.setThisTimeStepGreen(true);
			return;
		}
		//else everything normal...
		for (BasicSignalGroupDefinition signalGroup : this.signalGroups.values()) {
			this.setThisTimeStepGreen(signalGroup.isGreen());
		}
	}

	protected boolean bufferIsEmpty() {
		return this.buffer.isEmpty();
	}

	protected boolean isThisTimeStepGreen(){
		return this.thisTimeStepGreen ;
	}

	protected void setThisTimeStepGreen(final boolean b) {
		this.thisTimeStepGreen = b;
	}

	private void processVehicleArrival(final double now, final QueueVehicle veh) {
		QueueSimulation.getEvents().processEvent(
				new AgentArrivalEvent(now, veh.getDriver().getPerson(),
						this.queueLink.getLink(), veh.getDriver().getCurrentLeg()));
		// Need to inform the veh that it now reached its destination.
		veh.getDriver().legEnds(now);
//		veh.getDriver().reachActivity(now, this.queueLink);
	}

	/**
	 * Moves those vehicles, whose departure time has come, from the parking list
	 * to the wait list, from where they can later enter the link.
	 *
	 * @param now
	 *          the current time
	 */
	private void moveParkToWait(final double now) {
		QueueVehicle veh;
		while ((veh = this.parkingList.peek()) != null) {
			if (veh.getDriver().getDepartureTime() > now) {
				return;
			}

			// Need to inform the veh that it now leaves its activity.
			veh.getDriver().leaveActivity(now);

			// Generate departure event
			QueueSimulation.getEvents().processEvent(
					new AgentDepartureEvent(now, veh.getDriver().getPerson(), this.queueLink.getLink(), veh.getDriver().getCurrentLeg()));

			/*
			 * A.) we have an unknown leg mode (aka != "car").
			 *     In this case teleport veh to next activity location
			 * B.) we have no route (aka "next activity on same link") -> no waitingList
			 * C.) route known AND mode == "car" -> regular case, put veh in waitingList
			 */
			Leg leg = veh.getDriver().getCurrentLeg();

			if (!leg.getMode().equals(TransportMode.car)) {
				QueueSimulation.handleUnknownLegMode(veh);
			} else {
				if (((NetworkRoute) leg.getRoute()).getNodes().size() != 0) {
					this.waitingList.add(veh);
				} else {
					// this is the case where (hopefully) the next act happens at the same location as this act
					processVehicleArrival(now, veh);
				}
			}

			/*
			 * Remove vehicle from parkingList Do that as the last step to guarantee
			 * that the link is ACTIVE all the time because veh.reinitVeh() calls
			 * addParking which might come to the false conclusion, that this link
			 * needs to be activated, as parkingQueue is empty
			 */

			this.parkingList.poll();
		}
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
			addToBuffer(veh, now);
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
	private void moveLaneToBuffer(final double now) {
		QueueVehicle veh;
		while ((veh = this.vehQueue.peek()) != null) {
			//we have an original QueueLink behaviour
			if ((veh.getEarliestLinkExitTime() > now) && this.originalLane && (this.meterFromLinkEnd == 0.0)){
				return;
			}
			//this is the aneumann PseudoLink behaviour
			else if (Math.floor(veh.getEarliestLinkExitTime()) > now){
				return;
			}

			// Check if veh has reached destination:
			if (veh.getDriver().getDestinationLink() == this.queueLink.getLink()) {
				processVehicleArrival(now, veh);
				// remove _after_ processing the arrival to keep link active
				this.vehQueue.poll();
				continue;
			}

			/* is there still room left in the buffer, or is it overcrowded from the
			 * last time steps? */
			if (!hasBufferSpace()) {
				return;
			}

			addToBuffer(veh, now);
			this.vehQueue.poll();
		} // end while
	}

	private boolean isActive() {
		/*
		 * Leave Lane active as long as there are vehicles on the link (ignore
		 * buffer because the buffer gets emptied by nodes and not links) and leave
		 * link active until buffercap has accumulated (so a newly arriving vehicle
		 * is not delayed).
		 */
		boolean active = (this.buffercap_accumulate < 1.0) || (this.vehQueue.size() != 0) || (this.waitingList.size() != 0);
		return active;
	}


	/** called from framework, do everything related to link movement here
	 *
	 * @param now current time step
	 * @return
	 */
	protected boolean moveLane(final double now) {
		updateBufferCapacity();

		if (this.originalLane) {
			// move vehicles from parking into waitingQueue if applicable
			moveParkToWait(now);
		}
		// move vehicles from link to buffer
		moveLaneToBuffer(now);

		moveBufferToNextLane(now);

		if (this.originalLane){
			// move vehicles from waitingQueue into buffer if possible
			moveWaitToBuffer(now);
		}
		return this.isActive();
	}

	protected boolean moveLaneWaitFirst(final double now) {
		updateBufferCapacity();

		if (this.originalLane) {
			// move vehicles from parking into waitingQueue if applicable
			moveParkToWait(now);
			// move vehicles from waitingQueue into buffer if possible
			moveWaitToBuffer(now);
		}
		// move vehicles from link to buffer
		moveLaneToBuffer(now);

		moveBufferToNextLane(now);
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

	/*package*/ protected void addParking(final QueueVehicle veh) {
		this.parkingList.add(veh);
	}

	/**
	 * Adds a vehicle to the lane.
	 *
	 * @param veh
	 * @param now the current time
	 */
	/*package*/ void add(final QueueVehicle veh, final double now) {
//		activateLane();
		this.vehQueue.add(veh);
		if (this.isFireLaneEvents()){
			QueueSimulation.getEvents().processEvent(new LaneEnterEvent(now, veh.getDriver().getPerson(), this.queueLink.getLink(), this.getLaneId()));
		}
		double departureTime;
		if (this.originalLane) {
			// It's the original lane,
			// so we need to start with a 'clean' freeSpeedTravelTime
			departureTime = (now + this.freespeedTravelTime);
		}
		else {
			// It's not the original lane,
			// so there is a fractional rest we add to this link's freeSpeedTravelTime
			departureTime = now + this.freespeedTravelTime
			+ veh.getEarliestLinkExitTime() - Math.floor(veh.getEarliestLinkExitTime());
		}
		veh.setEarliestLinkExitTime(departureTime);

		if (this.meterFromLinkEnd == 0.0) {
			// It's a QueueLane that is directly connected to a QueueNode,
			// so we have to floor the freeLinkTravelTime in order the get the same
			// results compared to the old mobSim
			veh.setEarliestLinkExitTime(Math.floor(veh.getEarliestLinkExitTime()));
		}
	}


	private void addToBuffer(final QueueVehicle veh, final double now) {
//		log.debug("addToBuffer: " + now);
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
			QueueSimulation.getEvents().processEvent(new LaneEnterEvent(now, veh.getDriver().getPerson(), this.queueLink.getLink(), this.getLaneId()));
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

	public PriorityQueue<QueueVehicle> getVehiclesOnParkingList() {
		return this.parkingList;
	}


	/**
	 * @return <code>true</code> if there are less vehicles in buffer + vehQueue (=
	 *         the whole link), than there is space for vehicles.
	 */
	public boolean hasSpace() {
		return this.vehQueue.size() < getSpaceCap();
	}

	protected int vehOnLinkCount() {
		return this.vehQueue.size();
	}

	/**
	 * @return Returns the maximum number of vehicles that can be placed on the
	 *         link at a time.
	 */
	public double getSpaceCap() {
		return this.storageCapacity;
	}

	void clearVehicles() {
		double now = SimulationTimer.getTime();

		for (QueueVehicle veh : this.parkingList) {
			QueueSimulation.getEvents().processEvent(
					new AgentStuckEvent(now, veh.getDriver().getPerson(), veh.getCurrentLink(), veh.getDriver().getCurrentLeg()));
		}
		Simulation.decLiving(this.parkingList.size());
		Simulation.incLost(this.parkingList.size());
		this.parkingList.clear();

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
	public QueueVehicle getVehicle(final Id id) {
		for (QueueVehicle veh : this.vehQueue) {
			if (veh.getDriver().getPerson().getId().equals(id))
				return veh;
		}
		for (QueueVehicle veh : this.buffer) {
			if (veh.getDriver().getPerson().getId().equals(id))
				return veh;
		}
		for (QueueVehicle veh : this.parkingList) {
			if (veh.getDriver().getPerson().getId().equals(id))
				return veh;
		}
		for (QueueVehicle veh : this.waitingList) {
			if (veh.getDriver().getPerson().getId().equals(id))
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

		vehicles.addAll(this.waitingList);
		vehicles.addAll(this.parkingList);
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
		public void getVehiclePositionsEquil(final Collection<PositionInfo> positions) {
			double time = SimulationTimer.getTime();
			int cnt = QueueLane.this.buffer.size() + QueueLane.this.vehQueue.size();
			int nLanes = QueueLane.this.queueLink.getLink().getLanesAsInt(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
			if (cnt > 0) {
				double cellSize = QueueLane.this.queueLink.getLink().getLength() / cnt;
				double distFromFromNode = QueueLane.this.queueLink.getLink().getLength() - cellSize / 2.0;
				double freespeed = QueueLane.this.queueLink.getLink().getFreespeed(Time.UNDEFINED_TIME);

				// the cars in the buffer
				for (QueueVehicle veh : QueueLane.this.buffer) {
					int lane = 1 + Integer.parseInt(veh.getId().toString()) % nLanes;
					int cmp = (int) (veh.getEarliestLinkExitTime() + QueueLane.this.inverseSimulatedFlowCapacity + 2.0);
					double speed = (time > cmp ? 0.0 : freespeed);
					PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), QueueLane.this.queueLink.getLink(),
							distFromFromNode, lane, speed, PositionInfo.VehicleState.Driving, null);
					positions.add(position);
					distFromFromNode -= cellSize;
				}

				// the cars in the drivingQueue
				for (QueueVehicle veh : QueueLane.this.vehQueue) {
					int lane = 1 + Integer.parseInt(veh.getId().toString()) % nLanes;
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

			// the cars in the parkingQueue
			// the actual position doesn't matter, so they're distributed next to the
			// link
			cnt = QueueLane.this.parkingList.size();
			if (cnt > 0) {
				int lane = nLanes + 4;
				double cellSize = QueueLane.this.queueLink.getLink().getLength() / cnt;
				double distFromFromNode = QueueLane.this.queueLink.getLink().getLength() - cellSize / 2.0;
				for (QueueVehicle veh : QueueLane.this.parkingList) {
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
		public void getVehiclePositionsQueue(final Collection<PositionInfo> positions) {
			double now = SimulationTimer.getTime();
			double queueEnd = QueueLane.this.queueLink.getLink().getLength(); // the position of the start of the queue jammed vehicles build at the end of the link
			double storageCapFactor = Gbl.getConfig().simulation().getStorageCapFactor();
			double vehLen = Math.min( // the length of a vehicle in visualization
					QueueLane.this.queueLink.getLink().getLength() / (QueueLane.this.storageCapacity + QueueLane.this.bufferStorageCapacity), // all vehicles must have place on the link
					((NetworkLayer)QueueLane.this.queueLink.getLink().getLayer()).getEffectiveCellSize() / storageCapFactor); // a vehicle should not be larger than it's actual size

			// put all cars in the buffer one after the other
			for (QueueVehicle veh : QueueLane.this.buffer) {

				int lane = 1 + (Integer.parseInt(veh.getId().toString()) % QueueLane.this.queueLink.getLink().getLanesAsInt(org.matsim.core.utils.misc.Time.UNDEFINED_TIME));

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
				double travelTime = now - (veh.getEarliestLinkExitTime() - QueueLane.this.queueLink.getLink().getFreespeedTravelTime(now));
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
				int lane = 1 + (Integer.parseInt(veh.getId().toString()) % QueueLane.this.queueLink.getLink().getLanesAsInt(org.matsim.core.utils.misc.Time.UNDEFINED_TIME));
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
			int lane = QueueLane.this.queueLink.getLink().getLanesAsInt(org.matsim.core.utils.misc.Time.UNDEFINED_TIME) + 1; // place them next to the link
			for (QueueVehicle veh : QueueLane.this.waitingList) {
				PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), QueueLane.this.queueLink.getLink(),
						((NetworkLayer) QueueLane.this.queueLink.getLink().getLayer()).getEffectiveCellSize(), lane, 0.0,
						PositionInfo.VehicleState.Parking, null);
				positions.add(position);
			}

			/*
			 * put the vehicles from the parking list in positions their actual position
			 * doesn't matter, so they are just placed to the coordinates of the from
			 * node
			 */
			lane = QueueLane.this.queueLink.getLink().getLanesAsInt(org.matsim.core.utils.misc.Time.UNDEFINED_TIME) + 2; // place them next to the link
			for (QueueVehicle veh : QueueLane.this.parkingList) {
				PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), QueueLane.this.queueLink.getLink(),
						((NetworkLayer) QueueLane.this.queueLink.getLink().getLayer()).getEffectiveCellSize(), lane, 0.0,
						PositionInfo.VehicleState.Parking, null);
				positions.add(position);
			}
		}

	};

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

	protected int getVisualizerLane() {
		return this.visualizerLane;
	}

	protected void setVisualizerLane(final int visualizerLane) {
		this.visualizerLane = visualizerLane;
	}

	public static class FromLinkEndComparator implements Comparator<QueueLane>, Serializable {
		private static final long serialVersionUID = 1L;
		public int compare(QueueLane o1, QueueLane o2) {
			if (o1.getMeterFromLinkEnd() < o2.getMeterFromLinkEnd()) {
				return -1;
			} else if (o1.getMeterFromLinkEnd() > o2.getMeterFromLinkEnd()) {
				return 1;
			} else {
				return 0;
			}
		}
	}
}
