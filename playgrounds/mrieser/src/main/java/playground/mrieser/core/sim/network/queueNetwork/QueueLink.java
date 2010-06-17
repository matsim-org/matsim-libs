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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.mobsim.framework.Steppable;

import playground.mrieser.core.sim.api.SimVehicle;
import playground.mrieser.core.sim.network.api.SimLink;

/*package*/ class QueueLink implements SimLink, Steppable {

	private final QueueNetwork network;
	private final Link link;

	/* DRIVING VEHICLE QUEUE */

	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	private final LinkedList<SimVehicle> vehQueue = new LinkedList<SimVehicle>();
	private final HashMap<SimVehicle, Double> earliestLeaveTimes = new HashMap<SimVehicle, Double>();

	/* BUFFER */

	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	private final Queue<SimVehicle> buffer = new LinkedList<SimVehicle>();
	private int bufferStorageCapacity = 0;
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

	/* PARKING */

	private final Map<Id, SimVehicle> parkedVehicles = new LinkedHashMap<Id, SimVehicle>(10);

	private double freespeedTravelTime = 0.0;
	private double usedStorageCapacity = 0.0;

	public QueueLink(final Link link, final QueueNetwork network) {
		this.link = link;
		this.network = network;
	}

	@Override
	public void insertVehicle(SimVehicle vehicle, double position, double priority) {
		double now = this.network.simEngine.getCurrentTime();
		if (position == SimLink.POSITION_AT_FROM_NODE) {
			// vehicle enters from intersection
			this.vehQueue.add(vehicle);
			double earliestLeaveTime = now + this.freespeedTravelTime;
			this.earliestLeaveTimes.put(vehicle, earliestLeaveTime);
			this.usedStorageCapacity += vehicle.getSizeInEquivalents();
			this.network.simEngine.getEventsManager().processEvent(
					new LinkEnterEventImpl(now, vehicle.getVehicle().getId(), this.link.getId()));
		} else {
			// vehicle enters from a driveway

		}
	}

	@Override
	public void removeVehicle(SimVehicle vehicle) {
		// TODO Auto-generated method stub
	}

	@Override
	public void continueVehicle(SimVehicle vehicle) {
		// TODO Auto-generated method stub

	}

	@Override
	public void stopVehicle(SimVehicle vehicle) {
		// TODO Auto-generated method stub

	}

	@Override
	public SimVehicle getParkedVehicle(Id vehicleId) {
		return this.parkedVehicles.get(vehicleId);
	}

	@Override
	public void parkVehicle(SimVehicle vehicle) {
		this.parkedVehicles.put(vehicle.getVehicle().getId(), vehicle);
	}

	@Override
	public void doSimStep(final double time) {
		moveLinkToBuffer(time);
		moveWaitToBuffer(time);
	}

	private void moveLinkToBuffer(final double time) {

		// handle regular traffic
		SimVehicle veh;
		while ((veh = this.vehQueue.peek()) != null) {
			if (this.earliestLeaveTimes.get(veh).doubleValue() > time) {
				return;
			}
			if (!hasBufferSpace()) {
				return;
			}
			this.buffer.add(this.vehQueue.poll());
			this.usedStorageCapacity -= veh.getSizeInEquivalents();
		} // end while
	}

	private void moveWaitToBuffer(final double time) {

	}

	private boolean hasBufferSpace() {
		return ((this.buffer.size() < this.bufferStorageCapacity) && ((this.bufferCap >= 1.0)
				|| (this.buffercap_accumulate >= 1.0)));
	}

	@Override
	public Id getId() {
		return this.link.getId();
	}

	/*package*/ SimVehicle getFirstVehicleInBuffer() {
		return this.buffer.peek();
	}

	/*package*/ SimVehicle removeFirstVehicleInBuffer() {
		return this.buffer.poll();
	}

}
