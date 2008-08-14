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
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.AgentWait2LinkEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Leg;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.netvis.DrawableAgentI;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;

/**
 * @author dstrippgen
 * @author dgrether
 * @author mrieser
 *
 * Queue Model Link implementation
 */
public class QueueLink {

	final private static Logger log = Logger.getLogger(QueueLink.class);

	private static int spaceCapWarningCount = 0;

	// ////////////////////////////////////////////////////////////////////
	// Queue Model specific stuff
	// ////////////////////////////////////////////////////////////////////
	/**
	 * parking list includes all vehicle that do not have yet reached their start
	 * time, but will start at this link at some time
	 */
	private final PriorityQueue<Vehicle> parkingList = new PriorityQueue<Vehicle>(
			30, new VehicleDepartureTimeComparator());

	/**
	 * All vehicles from parkingList move to the waitingList as soon as their time
	 * has come. They are then filled into the vehQueue, depending on free space
	 * in the vehQueue
	 */
	private final Queue<Vehicle> waitingList = new LinkedList<Vehicle>();

	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	private final Queue<Vehicle> vehQueue = new LinkedList<Vehicle>();

	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	private final Queue<Vehicle> buffer = new LinkedList<Vehicle>();

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

	private final Link link;

	private final QueueNetwork queueNetwork;

