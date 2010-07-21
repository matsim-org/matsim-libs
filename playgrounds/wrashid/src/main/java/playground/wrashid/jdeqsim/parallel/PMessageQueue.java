package playground.wrashid.jdeqsim.parallel;

import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.mobsim.jdeqsim.DeadlockPreventionMessage;
import org.matsim.core.mobsim.jdeqsim.EndLegMessage;
import org.matsim.core.mobsim.jdeqsim.EndRoadMessage;
import org.matsim.core.mobsim.jdeqsim.EnterRoadMessage;
import org.matsim.core.mobsim.jdeqsim.EventMessage;
import org.matsim.core.mobsim.jdeqsim.LeaveRoadMessage;
import org.matsim.core.mobsim.jdeqsim.Message;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.jdeqsim.Road;
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
	// this is quite optimal, not much more optimization possible...
	// THIS PARAMETER CAN BE TUNED, but no tuning needed at the moment.
	// good value (at least for home compi): 10 seconds
	// E.g. making this parameter 100000 would make the simulation extremly fast
	// (fully parallel)
	// but the results are a bit of rubish probably...

	// in my personal opinion, a randomness within 1 to 5/10 minutes shouldn't be a problem,
	// if we think about the rest of MATSim (we can't model each person anyway that he is first and
	// the other is second - why it didn't happen as due to randomness?).

	// already putting it to 10min/600sec gives very good performance...

	// question: how does the output look in the case when we put it that high? does correction happen automatically
	// along the links (it should because of the congestions, because the time for that is given by the vehicle in front)
	// so it might be, this has even a smaller effect.
	// in the empty network a big delta doesn't anyway cause problems....

	// along the border it could cause some bad influence...
	private double maxTimeDelta = 10;

	// all events after 24 hours should be process in a faster way
	// (take maxTimeDelta effect away)
	private boolean secondDayStarted = false;

	public boolean lowerThreadWitnessedEmptyQueue = false;
	public boolean higherThreadWitnessedEmptyQueue = false;

	// THIS gives also some improvement in time...
	private static final int SECONDS_IN_DAY=86400;

	// how many times is it the case, that the process can't progress
	// although, it has messages in the queue (but it does not return them).
	private int thread1_TimesCantProgressBecauseOfMaxDelta=0;

	/**
	 *
	 * Putting a message into the queue
	 *
	 * @param m
	 */
	@Override
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
		Id tempLinkId = null;

		// the ordering of this sequence is according to the
		// frequency of the messages
		if (m instanceof EnterRoadMessage) {

			tempLinkId = vehicle.getCurrentLinkId();
			if (tempLinkId != null) {
				currentRoad = (ExtendedRoad) Road.getRoad(tempLinkId);
			}

			messageTargetRoad = currentRoad;

		} else if (m instanceof LeaveRoadMessage) {

			messageTargetRoad = receivingRoad;

		} else if (m instanceof EndRoadMessage) {

			tempLinkId = vehicle.getNextLinkInLeg();
			if (tempLinkId != null) {
				nextRoad = (ExtendedRoad) Road.getRoad(tempLinkId);
			}

			messageTargetRoad = nextRoad;
		} else if (m instanceof DeadlockPreventionMessage) {
			messageTargetRoad = receivingRoad;
		} else if (m instanceof StartingLegMessage) {

			if (vehicle.getCurrentLeg().getMode().equals(TransportMode.car)) {
				tempLinkId = vehicle.getCurrentLinkId();
				if (tempLinkId != null) {
					currentRoad = (ExtendedRoad) Road.getRoad(tempLinkId);
				}

				messageTargetRoad = currentRoad;
			} else {
				// TODO: we need the first link in the next leg. if this does
				// not function
				// then we need to do this manually.
				tempLinkId = vehicle.getNextLinkInLeg();
				if (tempLinkId != null) {
					nextRoad = (ExtendedRoad) Road.getRoad(tempLinkId);
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
		// Profiling shows: This strategy really removes blocking of the
		// threads...
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
	@Override
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
	@Override
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
	 * TODO: Probably we could switch back to getNextMessage now, as the locking
	 * logic has changed now... => not really oder?
	 */
	/**
	 * Especially if the network is almost empty, the simulation might slow down
	 * because of small max time delta. TODO: make this better.
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

		if (!secondDayStarted) {

			// as we are not using syncrhonization, a null pointer exception
			// might
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

			// allow each process to operate separatly now...
			// THIS gives also some improvement
			if (maxTimeStampAllowed>SECONDS_IN_DAY){
				secondDayStarted=true;
			}
		} else {
			maxTimeStampAllowed=Integer.MAX_VALUE;
		}

		if (inLowerThreadCurrently) {
			// just give back all messages which are in the allowed time
			// (stamp) range

			while (queueThread1.peek() != null
					&& queueThread1.peek().getMessageArrivalTime() <= maxTimeStampAllowed) {
				list.add(queueThread1.poll());
			}

			if (list.size()==0 && queueThread1.size()!=0){
				thread1_TimesCantProgressBecauseOfMaxDelta++;

//				if (thread1_TimesCantProgressBecauseOfMaxDelta>1000){
					//System.out.println();
					// this is really often the case...
//				}
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
	@Override
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

	public double getMaxTimeDelta() {
		return maxTimeDelta;
	}

	public void setMaxTimeDelta(double maxTimeDelta) {
		this.maxTimeDelta = maxTimeDelta;
	}
}
