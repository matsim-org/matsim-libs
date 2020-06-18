package org.matsim.core.mobsim.jdeqsim.util;

import org.matsim.core.mobsim.jdeqsim.Message;

public class DummyMessage extends Message {

	public DummyMessage() {
		super(null);
	}

	@Override
	public void handleMessage() {
	}

	@Override
	public void processEvent() {
	}

}