	private final QueueNode toQueueNode;

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
		initFlowCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME);
		recalcCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME);
	}

	public Link getLink() {
		return this.link;
	}

	protected QueueNode getToQueueNode() {
		return this.toQueueNode;
	}

	private void initFlowCapacity(final double time) {
		// network.capperiod is in hours, we need it per sim-tick and multiplied with flowCapFactor
		double flowCapFactor = Gbl.getConfig().simulation().getFlowCapFactor();

		// multiplying capacity from file by simTickCapFactor **and** flowCapFactor:
		this.simulatedFlowCapacity = this.link.getFlowCapacity(time)
				* SimulationTimer.getSimTickTime() * flowCapFactor;
	}

	private void recalcCapacity(final double time) {
		// network.capperiod is in hours, we need it per sim-tick and multiplied with flowCapFactor
		double storageCapFactor = Gbl.getConfig().simulation().getStorageCapFactor();

		this.inverseSimulatedFlowCapacity = 1.0 / this.simulatedFlowCapacity;
		this.bufferStorageCapacity = (int) Math.ceil(this.simulatedFlowCapacity);
		this.flowCapFraction = this.simulatedFlowCapacity - (int) this.simulatedFlowCapacity;

		// first guess at storageCapacity:
		this.storageCapacity = (this.link.getLength() * this.link.getLanes(time))
				/ ((NetworkLayer) this.link.getLayer()).getEffectiveCellSize() * storageCapFactor;

		// storage capacity needs to be at least enough to handle the cap_per_time_step:
		this.storageCapacity = Math.max(this.storageCapacity, this.bufferStorageCapacity);

		/*
		 * If speed on link is relatively slow, then we need MORE cells than the
		 * above spaceCap to handle the flowCap. Example: Assume freeSpeedTravelTime
		 * (aka freeTravelDuration) is 2 seconds. Than I need the spaceCap TWO times
		 * the flowCap to handle the flowCap.
		 */
		if (this.storageCapacity < this.getLink().getFreespeedTravelTime(time) * this.simulatedFlowCapacity) {
			if (spaceCapWarningCount <= 10) {
				log.warn("Link " + this.link.getId() + " too small: enlarge spaceCap.  This is not fatal, but modifies the traffic flow dynamics.");
				if (spaceCapWarningCount == 10) {
					log.warn("Additional warnings of this type are suppressed.");
				}
				spaceCapWarningCount++;
			}
			this.storageCapacity = this.getLink().getFreespeedTravelTime(time)
					* this.simulatedFlowCapacity;
		}
	}



	public void recalcTimeVariantAttributes(final double now) {
		initFlowCapacity(now);
		recalcCapacity(now);
	}


	// ////////////////////////////////////////////////////////////////////
	// Is called after link has been read completely
	// ////////////////////////////////////////////////////////////////////
	public void finishInit() {
		this.buffercap_accumulate = (this.flowCapFraction == 0.0 ? 0.0 : 1.0);
		this.active = false;
	}

	private void processVehicleArrival(final double now, final Vehicle veh) {
		QueueSimulation.getEvents().processEvent(
				new AgentArrivalEvent(now, veh.getDriver(),
						this.link, veh.getCurrentLeg()));
		// Need to inform the veh that it now reached its destination.
		veh.reachActivity(now, this);
	}

	/**
	 * Moves those vehicles, whose departure time has come, from the parking list
	 * to the wait list, from where they can later enter the link.
	 *
	 * @param now
	 *          the current time
	 */
	private void moveParkToWait(final double now) {
		Vehicle veh;
		while ((veh = this.parkingList.peek()) != null) {
			if (veh.getDepartureTime_s() > now) {
				return;
			}

			// Need to inform the veh that it now leaves its activity.
			veh.leaveActivity(now);

			// Generate departure event
			QueueSimulation.getEvents().processEvent(
					new AgentDepartureEvent(now, veh.getDriver(), this.link, veh.getCurrentLeg()));

			/*
			 * A.) we have an unknown leg mode (aka != "car").
			 *     In this case teleport veh to next activity location
			 * B.) we have no route (aka "next activity on same link") -> no waitingList
			 * C.) route known AND mode == "car" -> regular case, put veh in waitingList
			 */
			Leg leg = veh.getCurrentLeg();

			if (!leg.getMode().equals("car")) {
				QueueSimulation.handleUnknownLegMode(veh);
			} else {
				if (leg.getRoute().getRoute().size() != 0) {
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
			Vehicle veh = this.waitingList.poll();
			if (veh == null) {
				return;
			}
			addToBuffer(veh, now);
			QueueSimulation.getEvents().processEvent(
					new AgentWait2LinkEvent(now, veh.getDriver(), this.link, veh.getCurrentLeg()));
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

		Vehicle veh;
		while ((veh = this.vehQueue.peek()) != null) {
			if (veh.getDepartureTime_s() > now) {
				return;
			}
			// Check if veh has reached destination:
			if (veh.getDestinationLink().getId() == this.link.getId()) {
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
		}

	}

	private boolean updateActiveStatus() {
		/*
		 * Leave link active as long as there are vehicles on the link (ignore
		 * buffer because the buffer gets emptied by nodes and not links) and leave
		 * link active until buffercap has accumulated (so a newly arriving vehicle
		 * is not delayed).
		 */
		this.active = (this.buffercap_accumulate < 1.0) || (this.vehQueue.size() != 0) || (this.waitingList.size() != 0);
		return this.active;
	}

	public void activateLink() {
		if (!this.active) {
			this.queueNetwork.addActiveLink(this);
			this.active = true;
		}
	}

	// ////////////////////////////////////////////////////////////////////
	// called from framework, do everything related to link movement here
	// ////////////////////////////////////////////////////////////////////
	/*package*/  protected boolean moveLink(final double now) {
		updateBufferCapacity(now);
		// move vehicles from parking into waitingQueue if applicable
		moveParkToWait(now);
		// move vehicles from link to buffer
		moveLinkToBuffer(now);
		// move vehicles from waitingQueue into buffer if possible
		moveWaitToBuffer(now);

		return updateActiveStatus();
	}

	/*package*/  boolean moveLinkWaitFirst(final double now) {
		updateBufferCapacity(now);
		// move vehicles from parking into waitingQueue if applicable
		moveParkToWait(now);
		// move vehicles from waitingQueue into buffer if possible
		moveWaitToBuffer(now);
		// move vehicles from link to buffer
		moveLinkToBuffer(now);

		return updateActiveStatus();
	}

	private void updateBufferCapacity(final double time) {
		this.bufferCap = this.simulatedFlowCapacity;
		if (this.buffercap_accumulate < 1.0) {
			this.buffercap_accumulate += this.flowCapFraction;
		}
	}

	/*package*/ protected void addParking(final Vehicle veh) {
		this.parkingList.add(veh);
		this.queueNetwork.setLinkActivation(
				veh.getDepartureTime_s(), this);
	}

	/**
	 * Adds a vehicle to the link, called by
	 * {@link QueueNode#moveVehicleOverNode(Vehicle, double)}.
	 *
	 * @param veh
	 *          the vehicle
	 */
	public void add(final Vehicle veh) {
		double now = SimulationTimer.getTime();

		activateLink();
		veh.setCurrentLink(this.link);
		this.vehQueue.add(veh);
		veh.setDepartureTime_s((int) (now + this.getLink().getFreespeedTravelTime(now)));
		QueueSimulation.getEvents().processEvent(
				new LinkEnterEvent(now, veh.getDriver(),
						this.link, veh.getCurrentLeg().getNum()));
	}

	// ////////////////////////////////////////////////////////////////////
	// getter / setter
	// ////////////////////////////////////////////////////////////////////

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

	/*package*/ Vehicle popFirstFromBuffer() {
		double now = SimulationTimer.getTime();
		Vehicle veh = this.buffer.poll();
		Vehicle v2 = this.buffer.peek();
		if (v2 != null) {
			v2.setLastMovedTime(now);
		}

		QueueSimulation.getEvents().processEvent(new LinkLeaveEvent(now, veh.getDriver(), this.link, veh.getCurrentLeg().getNum()));

		return veh;
	}

	private void addToBuffer(final Vehicle veh, final double now) {
		if (this.bufferCap >= 1.0) {
			this.bufferCap--;
		} else if (this.buffercap_accumulate >= 1.0) {
			this.buffercap_accumulate--;
		} else {
			throw new RuntimeException("Buffer of link " + this.link.getId() + " has no space left!");
		}
		this.buffer.add(veh);
		veh.setLastMovedTime(now);
		this.toQueueNode.activateNode();
	}

	Vehicle getFirstFromBuffer() {
		return this.buffer.peek();
	}

	boolean bufferIsEmpty() {
		return this.buffer.isEmpty();
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
	 * @return <code>true</code> if there are less vehicles in buffer than the flowCapacity's ceil
	 */
	private boolean hasBufferSpace() {
		return ((this.buffer.size() < this.bufferStorageCapacity) && ((this.bufferCap >= 1.0) || (this.buffercap_accumulate >= 1.0)));
	}

	/**
	 * @return The value for coloring the link in NetVis. Actual: veh count / space capacity
	 */
	public double getDisplayableSpaceCapValue() {
		return (this.buffer.size() + this.vehQueue.size()) / this.storageCapacity;
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
		int count = this.buffer.size();
		double now = SimulationTimer.getTime();
		for (Vehicle veh : this.vehQueue) {
			// Check if veh has reached destination
			if (veh.getDepartureTime_s() <= now) {
				count++;
			}
		}
		return count * 2.0 / this.storageCapacity;
	}

	/**
	 * @return Returns the maximum number of vehicles that can be placed on the
	 *         link at a time.
	 */
	public double getSpaceCap() {
		return this.storageCapacity;
	}

	/**
	 * @return Returns a collection of all vehicles (driving, parking, in buffer,
	 *         ...) on the link.
	 */
	public Collection<Vehicle> getAllVehicles() {

		Collection<Vehicle> vehicles = new ArrayList<Vehicle>();

		vehicles.addAll(this.waitingList);
		vehicles.addAll(this.parkingList);
		vehicles.addAll(this.vehQueue);
		vehicles.addAll(this.buffer);

		return vehicles;
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
		int cnt = 0;
		double queueEnd = this.link.getLength(); // the position of the start of the queue jammed vehicles build at the end of the link
		double storageCapFactor = Gbl.getConfig().simulation().getStorageCapFactor();
		double vehLen = Math.min( // the length of a vehicle in visualization
				this.link.getLength() / (this.storageCapacity + this.bufferStorageCapacity), // all vehicles must have place on the link
				((NetworkLayer)this.link.getLayer()).getEffectiveCellSize() / storageCapFactor); // a vehicle should not be larger than it's actual size

		// put all cars in the buffer one after the other
		for (Vehicle veh : this.buffer) {

			int lane = 1 + (veh.getID() % this.link.getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME));

			int cmp = (int) (veh.getDepartureTime_s() + this.inverseSimulatedFlowCapacity + 2.0);
			double speed = (now > cmp) ? 0.0 : this.link.getFreespeed(Time.UNDEFINED_TIME);

			PositionInfo position = new PositionInfo(veh.getDriver().getId(), this.link, queueEnd,
					lane, speed, PositionInfo.VehicleState.Driving, veh.getDriver().getVisualizerData());
			positions.add(position);
			cnt++;
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
		for (Vehicle veh : this.vehQueue) {
			double travelTime = now - (veh.getDepartureTime_s() - this.getLink().getFreespeedTravelTime(now));
			double distanceOnLink = (this.getLink().getFreespeedTravelTime(now) == 0.0 ? 0.0
					: ((travelTime / this.getLink().getFreespeedTravelTime(now)) * this.link.getLength()));
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
					+ this.inverseSimulatedFlowCapacity + 2.0);
			double speed = (now > cmp) ? 0.0 : this.link.getFreespeed(now);
			int lane = 1 + (veh.getID() % this.link.getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME));
			PositionInfo position = new PositionInfo(veh.getDriver().getId(), this.link, distanceOnLink,
					lane, speed, PositionInfo.VehicleState.Driving, veh.getDriver().getVisualizerData());
			positions.add(position);
			lastDistance = distanceOnLink;
		}

		/*
		 * Put the vehicles from the waiting list in positions. Their actual
		 * position doesn't matter, so they are just placed to the coordinates of
		 * the from node
		 */
		int lane = this.link.getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME) + 1; // place them next to the link
		for (Vehicle veh : this.waitingList) {
			PositionInfo position = new PositionInfo(veh.getDriver().getId(), this.link,
					((NetworkLayer) this.link.getLayer()).getEffectiveCellSize(), lane, 0.0,
					PositionInfo.VehicleState.Parking, veh.getDriver().getVisualizerData());
			positions.add(position);
		}

		/*
		 * put the vehicles from the parking list in positions their actual position
		 * doesn't matter, so they are just placed to the coordinates of the from
		 * node
		 */
		lane = this.link.getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME) + 2; // place them next to the link
		for (Vehicle veh : this.parkingList) {
			PositionInfo position = new PositionInfo(veh.getDriver().getId(), this.link,
					((NetworkLayer) this.link.getLayer()).getEffectiveCellSize(), lane, 0.0,
					PositionInfo.VehicleState.Parking, veh.getDriver().getVisualizerData());
			positions.add(position);
		}
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
		int cnt = this.buffer.size() + this.vehQueue.size();
		int nLanes = this.link.getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME);
		if (cnt > 0) {
			double cellSize = this.link.getLength() / cnt;
			double distFromFromNode = this.link.getLength() - cellSize / 2.0;
			double freespeed = this.link.getFreespeed(Time.UNDEFINED_TIME);

			// the cars in the buffer
			for (Vehicle veh : this.buffer) {
				int lane = 1 + veh.getID() % nLanes;
				int cmp = (int) (veh.getDepartureTime_s() + this.inverseSimulatedFlowCapacity + 2.0);
				double speed = (time > cmp ? 0.0 : freespeed);
				PositionInfo position = new PositionInfo(veh.getDriver().getId(), this.link,
						distFromFromNode, lane, speed, PositionInfo.VehicleState.Driving, null);
				positions.add(position);
				distFromFromNode -= cellSize;
			}

			// the cars in the drivingQueue
			for (Vehicle veh : this.vehQueue) {
				int lane = 1 + veh.getID() % nLanes;
				int cmp = (int) (veh.getDepartureTime_s() + this.inverseSimulatedFlowCapacity + 2.0);
				double speed = (time > cmp ? 0.0 : freespeed);
				PositionInfo position = new PositionInfo(veh.getDriver().getId(), this.link,
						distFromFromNode, lane, speed, PositionInfo.VehicleState.Driving, null);
				positions.add(position);
				distFromFromNode -= cellSize;
			}
		}

		// the cars in the waitingQueue
		// the actual position doesn't matter, so they're just placed next to the
		// link at the end
		cnt = this.waitingList.size();
		if (cnt > 0) {
			int lane = nLanes + 2;
			double cellSize = Math.min(7.5, this.link.getLength() / cnt);
			double distFromFromNode = this.link.getLength() - cellSize / 2.0;
			for (Vehicle veh : this.waitingList) {
				PositionInfo position = new PositionInfo(veh.getDriver().getId(), this.link,
						distFromFromNode, lane, 0.0, PositionInfo.VehicleState.Parking, null);
				positions.add(position);
				distFromFromNode -= cellSize;
			}
		}

		// the cars in the parkingQueue
		// the actual position doesn't matter, so they're distributed next to the
		// link
		cnt = this.parkingList.size();
		if (cnt > 0) {
			int lane = nLanes + 4;
			double cellSize = this.link.getLength() / cnt;
			double distFromFromNode = this.link.getLength() - cellSize / 2.0;
			for (Vehicle veh : this.parkingList) {
				PositionInfo position = new PositionInfo(veh.getDriver().getId(), this.link,
						distFromFromNode, lane, 0.0, PositionInfo.VehicleState.Parking, null);
				positions.add(position);
				distFromFromNode -= cellSize;
			}
		}
	}

	void clearVehicles() {
		double now = SimulationTimer.getTime();

		for (Vehicle veh : this.parkingList) {
			QueueSimulation.getEvents().processEvent(
					new AgentStuckEvent(now, veh.getDriver(), veh.getCurrentLink(), veh.getCurrentLeg()));
		}
		Simulation.decLiving(this.parkingList.size());
		Simulation.incLost(this.parkingList.size());
		this.parkingList.clear();

		for (Vehicle veh : this.waitingList) {
			QueueSimulation.getEvents().processEvent(
					new AgentStuckEvent(now, veh.getDriver(), veh.getCurrentLink(), veh.getCurrentLeg()));
		}
		Simulation.decLiving(this.waitingList.size());
		Simulation.incLost(this.waitingList.size());
		this.waitingList.clear();

		for (Vehicle veh : this.vehQueue) {
			QueueSimulation.getEvents().processEvent(
					new AgentStuckEvent(now, veh.getDriver(), veh.getCurrentLink(), veh.getCurrentLeg()));
		}
		Simulation.decLiving(this.vehQueue.size());
		Simulation.incLost(this.vehQueue.size());
		this.vehQueue.clear();

		for (Vehicle veh : this.buffer) {
			QueueSimulation.getEvents().processEvent(
					new AgentStuckEvent(now, veh.getDriver(), veh.getCurrentLink(), veh.getCurrentLeg()));
		}
		Simulation.decLiving(this.buffer.size());
		Simulation.incLost(this.buffer.size());
		this.buffer.clear();
	}

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

	// search for vehicleId..
	public Vehicle getVehicle(final Id id) {
		for (Vehicle veh : this.vehQueue) {
			if (veh.getDriver().getId().equals(id))
				return veh;
		}
		for (Vehicle veh : this.buffer) {
			if (veh.getDriver().getId().equals(id))
				return veh;
		}
		for (Vehicle veh : this.parkingList) {
			if (veh.getDriver().getId().equals(id))
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
}
