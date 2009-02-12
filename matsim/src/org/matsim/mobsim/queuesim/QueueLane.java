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
package org.matsim.mobsim.queuesim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.lightsignalsystems.BasicLane;
import org.matsim.basic.lightsignalsystems.BasicLightSignalGroupDefinition;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.Id;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.AgentWait2LinkEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Leg;
import org.matsim.population.routes.CarRoute;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.netvis.DrawableAgentI;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;


/**
 * @author dgrether based on prior implementations of
 * @author dstrippgen
 * @author aneumann
 * @author mrieser
 *
 */
public class QueueLane implements Comparable<QueueLane> {
	
	private static final Logger log = Logger.getLogger(QueueLane.class);

	private static int spaceCapWarningCount = 0;

	/**
	 * parking list includes all vehicle that do not have yet reached their start
	 * time, but will start at this link at some time
	 */
	private final PriorityQueue<QueueVehicle> parkingList = new PriorityQueue<QueueVehicle>(
			30, new QueueVehicleDepartureTimeComparator());

	/**
	 * All vehicles from parkingList move to the waitingList as soon as their time
	 * has come. They are then filled into the vehQueue, depending on free space
	 * in the vehQueue
	 */
	private final Queue<QueueVehicle> waitingList = new LinkedList<QueueVehicle>();

	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	private final Queue<QueueVehicle> vehQueue = new LinkedList<QueueVehicle>();

	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	private final Queue<QueueVehicle> buffer = new LinkedList<QueueVehicle>();

	private double storageCapacity;

	/**
	 * The number of vehicles able to leave the buffer in one time step (usually 1s).
	 */
	private double simulatedFlowCapacity; // previously called timeCap

	private double inverseSimulatedFlowCapacity; // optimization, cache 1.0 / simulatedFlowCapacity

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

	private boolean active = false;
	
	private QueueLink queueLink;
	/**
	 * This collection contains all Lanes downstream, if null it is the last lane 
	 * within a QueueLink.
	 */
	private List<QueueLane> toLanes;
	
	/*package*/ VisData visdata = this.new VisDataImpl();

	/**
	 * This flag indicates whether the QueueLane is
	 * constructed by the original QueueLink of the network (true)
	 * or to represent a Lane configured in a signal system definition.
	 */
	private boolean originalLane;
	
	private double length_m = Double.NaN;
	
	private double freespeedTravelTime = Double.NaN;

	private double meterFromLinkEnd = Double.NaN;

//	private double flowCapacityFractionalRest = Double.NaN;
	
	private int visualizerLane;
	
	/**
	 * Contains all Link instances which are reachable from this lane
	 */
	private List<Link> destinationLinks = new ArrayList<Link>();
	
	private SortedMap<Id, BasicLightSignalGroupDefinition> signalGroups;

	private BasicLane laneData;

	private boolean thisTimeStepGreen = false;
	
