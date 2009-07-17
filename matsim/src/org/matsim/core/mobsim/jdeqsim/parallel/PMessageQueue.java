package org.matsim.core.mobsim.jdeqsim.parallel;

import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.core.mobsim.jdeqsim.EventMessage;
import org.matsim.core.mobsim.jdeqsim.Message;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.jdeqsim.Road;
import org.matsim.core.mobsim.jdeqsim.Scheduler;

public class PMessageQueue {
	// private PriorityQueue<Message> queue1 = new PriorityQueue<Message>();
	private PriorityQueue<Message> queueThread1 = new PriorityQueue<Message>();
	private PriorityQueue<Message> queueThread2 = new PriorityQueue<Message>();
	public long idOfLowerThread = 0;
	// private int queueSize = 0;
	// public long queueSizeThread1 = 0;
	// public long queueSizeThread2 = 0;
	private Scheduler scheduler;

	// the maximum time difference two threads are allowed to have in [s] (for
	// message time stamp)
	private double maxTimeDelta = 100;

	public boolean lowerThreadWitnessedEmptyQueue = false;
	public boolean higherThreadWitnessedEmptyQueue = false;

	/**
	 * 
	 * Putting a message into the queue
	 * 
	 * @param m
	 */
	public void putMessage(Message m) {
		// TODO: this should function also during initialization of the
		// simulation!!!

		boolean inLowerThreadCurrently = Thread.currentThread().getId() == idOfLowerThread ? true : false;
		ExtendedRoad curRoad = (ExtendedRoad) Road.getRoad(((EventMessage) m).vehicle.getCurrentLink().getId().toString());
		boolean roadBelongsToLowerThreadZone = curRoad.getThreadZoneId() == 0 ? true : false;

		synchronized (this) {
			if (roadBelongsToLowerThreadZone) {
				queueThread1.add(m);
				// queueSizeThread1++;
			} else if (!roadBelongsToLowerThreadZone) {
				queueThread2.add(m);
				// queueSizeThread2++;
			}
		}
	}

	/**
	 * 
	 * Remove the message from the queue and discard it. - queue1.remove(m) does
	 * not function, because it discards all message with the same priority as m
	 * from the queue. - This java api bug is reported at:
	 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6207984 =>
	 * queue1.removeAll(Collections.singletonList(m)); can be used, but it has
	 * been removed because of just putting a flag to kill a message is more
	 * efficient.
	 * 
	 * @param m
	 */
	public void removeMessage(Message m) {
		boolean inLowerThreadCurrently = Thread.currentThread().getId() == idOfLowerThread ? true : false;

		synchronized (m) {
			m.killMessage();
		}

	}

	/**
	 * 
	 * get the first message in the queue (with least time stamp)
	 * 
	 * @return
	 */
	public Message getNextMessage() {
		boolean inLowerThreadCurrently = Thread.currentThread().getId() == idOfLowerThread ? true : false;
		Message m = null;
		synchronized (this) {

			// don't allow one thread to advance too much (not more then
			// 'maxTimeDelta'
			if (queueThread1.peek() != null && queueThread2.peek() != null) {
				double delta = queueThread1.peek().getMessageArrivalTime() - queueThread2.peek().getMessageArrivalTime();
				if (Math.abs(delta) > maxTimeDelta) {
					if ((inLowerThreadCurrently && delta > 0) || (!inLowerThreadCurrently && delta < 0)) {
						return null;
					}
				}
			}

			if (inLowerThreadCurrently) {
				if (queueThread1.peek() != null) {
					// skip over dead messages
					// synchronization needed, because deadlock message might
					// have been manupulated

					while ((m = queueThread1.poll()) != null) {
						synchronized (m) {
							if (m.isAlive()) {
								break;
							}
						}
					}
				}
			} else {
				if (queueThread2.peek() != null) {
					// skip over dead messages
					// synchronization needed, because deadlock message might
					// have been manupulated
					while ((m = queueThread2.poll()) != null) {
						synchronized (m) {
							if (m.isAlive()) {
								break;
							}
						}
					}
				}
			}

		}

		return m;
	}

	/*
	 * Instead of just fetching one message, it should be possible to fetch all
	 * messages, which are within maxTimeDelta.
	 * 
	 * As input give an empty list and get the same list back with messages in
	 * it.
	 * 
	 */
	public LinkedList<Message> getNextMessages(LinkedList<Message> list) {
		boolean inLowerThreadCurrently = Thread.currentThread().getId() == idOfLowerThread ? true : false;
		Message m = null;

		synchronized (this) {

			// find out how far we can max go in time...
			double maxTimeStampAllowed = -1;
			double myMinTimeStamp = -1;
			double otherThreadMinTimeStamp = -1;

			if (inLowerThreadCurrently) {
				if (queueThread1.peek() != null) {
					myMinTimeStamp = queueThread1.peek().getMessageArrivalTime();
				}
				if (queueThread2.peek() != null) {
					otherThreadMinTimeStamp = queueThread2.peek().getMessageArrivalTime();
				}
			} else {
				if (queueThread2.peek() != null) {
					myMinTimeStamp = queueThread2.peek().getMessageArrivalTime();
				}
				if (queueThread1.peek() != null) {
					otherThreadMinTimeStamp = queueThread1.peek().getMessageArrivalTime();
				}
			}

			if (otherThreadMinTimeStamp == -1) {
				maxTimeStampAllowed = myMinTimeStamp + maxTimeDelta;
			} else {
				maxTimeStampAllowed = otherThreadMinTimeStamp + maxTimeDelta;
			}

			if (inLowerThreadCurrently) {
				// just give back all messages which are in the allowed time
				// (stamp) range

				while (queueThread1.peek() != null && queueThread1.peek().getMessageArrivalTime() <= maxTimeStampAllowed) {
					list.add(queueThread1.poll());
				}

			} else {
				while (queueThread2.peek() != null && queueThread2.peek().getMessageArrivalTime() <= maxTimeStampAllowed) {
					list.add(queueThread2.poll());
				}
			}
		}

		return list;
	}

	// this should be "synchronized (this)", but as each threads does a
	// synchronization in getNextmessage, we do
	// not need it here also... (here we only read...)
	// (basically performance improvement...)
	public boolean isEmpty() {
		// synchronized (this) {
		return queueThread1.size() + queueThread2.size() == 0;
		// }
	}

	// finds out, if all threads have witnessed the empty queue or not
	public boolean isEmptySync() {
		boolean inLowerThreadCurrently = Thread.currentThread().getId() == idOfLowerThread ? true : false;
		synchronized (this) {
			// just for debugging => change that afterwards to just one line
			if (isEmpty()) {
				// emptiness witnessed
				if (inLowerThreadCurrently) {
					lowerThreadWitnessedEmptyQueue = true;
				} else {
					higherThreadWitnessedEmptyQueue = true;
				}
			} else {
				if (inLowerThreadCurrently) {
					lowerThreadWitnessedEmptyQueue = false;
				} else {
					higherThreadWitnessedEmptyQueue = false;
				}
			}
			return lowerThreadWitnessedEmptyQueue && higherThreadWitnessedEmptyQueue;
		}
	}
}
