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

package playground.mrieser.core.mobsim.features.refQueueNetworkFeature;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.events.AgentWait2LinkEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.network.LinkImpl;

import playground.mrieser.core.mobsim.api.MobsimVehicle;
import playground.mrieser.core.mobsim.network.api.MobsimLink;

/*package*/ class QueueLink implements MobsimLink, Steppable {

	private final static Logger log = Logger.getLogger(QueueLink.class);

	/*package*/ final QueueNetwork network;
	/*package*/ final Link link;

	/* DRIVING VEHICLE QUEUE */

	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	private final LinkedList<MobsimVehicle> vehQueue = new LinkedList<MobsimVehicle>();
	private final HashMap<MobsimVehicle, Double> earliestLeaveTimes = new HashMap<MobsimVehicle, Double>();

	private double storageCapacity = 0.0;

	/* BUFFER */

	/*package*/ final QueueBuffer buffer;

	/* WAITING QUEUE = DRIVEWAYS */

	private final Queue<MobsimVehicle> waitingList = new LinkedList<MobsimVehicle>();

	/* PARKING */

	private final Map<Id, MobsimVehicle> parkedVehicles = new LinkedHashMap<Id, MobsimVehicle>(10);

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
		simulatedFlowCapacity = simulatedFlowCapacity * this.network.simEngine.getTimestepSize();
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

	/*package*/ void addVehicleFromIntersection(final MobsimVehicle vehicle) {
		insertVehicle(vehicle, MobsimLink.POSITION_AT_FROM_NODE, MobsimLink.PRIORITY_IMMEDIATELY);
	}

	@Override
	public void insertVehicle(final MobsimVehicle vehicle, final double position, final double priority) {
		double now = this.network.simEngine.getCurrentTime();
		if (priority == MobsimLink.PRIORITY_PARKING) {
			this.parkedVehicles.put(vehicle.getId(), vehicle);
			return;
		}
		if (position == MobsimLink.POSITION_AT_FROM_NODE) {
			// vehicle enters from intersection
			this.vehQueue.add(vehicle);
			double earliestLeaveTime = (int) (now + this.freespeedTravelTime); // (int) for backwards compatibility
			this.earliestLeaveTimes.put(vehicle, earliestLeaveTime);
			this.usedStorageCapacity += vehicle.getSizeInEquivalents();
			this.network.simEngine.getEventsManager().processEvent(
					new LinkEnterEventImpl(now, vehicle.getId(), this.link.getId()));
		} else {
			if (priority == MobsimLink.PRIORITY_IMMEDIATELY) {
				this.usedStorageCapacity += vehicle.getSizeInEquivalents();
				// vehicle enters from a driveway
				this.vehQueue.addFirst(vehicle);
			} else {
				// vehicle enters from a driveway
				this.waitingList.add(vehicle);
			}
		}
	}

	@Override
	public void removeVehicle(final MobsimVehicle vehicle) {
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
	public void continueVehicle(final MobsimVehicle vehicle) {
		if (this.parkedVehicles.remove(vehicle.getId()) != null) {
			this.waitingList.add(vehicle);
		} else {
			// TODO
		}
	}

	@Override
	public void stopVehicle(final MobsimVehicle vehicle) {
		// TODO Auto-generated method stub

	}

	@Override
	public MobsimVehicle getParkedVehicle(final Id vehicleId) {
		return this.parkedVehicles.get(vehicleId);
	}

	@Override
	public void parkVehicle(final MobsimVehicle vehicle) {
		if (this.vehQueue.remove(vehicle)) {
			this.usedStorageCapacity -= vehicle.getSizeInEquivalents();
			this.parkedVehicles.put(vehicle.getId(), vehicle);
		} else if (this.buffer.removeVehicle(vehicle) || this.waitingList.remove(vehicle)) {
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
		MobsimVehicle veh;
		while ((veh = this.vehQueue.peek()) != null) {
			if (this.earliestLeaveTimes.get(veh).doubleValue() > time) {
				return;
			}
			double actionLocation = veh.getDriver().getNextActionOnCurrentLink();
			if (actionLocation >= 0.0) {
				veh.getDriver().handleNextAction(this, time);
			} else if (!this.buffer.hasSpace()) {
				return;
			} else {
				this.buffer.addVehicle(this.vehQueue.poll(), time);
				this.usedStorageCapacity -= veh.getSizeInEquivalents();
			}
		} // end while
	}

	private void moveWaitToBuffer(final double time) {
		MobsimVehicle vehicle;
		while ((vehicle = this.waitingList.peek()) != null) {
			double actionLocation = vehicle.getDriver().getNextActionOnCurrentLink();
			if (actionLocation >= 0.0) {
				vehicle.getDriver().handleNextAction(this, time);
			} else {
				if (this.buffer.hasSpace()) {
					this.waitingList.poll();
					this.network.simEngine.getEventsManager().processEvent(
							new AgentWait2LinkEventImpl(time, vehicle.getId(), this.link.getId()));
					this.buffer.addVehicle(vehicle, time);
				} else {
					return;
				}
			}
		}
	}

	/*package*/ void makeAllVehiclesStuck() {
		double now = this.network.simEngine.getCurrentTime();
		for (MobsimVehicle v : this.buffer.buffer) {
			this.network.simEngine.getEventsManager().processEvent(
					new AgentStuckEventImpl(now, v.getId(), this.link.getId(), null));
		}
		for (MobsimVehicle v : this.vehQueue) {
			this.network.simEngine.getEventsManager().processEvent(
					new AgentStuckEventImpl(now, v.getId(), this.link.getId(), null));
		}
		for (MobsimVehicle v : this.parkedVehicles.values()) {
			this.network.simEngine.getEventsManager().processEvent(
					new AgentStuckEventImpl(now, v.getId(), this.link.getId(), null));
		}
		for (MobsimVehicle v : this.waitingList) {
			this.network.simEngine.getEventsManager().processEvent(
					new AgentStuckEventImpl(now, v.getId(), this.link.getId(), null));
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
