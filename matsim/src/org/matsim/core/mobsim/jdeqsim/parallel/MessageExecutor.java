package org.matsim.core.mobsim.jdeqsim.parallel;

import java.util.LinkedList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.core.events.parallelEventsHandler.ConcurrentListSPSC;
import org.matsim.core.mobsim.jdeqsim.DeadlockPreventionMessage;
import org.matsim.core.mobsim.jdeqsim.EventMessage;
import org.matsim.core.mobsim.jdeqsim.Message;
import org.matsim.core.mobsim.jdeqsim.Road;
import org.matsim.core.mobsim.jdeqsim.SimulationParameters;
import org.matsim.core.mobsim.jdeqsim.Vehicle;
import org.matsim.core.mobsim.jdeqsim.util.Timer;
import org.matsim.core.network.LinkImpl;

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
				.isEmptySync())
				&& scheduler.getSimTime() < SimulationParameters
						.getSimulationEndTime()) {
			list = scheduler.getQueue().getNextMessages(list);
			// m=scheduler.queue.getNextMessage();

			// if (m != null) {

			if (list.size() == 0) {

				// TODO: probably needed later again for satawal
				// This is extremly important!!!!!!!!!!!! => even on local
				// computer it makes performance much better

				// just wait for some time...
				// this is really some option, with which one can play with...
				// => it really gives improvement...
				for (int i = 0; i < 100000; i++) {

				}

				missedNumberOfLocks++;

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
				Vehicle vehicle = ((EventMessage) m).vehicle;
				Road road = Road.getRoad(vehicle.getCurrentLink().getId()
						.toString());

				// Note: synchronizing over the message is needed both for the
				// DeadlockPreventionMessage and LeaveRoadMessage

				if (((ExtendedRoad) m.getReceivingUnit()).isBorderZone()) {
					synchronized (m.getReceivingUnit()) {
						// synchronized (road) {

						// TODO: probably it is faster to just synchronize,
						// rather than checking fist => check with bigger
						// scenario
						// perhaps satawal reacts differently on this....

						synchronized (m) {
							if (m.isAlive()) {
								m.handleMessage();
							}
						}

					}
				} else {
					// TODO: probably it is faster to just synchronize, rather
					// than checking fist => check with bigger scenario
					// perhaps satawal reacts differently on this....
					synchronized (m) {
						if (m.isAlive()) {
							m.handleMessage();
						}
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

}
