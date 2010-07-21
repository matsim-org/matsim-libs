package playground.wrashid.jdeqsim.parallel;

import java.util.LinkedList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.core.mobsim.jdeqsim.DeadlockPreventionMessage;
import org.matsim.core.mobsim.jdeqsim.EventMessage;
import org.matsim.core.mobsim.jdeqsim.Message;
import org.matsim.core.mobsim.jdeqsim.SimulationParameters;
import org.matsim.core.mobsim.jdeqsim.util.Timer;

/*
 * The locking dependencies of each type of message for ROADS (does road change):
 * - DeadlockPreventionMessage: m.getReceivingUnit()
 * - EnterRoadMessage: vehicle.currentLink
 * - EndLegMessage: nothing (getCurrentLink: for finding out, which thread this message should be assigned to)
 * - EndRoadMessage: nextLink
 * - LeaveRoadMessage: m.getReceivingUnit()
 * - StartingLegMessage: vehicle.currentLink or nextLink
 */

/*
 * For which cases locking the VEHICLE is required when crossing the boundry:
 * (TODO: need to lock the vehicle for that type of Message in boundry region)
 * - DeadlockPreventionMessage: no
 * - EnterRoadMessage: no
 * - EndLegMessage: yes
 * - EndRoadMessage: yes
 * - LeaveRoadMessage: no
 * - StartingLegMessage: yes
 */

/*
 * Message locking only needed for DeadlockPreventionMessage (and check if alive).
 */
public class MessageExecutor extends Thread {

	PScheduler scheduler = null;
	int numberOfMessagesProcessed = 0;
	int missedNumberOfLocks = 0;
	int id;
	private CyclicBarrier barrier;

	public MessageExecutor(int id, PScheduler scheduler, CyclicBarrier cb) {
		this.id = id;
		this.scheduler = scheduler;
		this.barrier = cb;
	}

