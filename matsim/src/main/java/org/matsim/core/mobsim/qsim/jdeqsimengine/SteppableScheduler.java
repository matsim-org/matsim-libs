package org.matsim.core.mobsim.qsim.jdeqsimengine;

import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.mobsim.jdeqsim.Message;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.jdeqsim.Scheduler;

import javax.inject.Inject;

public class SteppableScheduler extends Scheduler implements Steppable {

	private Message lookahead;
	private boolean finished = false;

	@Inject
	public SteppableScheduler(MessageQueue queue) {
		super(queue);
	}

	@Override
	public void doSimStep(double time) {
		finished = false; // I don't think we can restart once the queue has run dry, but just in case.

		// "lookahead" is, I think, just a cache of the next message in the queue, to avoid having to retreive it again.
		// yyyy looks like a potential bug to me if some other message gets inserted with an earlier message arrival time?  kai, feb'19
		if (lookahead != null && time < lookahead.getMessageArrivalTime()) {
			return;
		}
		if (lookahead != null) {
			lookahead.processEvent();
			lookahead.handleMessage();
			lookahead = null;
		}
		while (!queue.isEmpty()) {
			Message m = queue.getNextMessage();
			if (m != null && m.getMessageArrivalTime() <= time) {
				m.processEvent();
				m.handleMessage();
			} else {
				lookahead = m;
				return;
			}
		}
		finished = true; // queue has run dry.
	}

	public boolean isFinished() {
		return finished;
	}

}
