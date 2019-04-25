package org.matsim.core.mobsim.jdeqsim.util;

import org.matsim.core.mobsim.jdeqsim.Message;

public class DummyMessage1 extends Message {

	public Message messageToUnschedule=null;

	@Override
	public void handleMessage() {
		this.getReceivingUnit().getScheduler().unschedule(messageToUnschedule);
	}

	@Override
	public void processEvent() {
	}

}
