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
 * @author mrieser
 */
/*package*/ class QueueBuffer {

	/*package*/ final QueueLink link;
	private QueueNode toNode = null;

	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	private final Queue<SimVehicle> buffer = new LinkedList<SimVehicle>();//ConcurrentLinkedQueue<SimVehicle>(); // must be thread-safe data structure
	private int storageCapacity = 0;
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
	private double buffercap_accumulate = 0.0;

	private double lastMovedTime = Time.UNDEFINED_TIME;

	private double flowCapacity = 0.0;
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
		return ((this.buffer.size() < this.storageCapacity) && ((this.bufferCap >= 1.0)
				|| (this.buffercap_accumulate >= 1.0)));
	}

	/*package*/ void addVehicle(final SimVehicle veh, final double now) {
		if (this.bufferCap >= 1.0) {
			this.bufferCap--;
		} else if (this.buffercap_accumulate >= 1.0) {
			this.buffercap_accumulate--;
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
		this.bufferCap = this.flowCapacity;
		if (this.buffercap_accumulate < 1.0) {
			this.buffercap_accumulate += this.flowCapFraction;
		}
	}

	public void setFlowCapacity(double flowCapacity) {
		this.flowCapacity = flowCapacity;
		this.flowCapFraction = this.flowCapacity - (int) this.flowCapacity;
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
