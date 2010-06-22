/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.sim.network.queueNetwork;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.AgentWait2LinkEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.network.LinkImpl;

import playground.mrieser.core.sim.api.SimVehicle;
import playground.mrieser.core.sim.network.api.SimLink;

/*package*/ class QueueLink implements SimLink, Steppable {

	private final static Logger log = Logger.getLogger(QueueLink.class);

	/*package*/ final QueueNetwork network;
	/*package*/ final Link link;

	/* DRIVING VEHICLE QUEUE */

	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	private final LinkedList<SimVehicle> vehQueue = new LinkedList<SimVehicle>();
	private final HashMap<SimVehicle, Double> earliestLeaveTimes = new HashMap<SimVehicle, Double>();

	private double storageCapacity = 0.0;

	/* BUFFER */

	/*package*/ final QueueBuffer buffer;

	/* WAITING QUEUE = DRIVEWAYS */

	private final Queue<SimVehicle> waitingList = new LinkedList<SimVehicle>();

	/* PARKING */

	private final Map<Id, SimVehicle> parkedVehicles = new LinkedHashMap<Id, SimVehicle>(10);

	/* TRAFFIC FLOW CHARACTERISTICS */

	private double freespeedTravelTime = 0.0;
	private double usedStorageCapacity = 0.0;

	/* OTHER MEMBERS */

	private static int spaceCapWarningCount = 0;

	public QueueLink(final Link link, final QueueNetwork network) {
		this.link = link;
		this.network = network;
		this.buffer = new QueueBuffer(this);
		recalculateAttributes();
	}

	private void recalculateAttributes() {
		double now = this.network.simEngine.getCurrentTime();
		double length = this.link.getLength();

		this.freespeedTravelTime = length / this.link.getFreespeed(now);

		double simulatedFlowCapacity = ((LinkImpl)this.link).getFlowCapacity(now);
		simulatedFlowCapacity = simulatedFlowCapacity * this.network.simEngine.getTimestepSize() * this.network.getFlowCapFactor();
		this.buffer.setFlowCapacity(simulatedFlowCapacity);

		double numberOfLanes = this.link.getNumberOfLanes(now);
		// first guess at storageCapacity:
		this.storageCapacity = (length * numberOfLanes) / this.network.getEffectiveCellSize() * this.network.getStorageCapFactor();

		// storage capacity needs to be at least enough to handle the cap_per_time_step:
		this.storageCapacity = Math.max(this.storageCapacity, this.buffer.getStorageCapacity());

		/*
		 * If speed on link is relatively slow, then we need MORE cells than the
		 * above spaceCap to handle the flowCap. Example: Assume freeSpeedTravelTime
		 * (aka freeTravelDuration) is 2 seconds. Than I need the spaceCap TWO times
		 * the flowCap to handle the flowCap.
		 */
		double tempStorageCapacity = this.freespeedTravelTime * simulatedFlowCapacity;
		if (this.storageCapacity < tempStorageCapacity) {
			if (spaceCapWarningCount <= 10) {
				log.warn("Link " + this.link.getId() + " too small: enlarge storage capcity from: " + this.storageCapacity + " Vehicles to: " + tempStorageCapacity + " Vehicles.  This is not fatal, but modifies the traffic flow dynamics.");
				if (spaceCapWarningCount == 10) {
					log.warn("Additional warnings of this type are suppressed.");
				}
				spaceCapWarningCount++;
			}
			this.storageCapacity = tempStorageCapacity;
		}

	}

	/*package*/ void addVehicleFromIntersection(final SimVehicle vehicle) {
		insertVehicle(vehicle, SimLink.POSITION_AT_FROM_NODE, SimLink.PRIORITY_IMMEDIATELY);
	}

	@Override
	public void insertVehicle(final SimVehicle vehicle, final double position, final double priority) {
		double now = this.network.simEngine.getCurrentTime();
		if (priority == SimLink.PRIORITY_PARKING) {
			this.parkedVehicles.put(vehicle.getId(), vehicle);
			return;
		}
		if (position == SimLink.POSITION_AT_FROM_NODE) {
			// vehicle enters from intersection
			this.vehQueue.add(vehicle);
			double earliestLeaveTime = now + this.freespeedTravelTime;
			this.earliestLeaveTimes.put(vehicle, earliestLeaveTime);
			this.usedStorageCapacity += vehicle.getSizeInEquivalents();
			this.network.simEngine.getEventsManager().processEvent(
					new LinkEnterEventImpl(now, vehicle.getId(), this.link.getId()));
		} else {
			if (priority == SimLink.PRIORITY_IMMEDIATELY) {
				// vehicle enters from a driveway
				this.vehQueue.addFirst(vehicle);
			} else {
				// vehicle enters from a driveway
				this.waitingList.add(vehicle);
			}
		}
	}

	@Override
	public void removeVehicle(final SimVehicle vehicle) {
		if (this.parkedVehicles.remove(vehicle.getId()) != null) {
			return;
		}
		if (this.waitingList.remove(vehicle)) {
			return;
		}
		if (this.vehQueue.remove(vehicle)) {
			return;
		}
		this.buffer.removeVehicle(vehicle);
	}

	@Override
	public void continueVehicle(final SimVehicle vehicle) {
		if (this.parkedVehicles.remove(vehicle.getId()) != null) {
			this.waitingList.add(vehicle);
		} else {
			// TODO
		}
	}

	@Override
	public void stopVehicle(final SimVehicle vehicle) {
		// TODO Auto-generated method stub

	}

	@Override
	public SimVehicle getParkedVehicle(final Id vehicleId) {
		return this.parkedVehicles.get(vehicleId);
	}

	@Override
	public void parkVehicle(final SimVehicle vehicle) {
		if (this.vehQueue.remove(vehicle) || this.buffer.removeVehicle(vehicle) || this.waitingList.remove(vehicle)) {
			this.parkedVehicles.put(vehicle.getId(), vehicle);
		}
	}

	@Override
	public void doSimStep(final double time) {
		this.buffer.updateCapacity();
		moveLinkToBuffer(time);
		moveWaitToBuffer(time);
	}

	private void moveLinkToBuffer(final double time) {
		SimVehicle veh;
		while ((veh = this.vehQueue.peek()) != null) {
			if (this.earliestLeaveTimes.get(veh).doubleValue() > time) {
				return;
			}
			if (!this.buffer.hasSpace()) {
				return;
			}
			double actionLocation = veh.getDriver().getNextActionOnCurrentLink();
			if (actionLocation >= 0.0) {
				veh.getDriver().handleNextAction(this);
			} else {
				this.buffer.addVehicle(this.vehQueue.poll(), time);
				this.usedStorageCapacity -= veh.getSizeInEquivalents();
			}
		} // end while
	}

	private void moveWaitToBuffer(final double time) {
		while (this.buffer.hasSpace()) {
			SimVehicle vehicle = this.waitingList.poll();
			if (vehicle == null) {
				return;
			}

			this.network.simEngine.getEventsManager().processEvent(
					new AgentWait2LinkEventImpl(time, vehicle.getId(), this.link.getId(), TransportMode.car));
			this.buffer.addVehicle(vehicle, time);
		}
	}


	@Override
	public Id getId() {
		return this.link.getId();
	}

	/*package*/ boolean hasSpace() {
		return this.usedStorageCapacity < this.storageCapacity;
	}

}
