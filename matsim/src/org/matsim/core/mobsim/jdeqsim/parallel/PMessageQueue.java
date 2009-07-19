package org.matsim.core.mobsim.jdeqsim.parallel;

import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.mobsim.jdeqsim.DeadlockPreventionMessage;
import org.matsim.core.mobsim.jdeqsim.EndLegMessage;
import org.matsim.core.mobsim.jdeqsim.EndRoadMessage;
import org.matsim.core.mobsim.jdeqsim.EnterRoadMessage;
import org.matsim.core.mobsim.jdeqsim.EventMessage;
import org.matsim.core.mobsim.jdeqsim.LeaveRoadMessage;
import org.matsim.core.mobsim.jdeqsim.Message;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.jdeqsim.Road;
import org.matsim.core.mobsim.jdeqsim.Scheduler;
import org.matsim.core.mobsim.jdeqsim.StartingLegMessage;

public class PMessageQueue extends MessageQueue {
	// private PriorityQueue<Message> queue1 = new PriorityQueue<Message>();
	private PriorityQueue<Message> queueThread1 = new PriorityQueue<Message>();
	private PriorityQueue<Message> queueThread2 = new PriorityQueue<Message>();
	public long idOfLowerThread = 0;
	public long idOfMainThread = 0;
	private LinkedList<Message> bufferThread1 = new LinkedList<Message>();
	private LinkedList<Message> bufferThread2 = new LinkedList<Message>();
	// private int queueSize = 0;

	// the maximum time difference two threads are allowed to have in [s] (for
	// message time stamp) => with this parameter the number of locks can be
	// reduced... => we need to find out, how much effect it has...
	private double maxTimeDelta = 10;

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

		long idOfCurrentThread = Thread.currentThread().getId();
		boolean inLowerThreadCurrently = idOfCurrentThread == idOfLowerThread ? true
				: false;
		ExtendedRoad messageTargetRoad = null;

		PVehicle vehicle = (PVehicle) ((EventMessage) m).vehicle;

		ExtendedRoad receivingRoad = (ExtendedRoad) m.getReceivingUnit();

		ExtendedRoad currentRoad = receivingRoad;
		ExtendedRoad nextRoad = receivingRoad;
		Link tempLink = null;

		// the ordering of this sequence is according to the
		// frequency of the messages
		if (m instanceof EnterRoadMessage) {

			tempLink = vehicle.getCurrentLink();
			if (tempLink != null) {
				currentRoad = (ExtendedRoad) Road.getRoad(tempLink.getId()
						.toString());
			}

			messageTargetRoad = currentRoad;

		} else if (m instanceof LeaveRoadMessage) {

			messageTargetRoad = receivingRoad;

		} else if (m instanceof EndRoadMessage) {

			tempLink = vehicle.getNextLinkInLeg();
			if (tempLink != null) {
				nextRoad = (ExtendedRoad) Road.getRoad(tempLink.getId()
						.toString());
			}

			messageTargetRoad = nextRoad;
		} else if (m instanceof DeadlockPreventionMessage) {
			messageTargetRoad = receivingRoad;
		} else if (m instanceof StartingLegMessage) {

			if (vehicle.getCurrentLeg().getMode().equals(TransportMode.car)) {
				tempLink = vehicle.getCurrentLink();
				if (tempLink != null) {
					currentRoad = (ExtendedRoad) Road.getRoad(tempLink.getId()
							.toString());
				}

				messageTargetRoad = currentRoad;
			} else {
				// TODO: we need the first link in the next leg. if this does
				// not function
				// then we need to do this manually.
				tempLink = vehicle.getNextLinkInLeg();
				if (tempLink != null) {
					nextRoad = (ExtendedRoad) Road.getRoad(tempLink.getId()
							.toString());
				}
				messageTargetRoad = nextRoad;
			}
		} else if (m instanceof EndLegMessage) {
			messageTargetRoad = currentRoad;
		} else {
			// there are no other type of messages which can come
			// here...
			assert (false);
		}

		boolean roadBelongsToLowerThreadZone = messageTargetRoad
				.getThreadZoneId() == 0 ? true : false;

		// a thread should put messages of his zone directly into his zone queue
		// but messages for other zone should be put into a buffer
		// Profiling shows: This strategy really removes blocking of the threads...
		if (roadBelongsToLowerThreadZone) {
			if (inLowerThreadCurrently) {
				queueThread1.add(m);
			} else {
				synchronized (bufferThread1) {
					bufferThread1.add(m);
				}
			}
		} else {
			if (!inLowerThreadCurrently) {
				queueThread2.add(m);
			} else {
				synchronized (bufferThread2) {
					bufferThread2.add(m);
				}
			}
		}

