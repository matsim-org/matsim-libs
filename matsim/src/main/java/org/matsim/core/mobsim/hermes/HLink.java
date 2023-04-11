/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.hermes;

import java.util.Iterator;

class HLink {

	private float currentCapacity;
	private final int initialCapacity;

	// Id of the link.
	private final int id;

	// The whole purpose of this implementation is to have a dynamically sized queue that never goes over the capacity
	// restriction. This becomes a big memory waste when large scenarios are used. This implementation is inspired in
	// Java's implementation of ArrayDequeue.
	public static class AgentQueue implements Iterable<Agent> {

		// the storage
		private Agent[] array;
		// the max capacity of the queue
		private int maxCapacity;

		// Pop/peak from head
		private int head;
		// Push to tail
		private int tail;
		// Number of elements in the queue
		private int size;

		public AgentQueue(int maxCapacity, int initialcapacity) {
			this.maxCapacity = maxCapacity;
			this.array = new Agent[initialcapacity];
		}

		private int inc(int number) {
			if (++number == array.length) {
				number = 0;
			}
			return number;
		}

		public boolean forcePush(Agent agent) {
			maxCapacity++;
			return push(agent);
		}

		public boolean push(Agent agent) {

			if (size == 0) {
				array[tail] = agent;
				size += 1;
				return true;
			} else if (array.length > size) {
				array[tail = inc(tail)] = agent;
				size += 1;
				return true;
			} else {
				// expand array
				Agent[] narray = new Agent[array.length * 2];
				for (int i = head, left = size, dst = 0; left > 0; i = inc(i), left--, dst++) {
					narray[dst] = array[i];
				}
				array = narray;
				head = 0;
				tail = size - 1;
				// push
				array[tail = inc(tail)] = agent;
				size += 1;
				return true;
			}
		}

		public Agent peek() {
			return size == 0 ? null : array[head];
		}

		public void pop() {
			if (size > 0) {
	            array[head] = null;
				head = inc(head);
	            if (--size == 0) {
	                tail = head = 0;
	            }
			}
		}

		public int size() {
			return size;
		}

		public void clear() {
			for (int i = head, left = size; left > 0; i = inc(i), left--) {
				array[i] = null;
			}
			head = tail = size = 0;
		}

		@Override
		public Iterator<Agent> iterator() {
			return new Iterator<>() {

				private int idx = head;
				private int left = size;

				@Override
				public boolean hasNext() {
					return left-- > 0;
				}

				@Override
				public Agent next() {
					Agent agent = array[idx];
					idx = inc(idx);
					return agent;
				}
			};
		}

		public int capacity() {
			return maxCapacity;
		}
	}
    // Length of the link in meters.
    private final int length;
    // Max velocity within the link (meters per second).
    private final int velocity;
    // Queues of agents on this link. Boundary links use both queues.
    private final AgentQueue queue;
    // Number of vehicles that can leave the link per time second.
    private final float flowCapacityPerS;
    private float flowLeftInTimestep;
    private int lastUpdate;
    // When (which timestep) flow was updated the last time.
    private int nextFreeFlowSlot;
	private int lastPush;
	private final int stuckTimePeriod;

    public HLink(int id, int capacity, int length, int velocity,  float flowCapacityperSecond, int stuckTimePeriod) {
        this.id = id;
        this.length = length;
        this.velocity = velocity;
        this.flowCapacityPerS = flowCapacityperSecond;
        this.stuckTimePeriod = stuckTimePeriod;
        this.lastPush = 0;
        this.lastUpdate = 0;
        this.nextFreeFlowSlot = 0;
        this.initialCapacity = capacity;
        this.currentCapacity = capacity;
        this.flowLeftInTimestep = flowCapacityperSecond;

		// We do not preallocate using the capacity because it leads to huge memory waste.
		//this.queue = new AgentQueue(Math.max(1, capacity));
		this.queue = new AgentQueue(Math.max(1, capacity), Math.min(capacity, 16));
	}

	public void reset() {
		queue.clear();
		this.nextFreeFlowSlot = 0;
		this.lastPush = 0;
		this.lastUpdate = 0;
		this.currentCapacity = initialCapacity;
		this.flowLeftInTimestep = flowCapacityPerS;

	}

	public boolean push(Agent agent, int timestep, float storageCapacityPCU) {
		//avoid long vehicles not being able to enter a short link
		float effectiveStorageCapacity = Math.min(storageCapacityPCU, initialCapacity);
		if (currentCapacity - effectiveStorageCapacity >= 0) {
			if (queue.push(agent)) {
				lastPush = timestep;
				currentCapacity = currentCapacity - effectiveStorageCapacity;
				return true;
			} else {
				throw new RuntimeException("should not happen?");
			}
		} else if (stuckTimePeriod != Integer.MAX_VALUE && (lastPush + stuckTimePeriod) < timestep) {
			boolean result = queue.forcePush(agent);
			lastPush = timestep;
			currentCapacity = currentCapacity - effectiveStorageCapacity;
			return result;
		} else {
			return false;
		}
	}

	public void pop(float storageCapacityPCE) {
		queue.pop();
		currentCapacity += storageCapacityPCE;
	}

    public int nexttime () {
        if (queue.size() == 0) {
            return 0;
        } else {
            return queue.peek().linkFinishTime;
        }
    }

    public int length() {
        return this.length;
    }

    public boolean flow(int timestep, float requestedFlow) {
    	if (timestep  >= nextFreeFlowSlot) {
			// if requestedFlow<flowCapacityPerS, more than one vehicle can pass per timestep
			if (lastUpdate == timestep){
				if (flowLeftInTimestep >= 0) {
					flowLeftInTimestep-=requestedFlow;
					nextFreeFlowSlot = timestep + (int) Math.floor( requestedFlow / flowCapacityPerS);
					return true;
				} else return false;
			} else {
				flowLeftInTimestep=flowLeftInTimestep+flowCapacityPerS-requestedFlow;
				lastUpdate = timestep;
				nextFreeFlowSlot = timestep + (int) Math.floor( requestedFlow / flowCapacityPerS);
				return true;
			}

    	} else {
    		return false;
    	}
    }

    public int velocity() {
        return this.velocity;
    }

    public AgentQueue queue() {
        return this.queue;
    }

    public int capacity() {
        return this.queue.capacity();
    }

    public int id() {
        return this.id;
    }
}
