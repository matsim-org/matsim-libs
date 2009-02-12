package org.matsim.mobsim.jdeqsim.util;

import org.matsim.mobsim.jdeqsim.Message;

public class DummyMessage1 extends Message {

	public Message messageToUnschedule=null;
	
	public DummyMessage1(){
		super();
	}
	

	@Override
	public void handleMessage() {
		this.getSendingUnit().getScheduler().unschedule(messageToUnschedule);
	}

	@Override
	public void processEvent() {
		// TODO Auto-generated method stub
		
	}
	
}