	/* package */ QueueLane(QueueLink ql, boolean isOriginalLane) {
		this.queueLink = ql;
		this.originalLane = isOriginalLane;
		this.freespeedTravelTime = ql.getLink().getFreespeedTravelTime(Time.UNDEFINED_TIME);
		this.length_m = ql.getLink().getLength();
		this.meterFromLinkEnd = 0.0;
		// yy: I am really not so happy about these indirect constructors with
		// long argument lists. But be it if other people
		// like them. kai, nov06
		/*
		 * moved capacity calculation to two methods, to be able to call it from
		 * outside e.g. for reducing cap in case of an incident
		 */
		initFlowCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME);
		recalcCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME);
	}
	
	
	/**
	 * Call recalculateProperties(...) if you use this constructor, otherwise the QueueLane
	 * is not initialized correctly!
	 * TODO consider unifying the Constructor and the method
	 * @param ql
	 * @param laneData
	 * @param isOriginalLane
	 */
	/*package*/ QueueLane(QueueLink ql, BasicLane laneData, boolean isOriginalLane) {
		this.queueLink = ql;
		this.laneData = laneData;
		this.originalLane = isOriginalLane;
//		this.freespeedTravelTime = ql.getLink().getFreespeedTravelTime(Time.UNDEFINED_TIME);
	}
	
	protected void addLightSignalGroupDefinition(BasicLightSignalGroupDefinition signalGroupDefinition) {
		for (Id laneId : signalGroupDefinition.getLaneIds()) {
			if (this.laneData.getId().equals(laneId)) {
				if (this.signalGroups == null) {
					this.signalGroups = new TreeMap<Id, BasicLightSignalGroupDefinition>();
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
		this.storageCapacity = (this.length_m * this.queueLink.getLink().getLanes(time))
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
	
	/*package*/ void recalculateProperties(double meterFromLinkEnd_m, double lengthOfPseudoLink_m, double numberOfLanes) {
		/*variable was given as parameter in original but the method was called everywhere with the expression below, 
		 * TODO Check if this is correct! dg[jan09]*/
		double averageSimulatedFlowCapacityPerLane_Veh_s = this.queueLink.getSimulatedFlowCapacity() / this.queueLink.getLink().getLanes(Time.UNDEFINED_TIME);
		
		if(lengthOfPseudoLink_m < 15){
			log.warn("Length of one of link " + this.queueLink.getLink().getId() + " sublinks is less than 15m." +
					" Will enlarge length to 15m, since I need at least additional 15m space to store 2 vehicles" +
					" at the original link.");
			this.length_m = 15.0;
		} else {
			this.length_m = lengthOfPseudoLink_m;
		}
		
		this.meterFromLinkEnd  = meterFromLinkEnd_m;
		this.freespeedTravelTime = this.length_m / this.queueLink.getLink().getFreespeed(Time.UNDEFINED_TIME);

		this.simulatedFlowCapacity = numberOfLanes * averageSimulatedFlowCapacityPerLane_Veh_s
				* SimulationTimer.getSimTickTime() * Gbl.getConfig().simulation().getFlowCapFactor();

		this.bufferStorageCapacity = (int) Math.ceil(this.simulatedFlowCapacity);
		this.flowCapFraction = this.simulatedFlowCapacity - (int) this.simulatedFlowCapacity;
		this.storageCapacity = (this.length_m * numberOfLanes) / 
								this.queueLink.getQueueNetwork().getNetworkLayer().getEffectiveCellSize() * Gbl.getConfig().simulation().getStorageCapFactor();
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
		this.active = false;
	}
	
	public boolean canMoveFirstVehicle() {
		if (this.signalGroups == null) {
			log.fatal("This should never happen, since every lane link at a signalized intersection" +
					" should have at least one signal(group). Please check integrity of traffic light data on link " + 
					this.queueLink.getLink().getId() + " lane " + this.laneData.getId() + ". Allowing to move anyway.");
			this.setThisTimeStepGreen(true);
			if (this.getFirstFromBuffer() != null) {
				return true;
			}
			return false;
		}
		//else everything normal...
		boolean signalGroupGreen;
		for (BasicLightSignalGroupDefinition signalGroup : this.signalGroups.values()) {
			signalGroupGreen = signalGroup.isGreen();
			if (signalGroupGreen) {
				this.setThisTimeStepGreen(true);
			}
			QueueVehicle firstVeh = this.getFirstFromBuffer();
			if (firstVeh != null){
				// check if the vehicle's next link is valid according to signal's specification
				if (!(signalGroup.getToLinkIds().contains(firstVeh.getDriver().chooseNextLink().getId()) ||
						firstVeh.getDriver().chooseNextLink().getToNode().equals(this.queueLink.getLink().getFromNode()))) {
					log.error("Person Id: "+ firstVeh.getDriver().getPerson().getId() + " has invalid route according to signal system specification!");
					return false;
				}
				if (signalGroupGreen) {
					return true;
				}
			}
		}
		return false;
	}

	protected boolean bufferIsEmpty() {
		return this.buffer.isEmpty();
	}
	
	protected void setThisTimeStepGreen(boolean b) {
		this.thisTimeStepGreen = b;
	}

	private void processVehicleArrival(final double now, final QueueVehicle veh) {
		QueueSimulation.getEvents().processEvent(
				new AgentArrivalEvent(now, veh.getDriver().getPerson(),
						this.queueLink.getLink(), veh.getCurrentLeg()));
		// Need to inform the veh that it now reached its destination.
		veh.getDriver().reachActivity(now, this.queueLink);
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
			if (veh.getDepartureTime_s() > now) {
				return;
			}

			// Need to inform the veh that it now leaves its activity.
			veh.getDriver().leaveActivity(now);

			// Generate departure event
			QueueSimulation.getEvents().processEvent(
					new AgentDepartureEvent(now, veh.getDriver().getPerson(), this.queueLink.getLink(), veh.getCurrentLeg()));

			/*
			 * A.) we have an unknown leg mode (aka != "car").
			 *     In this case teleport veh to next activity location
			 * B.) we have no route (aka "next activity on same link") -> no waitingList
			 * C.) route known AND mode == "car" -> regular case, put veh in waitingList
			 */
			Leg leg = veh.getCurrentLeg();

			if (!leg.getMode().equals(BasicLeg.Mode.car)) {
				QueueSimulation.handleUnknownLegMode(veh);
			} else {
				if (((CarRoute) leg.getRoute()).getNodes().size() != 0) {
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
					new AgentWait2LinkEvent(now, veh.getDriver().getPerson(), this.queueLink.getLink(), veh.getCurrentLeg()));
		}
	}

	/**
	 * Move vehicles from link to buffer, according to buffer capacity and
	 * departure time of vehicle.
	 *
	 * @param now
	 *          The current time.
	 */
	private void moveLinkToBuffer(final double now) {
		QueueVehicle veh;
		while ((veh = this.vehQueue.peek()) != null) {
			//we have an original QueueLink behaviour
			if ((veh.getDepartureTime_s() > now) && this.originalLane && (this.meterFromLinkEnd == 0.0)){
				return;
			}
			//this is the aneumann PseudoLink behaviour
			else if (Math.floor(veh.getDepartureTime_s()) > now){
				return;
			}
						
			// Check if veh has reached destination:
			if (veh.getDriver().getDestinationLink().getId() == this.queueLink.getLink().getId()) {
				processVehicleArrival(now, veh);
				// remove _after_ processing the arrival to keep link active
				this.vehQueue.poll();
				continue;
			}

			/*
			 * using the following line instead should, I think, be an easy way to
			 * make the mobsim stochastic. not tested. kai
			 */
			// if ( Gbl.random.nextDouble() < this.buffercap_accumulate ) {
			// is there still room left in the buffer, or is it overcrowded from the
			// last time steps?
			if (!hasBufferSpace()) {
				return;
			}

			addToBuffer(veh, now);
			this.vehQueue.poll();
		} // end while
	}
	
	/*package*/ boolean updateActiveStatus() {
		/*
		 * Leave link active as long as there are vehicles on the link (ignore
		 * buffer because the buffer gets emptied by nodes and not links) and leave
		 * link active until buffercap has accumulated (so a newly arriving vehicle
		 * is not delayed).
		 */
		this.active = (this.buffercap_accumulate < 1.0) || (this.vehQueue.size() != 0) || (this.waitingList.size() != 0);
		return this.active;
	}

	/*package*/ void activateLane() {
		if (!this.active) {
			this.queueLink.addActiveLane(this);
			this.active = true;
		}
	}
	
	/*package*/ boolean isActive() {
		return this.active;
	}
	
	
	/** called from framework, do everything related to link movement here
	 * 
	 * @param now current time step
	 * @return 
	 */
	protected boolean moveLane(final double now) {
		if (this.meterFromLinkEnd == 0.0) {
			if (this.originalLane || this.thisTimeStepGreen) 
				updateBufferCapacity();
		}
		else {
			updateBufferCapacity();
		}
		
		this.bufferCap = this.simulatedFlowCapacity;

		
		if (this.originalLane) {
			// move vehicles from parking into waitingQueue if applicable
			moveParkToWait(now);
		}
		// move vehicles from link to buffer
		moveLinkToBuffer(now);
		
		moveBufferToNextLane(now);
		
		if (this.originalLane){
			// move vehicles from waitingQueue into buffer if possible
			moveWaitToBuffer(now);
		}
		this.setThisTimeStepGreen(false);
		return this.updateActiveStatus();
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
	

	/*package*/ boolean moveLinkWaitFirst(final double now) {
		if (!this.originalLane) {
			throw new UnsupportedOperationException("Method not yet implemented for multilane simulation!");
		}
		updateBufferCapacity();
		// move vehicles from parking into waitingQueue if applicable
		moveParkToWait(now);
		// move vehicles from waitingQueue into buffer if possible
		moveWaitToBuffer(now);
		// move vehicles from link to buffer
		moveLinkToBuffer(now);

		return this.updateActiveStatus();
	}
	
	private void updateBufferCapacity() {
//		this.bufferCap = this.simulatedFlowCapacity;
		if (this.buffercap_accumulate < 1.0) {
			this.buffercap_accumulate += this.flowCapFraction;
		}
	}

	/*package*/ protected void addParking(final QueueVehicle veh) {
		this.parkingList.add(veh);
	}
	
	/**
	 * Adds a vehicle to the link, called by
	 * {@link QueueNode#moveVehicleOverNode(QueueVehicle, double)}.
	 *
	 * @param veh
	 *          the vehicle
	 */
	/*package*/ void add(final QueueVehicle veh, double now) {
//		log.debug("add(): " + now);
		activateLane();
		this.vehQueue.add(veh);
//		veh.setDepartureTime_s((int) (now + this.queueLink.getLink().getFreespeedTravelTime(now)));
		double departureTime;
		if (this.originalLane) {
			// It's the original link,
			// so we need to start with a 'clean' freeSpeedTravelTime
			departureTime = (now + this.freespeedTravelTime);
		} 
		else {
			// It's not the original link,
			// so there is a fractional rest we add to this link's freeSpeedTravelTime
			departureTime = now + this.freespeedTravelTime
			+ veh.getDepartureTime_s() - Math.floor(veh.getDepartureTime_s());
//			veh.setDepartureTime_s(now + this.freespeedTravelTime
//					+ veh.getDepartureTime_s() - Math.floor(veh.getDepartureTime_s()));
		}
		veh.setDepartureTime_s(departureTime);
		
		if (this.meterFromLinkEnd == 0.0) {
			// It's a nodePseudoLink,
			// so we have to floor the freeLinkTravelTime in order the get the same
			// results compared to the old mobSim
			veh.setDepartureTime_s(Math.floor(veh.getDepartureTime_s()));
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
		veh.setLastMovedTime(now);
		this.queueLink.toQueueNode.activateNode();
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
		QueueVehicle v2 = this.buffer.peek();
		if (v2 != null) {
			v2.setLastMovedTime(now);
		}

		QueueSimulation.getEvents().processEvent(new LinkLeaveEvent(now, veh.getDriver().getPerson(), this.queueLink.getLink(), veh.getCurrentLeg()));

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
					new AgentStuckEvent(now, veh.getDriver().getPerson(), veh.getCurrentLink(), veh.getCurrentLeg()));
		}
		Simulation.decLiving(this.parkingList.size());
		Simulation.incLost(this.parkingList.size());
		this.parkingList.clear();

		for (QueueVehicle veh : this.waitingList) {
			QueueSimulation.getEvents().processEvent(
					new AgentStuckEvent(now, veh.getDriver().getPerson(), veh.getCurrentLink(), veh.getCurrentLeg()));
		}
		Simulation.decLiving(this.waitingList.size());
		Simulation.incLost(this.waitingList.size());
		this.waitingList.clear();

		for (QueueVehicle veh : this.vehQueue) {
			QueueSimulation.getEvents().processEvent(
					new AgentStuckEvent(now, veh.getDriver().getPerson(), veh.getCurrentLink(), veh.getCurrentLeg()));
		}
		Simulation.decLiving(this.vehQueue.size());
		Simulation.incLost(this.vehQueue.size());
		this.vehQueue.clear();

		for (QueueVehicle veh : this.buffer) {
			QueueSimulation.getEvents().processEvent(
					new AgentStuckEvent(now, veh.getDriver().getPerson(), veh.getCurrentLink(), veh.getCurrentLeg()));
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
	
	/*
	 * The visualization is interested in this status
	 *
	 * It is NOT the same as the boolean "active", because we DO NOT need to wait
	 * for buffer_cap to accumulate AND we DO need to know if there are vehs in
	 * the buffer, which is not important for active state, as they are used by
	 * node only
	 */
	public boolean hasDrivingCars() {
		return ((this.vehQueue.size() + this.waitingList.size() + this.buffer.size()) != 0);
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
			return (buffer.size() + vehQueue.size()) / storageCapacity;
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
			int count = buffer.size();
			double now = SimulationTimer.getTime();
			for (QueueVehicle veh : vehQueue) {
				// Check if veh has reached destination
				if (veh.getDepartureTime_s() <= now) {
					count++;
				}
			}
			return count * 2.0 / storageCapacity;
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
			int cnt = buffer.size() + vehQueue.size();
			int nLanes = queueLink.getLink().getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME);
			if (cnt > 0) {
				double cellSize = queueLink.getLink().getLength() / cnt;
				double distFromFromNode = queueLink.getLink().getLength() - cellSize / 2.0;
				double freespeed = queueLink.getLink().getFreespeed(Time.UNDEFINED_TIME);

				// the cars in the buffer
				for (QueueVehicle veh : buffer) {
					int lane = 1 + Integer.parseInt(veh.getId().toString()) % nLanes;
					int cmp = (int) (veh.getDepartureTime_s() + inverseSimulatedFlowCapacity + 2.0);
					double speed = (time > cmp ? 0.0 : freespeed);
					PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), queueLink.getLink(),
							distFromFromNode, lane, speed, PositionInfo.VehicleState.Driving, null);
					positions.add(position);
					distFromFromNode -= cellSize;
				}

				// the cars in the drivingQueue
				for (QueueVehicle veh : vehQueue) {
					int lane = 1 + Integer.parseInt(veh.getId().toString()) % nLanes;
					int cmp = (int) (veh.getDepartureTime_s() + inverseSimulatedFlowCapacity + 2.0);
					double speed = (time > cmp ? 0.0 : freespeed);
					PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), queueLink.getLink(),
							distFromFromNode, lane, speed, PositionInfo.VehicleState.Driving, null);
					positions.add(position);
					distFromFromNode -= cellSize;
				}
			}

			// the cars in the waitingQueue
			// the actual position doesn't matter, so they're just placed next to the
			// link at the end
			cnt = waitingList.size();
			if (cnt > 0) {
				int lane = nLanes + 2;
				double cellSize = Math.min(7.5, queueLink.getLink().getLength() / cnt);
				double distFromFromNode = queueLink.getLink().getLength() - cellSize / 2.0;
				for (QueueVehicle veh : waitingList) {
					PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), queueLink.getLink(),
							distFromFromNode, lane, 0.0, PositionInfo.VehicleState.Parking, null);
					positions.add(position);
					distFromFromNode -= cellSize;
				}
			}

			// the cars in the parkingQueue
			// the actual position doesn't matter, so they're distributed next to the
			// link
			cnt = parkingList.size();
			if (cnt > 0) {
				int lane = nLanes + 4;
				double cellSize = queueLink.getLink().getLength() / cnt;
				double distFromFromNode = queueLink.getLink().getLength() - cellSize / 2.0;
				for (QueueVehicle veh : parkingList) {
					PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), queueLink.getLink(),
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
			double queueEnd = queueLink.getLink().getLength(); // the position of the start of the queue jammed vehicles build at the end of the link
			double storageCapFactor = Gbl.getConfig().simulation().getStorageCapFactor();
			double vehLen = Math.min( // the length of a vehicle in visualization
					queueLink.getLink().getLength() / (storageCapacity + bufferStorageCapacity), // all vehicles must have place on the link
					((NetworkLayer)queueLink.getLink().getLayer()).getEffectiveCellSize() / storageCapFactor); // a vehicle should not be larger than it's actual size

			// put all cars in the buffer one after the other
			for (QueueVehicle veh : buffer) {

				int lane = 1 + (Integer.parseInt(veh.getId().toString()) % queueLink.getLink().getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME));

				int cmp = (int) (veh.getDepartureTime_s() + inverseSimulatedFlowCapacity + 2.0);
				double speed = (now > cmp) ? 0.0 : queueLink.getLink().getFreespeed(Time.UNDEFINED_TIME);

				PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), queueLink.getLink(), queueEnd,
						lane, speed, PositionInfo.VehicleState.Driving, veh.getDriver().getPerson().getVisualizerData());
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
			for (QueueVehicle veh : vehQueue) {
				double travelTime = now - (veh.getDepartureTime_s() - queueLink.getLink().getFreespeedTravelTime(now));
				double distanceOnLink = (queueLink.getLink().getFreespeedTravelTime(now) == 0.0 ? 0.0
						: ((travelTime / queueLink.getLink().getFreespeedTravelTime(now)) * queueLink.getLink().getLength()));
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
				int cmp = (int) (veh.getDepartureTime_s()
						+ inverseSimulatedFlowCapacity + 2.0);
				double speed = (now > cmp) ? 0.0 : queueLink.getLink().getFreespeed(now);
				int lane = 1 + (Integer.parseInt(veh.getId().toString()) % queueLink.getLink().getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME));
				PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), queueLink.getLink(), distanceOnLink,
						lane, speed, PositionInfo.VehicleState.Driving, veh.getDriver().getPerson().getVisualizerData());
				positions.add(position);
				lastDistance = distanceOnLink;
			}

			/*
			 * Put the vehicles from the waiting list in positions. Their actual
			 * position doesn't matter, so they are just placed to the coordinates of
			 * the from node
			 */
			int lane = queueLink.getLink().getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME) + 1; // place them next to the link
			for (QueueVehicle veh : waitingList) {
				PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), queueLink.getLink(),
						((NetworkLayer) queueLink.getLink().getLayer()).getEffectiveCellSize(), lane, 0.0,
						PositionInfo.VehicleState.Parking, veh.getDriver().getPerson().getVisualizerData());
				positions.add(position);
			}

			/*
			 * put the vehicles from the parking list in positions their actual position
			 * doesn't matter, so they are just placed to the coordinates of the from
			 * node
			 */
			lane = queueLink.getLink().getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME) + 2; // place them next to the link
			for (QueueVehicle veh : parkingList) {
				PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), queueLink.getLink(),
						((NetworkLayer) queueLink.getLink().getLayer()).getEffectiveCellSize(), lane, 0.0,
						PositionInfo.VehicleState.Parking, veh.getDriver().getPerson().getVisualizerData());
				positions.add(position);
			}
		}

	};
	
	// //////////////////////////////////////////////////////////
	// For NetStateWriter
	// /////////////////////////////////////////////////////////

	static public class AgentOnLink implements DrawableAgentI {

		public double posInLink_m;

		public int lane = 1;

		public double getPosInLink_m() {
			return this.posInLink_m;
		}

		public int getLane() {
			return this.lane;
		}
	}

	public void addToLane(QueueLane lane) {
		if (this.toLanes == null) {
			this.toLanes = new LinkedList<QueueLane>();
		}
		this.toLanes.add(lane);
	}
	
	protected void addDestinationLink(Link l) {
		this.destinationLinks.add(l);
	}
	
	protected List<Link> getDestinationLinks(){
		return this.destinationLinks;
	}

	
	protected int getVisualizerLane() {
		return visualizerLane;
	}

	
	protected void setVisualizerLane(int visualizerLane) {
		this.visualizerLane = visualizerLane;
	}
	
	// --- Implementation of Comparable interface ---
	// Sorts SubLinks of a QueueLink 
	
	public int compareTo(QueueLane queueLane) {
		if (this.meterFromLinkEnd < queueLane.getMeterFromLinkEnd()) {
			return -1;
		} else if (this.meterFromLinkEnd > queueLane.getMeterFromLinkEnd()) {
			return 1;
		} else {
			return 0;
		}
	}

	


}
