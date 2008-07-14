package playground.wrashid.DES;

public class DeadlockPreventionMessage extends EventMessage {

	@Override
	// let enter the car into the road
	public void selfhandleMessage() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void printMessageLogString() {
		// TODO Auto-generated method stub
		
	}

	public DeadlockPreventionMessage(Scheduler scheduler,Vehicle vehicle) {
		super(scheduler,vehicle);
		eventType="";
		logMessage=false;
	}
	
	public void logEvent() {
		// don't do anything
	}

}
