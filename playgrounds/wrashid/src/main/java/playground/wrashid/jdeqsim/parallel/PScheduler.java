package playground.wrashid.jdeqsim.parallel;

import java.util.concurrent.CyclicBarrier;

import org.matsim.core.mobsim.jdeqsim.Scheduler;

public class PScheduler extends Scheduler {

	public PScheduler(PMessageQueue queue) {
		super(queue);
	}

	@Override
	public void startSimulation() {
		CyclicBarrier cb = new CyclicBarrier(3);
		MessageExecutor[] messageExecutors = new MessageExecutor[2];
		messageExecutors[0] = new MessageExecutor(0, this, cb);
		messageExecutors[1] = new MessageExecutor(1, this, cb);
		if (messageExecutors[0].getId() < messageExecutors[1].getId()) {
			getQueue().idOfLowerThread = messageExecutors[0].getId();
		} else {
			getQueue().idOfLowerThread = messageExecutors[1].getId();
		}


		messageExecutors[0].start();
		messageExecutors[1].start();

		// System.out.println(messageExecutors[0].getId());
		// System.out.println(messageExecutors[1].getId());

		try {
			cb.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (java.util.concurrent.BrokenBarrierException e) {
			e.printStackTrace();
		}

	}

	public PMessageQueue getQueue() {
		return (PMessageQueue) queue;
	}
}
