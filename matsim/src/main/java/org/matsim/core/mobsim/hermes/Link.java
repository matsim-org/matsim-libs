package org.matsim.core.mobsim.hermes;

import java.util.Iterator;

public class Link {

	// The whole purpose of this implementation is to have a dynamically sized queue that never goes over the capacity
	// restriction. This becomes a big memory waste when large scenarios are used. This implementation is inspired in
	// Java's implementation of ArrayDequeue.
	public class AgentQueue implements Iterable<Agent> {
		// the storage
		private Agent[] array;
		// the max capacity of the queue
		private final int maxcapacity;
		// Pop/peak from head
		private int head;
		// Push to tail
		private int tail;
	    // Number of elements in the queue
	    private int size;

		public AgentQueue(int maxcapacity, int initialcapacity) {
			this.maxcapacity = maxcapacity;
			this.array = new Agent[initialcapacity];
		}

		private int inc(int number) {
			if (++number == array.length) {
				number = 0;
			}
			return number;
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
			    if (array.length == maxcapacity) {
                    return false;
				}
                // expand array
                Agent[] narray = new Agent[Math.min(maxcapacity, array.length * 2)];
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
			return new Iterator<Agent>() {

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
			return maxcapacity;
		}
	}

    // Id of the link.
    private int id;
    // Length of the link in meters.
    private final int length;
    // Max velocity within the link (meters per second).
    private final int velocity;
    // Queues of agents on this link. Boundary links use both queues.
    private final AgentQueue queue;
    // Number of time steps necessary to reset the flow.
    private final int flowPeriod;
    // Number of vehicles that can leave the link per time period.
    private final int flowCapacity;
    // When (which timestep) flow was updated the last time.
    private int lastFlowUpdate;

    public Link(int id, int capacity, int length, int velocity, int flowPeriod, int flowCapacity) {
        this.id = id;
        this.length = length;
        this.velocity = velocity;
        this.flowPeriod = flowPeriod;
        this.flowCapacity = flowCapacity;
        this.lastFlowUpdate = 0;
        // We do not preallocate using the capacity because it leads to huge memory waste.
        //this.queue = new AgentQueue(Math.max(1, capacity));
        this.queue = new AgentQueue(Math.max(1, capacity), Math.max(1, 16));
    }

    public void reset() {
    	queue.clear();
    }

    public boolean push(Agent agent) {
    	return queue.push(agent);
    }

    public void pop() {
        queue.pop();
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

    public int flow(int timestep) {
    	if (timestep - lastFlowUpdate >= flowPeriod) {
    		lastFlowUpdate = timestep;
    		return flowCapacity;
    	} else {
    		return 0;
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