		/*
		 * if (curRoad.isBorderZone() || messageForDifferentZone || true) {
		 * synchronized (this) { if (roadBelongsToLowerThreadZone) {
		 * queueThread1.add(m); // queueSizeThread1++; } else if
		 * (!roadBelongsToLowerThreadZone) { queueThread2.add(m); //
		 * queueSizeThread2++; } } } else if (idOfCurrentThread ==
		 * idOfMainThread) { // during initialization if
		 * (roadBelongsToLowerThreadZone) { queueThread1.add(m); //
		 * queueSizeThread1++; } else if (!roadBelongsToLowerThreadZone) {
		 * queueThread2.add(m); // queueSizeThread2++; } } else { if
		 * (roadBelongsToLowerThreadZone && inLowerThreadCurrently) {
		 * queueThread1.add(m); // queueSizeThread1++; } else if
		 * (!roadBelongsToLowerThreadZone && !inLowerThreadCurrently) {
		 * queueThread2.add(m); // queueSizeThread2++; } else { assert (false) :
		 * "Inconsitency in logic!!! => the border area is not setup in the right way..."
		 * ; }
		 * 
		 * }
		 */
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
		boolean inLowerThreadCurrently = Thread.currentThread().getId() == idOfLowerThread ? true
				: false;

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
		boolean inLowerThreadCurrently = Thread.currentThread().getId() == idOfLowerThread ? true
				: false;
		Message m = null;

		// don't allow one thread to advance too much (not more then
		// 'maxTimeDelta'
		// this operation should be synchronized (queueThread1 and
		// queueThread2), but it is not
		// for performance reasons...

		// if (queueThread1.peek() != null && queueThread2.peek() != null) {
		// double delta = queueThread1.peek().getMessageArrivalTime()
		// - queueThread2.peek().getMessageArrivalTime();
		// if (Math.abs(delta) > maxTimeDelta) {
		// if ((inLowerThreadCurrently && delta > 0)
		// || (!inLowerThreadCurrently && delta < 0)) {
		// return null;
		// }
		// }
		// }
		//
		// if (inLowerThreadCurrently) {
		// synchronized (queueThread1) {
		// if (queueThread1.peek() != null) {
		// // skip over dead messages
		// // synchronization needed, because deadlock message
		// // might
		// // have been manupulated
		//
		// while ((m = queueThread1.poll()) != null) {
		// synchronized (m) {
		// if (m.isAlive()) {
		// break;
		// }
		// }
		// }
		// }
		// }
		// } else {
		// synchronized (queueThread2) {
		// if (queueThread2.peek() != null) {
		// // skip over dead messages
		// // synchronization needed, because deadlock message
		// // might
		// // have been manupulated
		// while ((m = queueThread2.poll()) != null) {
		// synchronized (m) {
		// if (m.isAlive()) {
		// break;
		// }
		// }
		// }
		// }
		// }
		// }

		return m;

	}

	/*
	 * Instead of just fetching one message, it should be possible to fetch all
	 * messages, which are within maxTimeDelta.
	 * 
	 * As input give an empty list and get the same list back with messages in
	 * it.
	 */
	/**
	 * TODO: Probably we could switch back to getNextMessage now, as the locking logic has changed now...
	 * => not really oder?
	 */
	public LinkedList<Message> getNextMessages(LinkedList<Message> list) {
		boolean inLowerThreadCurrently = Thread.currentThread().getId() == idOfLowerThread ? true
				: false;
		Message m = null;

		if (inLowerThreadCurrently) {
			synchronized (bufferThread1) {
				queueThread1.addAll(bufferThread1);
				bufferThread1.clear();
			}
		} else {
			synchronized (bufferThread2) {
				queueThread2.addAll(bufferThread2);
				bufferThread2.clear();
			}
		}

		// find out how far we can max go in time...
		// this operation should be synchronized (queueThread1 and
		// queueThread2), but it is not
		// for performance reasons...
		double maxTimeStampAllowed = -1;
		double myMinTimeStamp = -1;
		double otherThreadMinTimeStamp = -1;

		// as we are not using syncrhonization, a null pointer exception might
		// happen...
		try {
			if (inLowerThreadCurrently) {
				if (queueThread1.peek() != null) {
					myMinTimeStamp = queueThread1.peek()
							.getMessageArrivalTime();
				}
				if (queueThread2.peek() != null) {
					otherThreadMinTimeStamp = queueThread2.peek()
							.getMessageArrivalTime();
				}
			} else {
				if (queueThread2.peek() != null) {
					myMinTimeStamp = queueThread2.peek()
							.getMessageArrivalTime();
				}
				if (queueThread1.peek() != null) {
					otherThreadMinTimeStamp = queueThread1.peek()
							.getMessageArrivalTime();
				}
			}
		} catch (Exception e) {
			// just continue with the current values...
		}

		if (otherThreadMinTimeStamp == -1) {
			maxTimeStampAllowed = myMinTimeStamp + maxTimeDelta;
		} else {
			maxTimeStampAllowed = otherThreadMinTimeStamp + maxTimeDelta;
		}

		if (inLowerThreadCurrently) {
			// just give back all messages which are in the allowed time
			// (stamp) range

			while (queueThread1.peek() != null
					&& queueThread1.peek().getMessageArrivalTime() <= maxTimeStampAllowed) {
				list.add(queueThread1.poll());
			}

		} else {

			while (queueThread2.peek() != null
					&& queueThread2.peek().getMessageArrivalTime() <= maxTimeStampAllowed) {
				list.add(queueThread2.poll());
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
	public boolean isListEmptyWitnessedByAll() {
		boolean inLowerThreadCurrently = Thread.currentThread().getId() == idOfLowerThread ? true
				: false;
		synchronized (queueThread1) {
			synchronized (queueThread2) {
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
				return lowerThreadWitnessedEmptyQueue
						&& higherThreadWitnessedEmptyQueue;
			}
		}
	}
}