	@Override
	public void run() {

		Timer timer = new Timer();
		timer.startTimer();

		// System.out.println(Thread.currentThread().getId());
		processMessages();

		System.out.println("fertig:" + id + " - " + numberOfMessagesProcessed);
		System.out.println("missed locks:" + id + " - " + missedNumberOfLocks);

		timer.endTimer();
		timer.printMeasuredTime("time needed[ms]: ");

		try {
			barrier.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void processMessages() {
		Message m;
		LinkedList<Message> list = new LinkedList<Message>();
		while (!(scheduler.getQueue().isEmpty() && scheduler.getQueue()
				.isListEmptyWitnessedByAll())
				&& scheduler.getSimTime() < SimulationParameters
						.getSimulationEndTime()) {
			list = scheduler.getQueue().getNextMessages(list);
			// m=scheduler.queue.getNextMessage();

			// if (m != null) {

			if (list.size() == 0) {

				waitingOnMessages();

			}

			// process all messages received
			while (list.size() > 0) {
				m = list.poll();

				// m.processEvent();

				// if
				// (((EventMessage)m).vehicle.getCurrentLink().getCoord().getX()>
				// SimulationParameters.networkXMedian && id==0){
				// m.handleMessage();
				// } else {
				// messageExecutors[0].queueMessageForExecution(m);
				// }

				// make sure, that deadlock messages are upto date...

				// TODO: synchronization of message only needed, when it is of
				// type Deadlock prevention message...

				// TODO: do fair partitioning of network (at the moment most
				// events are in the same area...)
				PVehicle vehicle = (PVehicle) ((EventMessage) m).vehicle;

				ExtendedRoad receivingRoad = (ExtendedRoad) m
						.getReceivingUnit();

				// Note: synchronizing over the message is needed both for the
				// DeadlockPreventionMessage and LeaveRoadMessage

				// the deadlock message must lock the road before starting
				// processing, because
				// else a leave road message might remove it (or the other way
				// arround: deadlock message has been processed
				// while someone sees concurrent old state...

				// we need to synchronize on currentRoad, nextRoad and previous
				// road, because
				// not in all handlers just the receivingUnit/road is
				// manupulated

				if (receivingRoad.isBorderZone()) {

					// TODO: the following are quite expensive operations (are
					// they?)
					// some of them can be left out (only some are really needed
					// for each type of message).
					/*
					ExtendedRoad currentRoad = receivingRoad;
					ExtendedRoad nextRoad = receivingRoad;
					ExtendedRoad prevRoad = receivingRoad;
					Link tempLink = null;

					// the ordering of this sequence is according to the
					// frequency of the messages
					if (m instanceof EnterRoadMessage) {

						tempLink = vehicle.getCurrentLink();
						if (tempLink != null) {
							currentRoad = (ExtendedRoad) Road.getRoad(tempLink
									.getId().toString());
						}

						synchronized (currentRoad) {
							m.handleMessage();
						}

					} else if (m instanceof LeaveRoadMessage) {

						synchronized (receivingRoad) {
							m.handleMessage();
						}

					} else if (m instanceof EndRoadMessage) {

						tempLink = vehicle.getCurrentLink();
						if (tempLink != null) {
							currentRoad = (ExtendedRoad) Road.getRoad(tempLink
									.getId().toString());
						}

						tempLink = vehicle.getNextLinkInLeg();
						if (tempLink != null) {
							nextRoad = (ExtendedRoad) Road.getRoad(tempLink
									.getId().toString());
						}

						synchronized (nextRoad) {
							synchronized (vehicle) {
								m.handleMessage();
							}

						}
					} else if (m instanceof DeadlockPreventionMessage) {
						synchronized (receivingRoad) {
							synchronized (m) {
								if (m.isAlive()) {
									m.handleMessage();
								}
							}
						}
					} else if (m instanceof StartingLegMessage) {
						tempLink = vehicle.getCurrentLink();
						if (tempLink != null) {
							currentRoad = (ExtendedRoad) Road.getRoad(tempLink
									.getId().toString());
						}

						tempLink = vehicle.getNextLinkInLeg();
						if (tempLink != null) {
							nextRoad = (ExtendedRoad) Road.getRoad(tempLink
									.getId().toString());
						}

						synchronized (currentRoad) {
							synchronized (nextRoad) {
								synchronized (vehicle) {
									m.handleMessage();
								}
							}
						}
					} else if (m instanceof EndLegMessage) {
						synchronized (vehicle) {
							m.handleMessage();
						}
					} else {
						// there are no other type of messages which can come
						// here...
						assert (false);
					}
*/
					// WE MUST LOCK THE VEHICLE ALWAYS DURING CROSSING THE BORDER
					// ONE TIME ON THIS SIDE OF THE BORDER AND THE SECOND TIME ON THE
					// OTHER SIDE OF THE BORDER
					if (m instanceof DeadlockPreventionMessage) {
						synchronized (vehicle) {
							synchronized (m) {
								if (m.isAlive()) {
									//m.processEvent();
									m.handleMessage();
								}
							}
						}
					} else {
						synchronized (vehicle) {
							//m.processEvent();
							m.handleMessage();
						}
					}


				} else {

					if (m.isAlive()) {
						//m.processEvent();
						m.handleMessage();
					}

				}

				numberOfMessagesProcessed++;

				// synchronized (m) {
				// if (m.isAlive()) {

				// LinkImpl nextLink = vehicle.getNextLinkInLeg();
				// LinkImpl previousLink =
				// vehicle.getPreviousLinkInLeg();

				// if next link exists for this route, than also
				// lock
				// that
				// link

				// attention...
				// turn this on again, if this does not work...
				// if (nextLink != null) {
				// Road nextRoad = Road.getRoad(nextLink.getId().toString());
				// TODO: if this functions, then try if it
				// functions
				// also, if we do this only EndRoadMessage
				// TODO: can we just lock only receiving
				// unit
				// and
				// not
				// road?

				// TODO: perhaps we only need to lock the
				// road,
				// if
				// we
				// are in the border area...

				// TODO: many of the locks might be removed,
				// because the main problem was the locking
				// of
				// deadlock messages,
				// which causeed problems, for which many of
				// these locks were introduced...

				// Notes: we need to lock the receiving
				// unit,
				// because else there is a race condition
				// because
				// of deadlock prevention message...
				// synchronized (nextRoad) {
				// lock road

				// lock next road
				// lock vehicle
				// synchronized (vehicle) {

				// synchronized (previousLink) {
				// m.handleMessage();
				// }

				// }

				// }

				// } else {
				// lock road

				// lock vehicle
				// synchronized (vehicle) {

				// m.handleMessage();

				// }

				// }
				// }
			}

			// }
		}


	}

	private void waitingOnMessages(){
		// TODO: probably needed later again for satawal
		// This is extremly important!!!!!!!!!!!! => even on local
		// computer it makes performance much better

		// just wait for some time...
		// this is really some option, with which one can play with...
		// => it really gives improvement...

		// => this parameter can reduce the number of unnecessary locks
		// but as with the new implementation with two separate locks
		// instead of just
		// one in the queue, the advantage of this is not given
		// anymore...
		//
		// even with new form of locking, this is quite important!!!!
		// changing the computation size has huge effect!!!
		// TUNING NEEDED of this parameter...
		// both too low and two high parameter cause lots of empty lists back. make it optimal, so that you have
		// a minimum of missed locks/empty lists...
		for (int i = 0; i < 8000; i++) {

		}
		// experiments on local compi of the parameter:
		// timing of 10% zh.
		// value: 1000, time: 17.6 sec.
		// value: 5000, time: 16.7 sec.
		// value: 10000, time: 17.1 sec.
		// value: 20000, time: 16.6 sec.
		// value: 30000, time: 17.3 sec.
		// it is not fully correlated with the number of missed locks... (good time does not imply automatically low number of missed locks...)


		missedNumberOfLocks++;
	}
}
