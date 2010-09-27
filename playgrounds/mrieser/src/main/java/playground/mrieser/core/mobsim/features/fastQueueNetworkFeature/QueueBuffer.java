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

package playground.mrieser.core.mobsim.features.fastQueueNetworkFeature;

import java.util.LinkedList;
import java.util.Queue;

import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.utils.misc.Time;

import playground.mrieser.core.mobsim.api.SimVehicle;

/**
 * The "buffer" contains those vehicles that are allowed to leave a link. A
 * {@link QueueNode} will check the buffer of each incoming link to see if there
 * are vehicles that need to be moved.
 *
 * <h3>Calculation of buffer-capacity</h3>
 * <p>(Flow)Capacity for the buffer is defined as an amount of vehicles,
 * which can leave the buffer per timestep. Each second, this amount is
 * available. Assuming this flowCapacity contains fractional parts, these
 * fractional parts need to be summed up (accumulated) over multiple time
 * steps, until probably they sum up to allow an additional vehicle to
 * pass. Thus, the available space in the buffer can be calculated as follows:
 * <ul>
 * <li>flowCapacity: defines how much capacity is newly available in each timestep.
 * This is typically fixed for the whole simulation, and can be splitted into an
 * integer part and a fractional part</li>
 * <li>bufferCap: the capacity available in the current timestep. Initialized to
 * the integer part of flowCapacity at the beginning of each timestep. If
 * bufferCap >= 1.0, a vehicle can pass.</li>
 * <li>bufferCapAccumulate: Accumulation of fractional parts of flowCapacity. If
 * bufferCapAccumulate >= 1.0, an additional vehicle can pass. bufferCapAccumulate
 * is increased in each timestep by the fractional part of flowCapacity, until
 * bufferCapAccumulate >= 1.0.
 * </ul>
 * Whenever a vehicle is added to the buffer, is uses up some of the capacity. Thus,
 * the capacity consumed by the vehicle (see {@link SimVehicle#getSizeInEquivalents()})
 * is subtracted from either bufferCap or bufferCapAccumulate. If it is a large vehicle
 * (consuming more capacity than 1 standard vehicle), it may happen that bufferCap gets
 * negative. In this case, bufferCap is set to zero, but the missing difference is
 * subtracted from bufferCapAccumulate, leading to a possibly negative
 * bufferCapAccumulate. When a new timestep starts, and bufferCapAccumulate is negative,
 * bufferCap is initialied with zero, and the full flowCapacity is added to
 * bufferCapAccumulate.</p>
 * <p>
 * Thus, one could think of the variables in the following way:
 * <ul>
 * <li>flowCapacity: defines how much capacity is newly available in each timestep.</li>
 * <li>bufferCap: capacity newly available in current timestep</li>
 * <li>bufferCapAccumulate: compensating capacity amount for fractional parts of
 * flowCapacity and pre-used capacity by large vehicles</li>
 * <li>bufferCap + bufferCapAccumulate: effectively available flow capacity in current timestep.
 * If the sum >= 1.0, vehicles can leave the link (and thus be added to the buffer).</li>
 * </ul>
 * </p>
 *
 * @author mrieser
 */
/*package*/ class QueueBuffer {

	/*package*/ final QueueLink link;
	private QueueNode toNode = null;

	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	/*package*/ final Queue<SimVehicle> buffer = new LinkedList<SimVehicle>();

	private int storageCapacity = 0;

	private double bufferCap = 0.0;

	private double buffercap_accumulate = 0.0;

	private double lastMovedTime = Time.UNDEFINED_TIME;

	private double flowCapInt = 0;
	private double flowCapFraction = 0.0;

	public QueueBuffer(final QueueLink link) {
		this.link = link;
	}

	/*package*/ void init() {
		this.toNode = this.link.network.getNodes().get(link.link.getToNode().getId());
	}

	/*package*/ double getLastMovedTime() {
		return this.lastMovedTime;
	}

	/*package*/ SimVehicle getFirstVehicleInBuffer() {
		return this.buffer.peek();
	}

	/*package*/ SimVehicle removeFirstVehicleInBuffer() {
		double now = this.link.network.simEngine.getCurrentTime();
		SimVehicle veh = this.buffer.poll();
		this.lastMovedTime = now;
		this.link.network.simEngine.getEventsManager().processEvent(new LinkLeaveEventImpl(now, veh.getId(), this.link.getId()));
		return veh;
	}

	/*package*/ boolean removeVehicle(final SimVehicle vehicle) {
		return this.buffer.remove(vehicle);
	}

	/*package*/ boolean hasSpace() {
		return ((this.buffer.size() < this.storageCapacity)
				&& ((this.bufferCap + this.buffercap_accumulate) >= 1.0));
	}

	/*package*/ void addVehicle(final SimVehicle veh, final double now) {
		if (this.bufferCap + this.buffercap_accumulate >= 1.0) {
			this.bufferCap -= veh.getSizeInEquivalents();
			if (this.bufferCap < 0.0) {
				this.buffercap_accumulate += this.bufferCap;
				this.bufferCap = 0.0;
			}
		} else {
			throw new IllegalStateException("Buffer of link " + this.link.getId() + " has no space left!");
		}
		this.buffer.add(veh);
		this.toNode.activate();

		if (this.buffer.size() == 1) {
			this.lastMovedTime = now;
		}
	}

	/*package*/ void updateCapacity() {
		if (this.buffercap_accumulate < 0.0) {
			this.buffercap_accumulate += this.flowCapInt + this.flowCapFraction;
			this.bufferCap = 0.0;
		} else {
			this.bufferCap = this.flowCapInt;
			if (this.buffercap_accumulate < 1.0) {
				this.buffercap_accumulate += this.flowCapFraction;
			}
		}
	}

	public void setFlowCapacity(double flowCapacity) {
		this.flowCapInt = (int) flowCapacity;
		this.flowCapFraction = flowCapacity - this.flowCapInt;
		this.storageCapacity = (int) Math.ceil(flowCapacity);
		if (this.flowCapFraction > 0.0) {
			this.buffercap_accumulate = 1.0;
		}
	}

	/*package*/ int getStorageCapacity() {
		return this.storageCapacity;
	}

	/*package*/ boolean isActive() {
		return (this.buffercap_accumulate < 1.0 && this.flowCapFraction > 0.0);
	}

}
