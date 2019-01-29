package org.matsim.core.mobsim.jdeqsim.util;

import org.matsim.core.mobsim.jdeqsim.Message;
import org.matsim.core.mobsim.jdeqsim.Scheduler;
import org.matsim.core.mobsim.jdeqsim.SimUnit;

public class DummySimUnit extends SimUnit{

	public DummySimUnit(Scheduler scheduler) {
		super(scheduler);
	}

	public void handleMessage(Message m) {
	}

}
