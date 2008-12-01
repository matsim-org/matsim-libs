package playground.wrashid.DES;

public class DeadlockPreventionMessage extends EventMessage {

	@Override
	// let enter the car into the road immediatly
	public void selfhandleMessage() {
		
		Road road=(Road)scheduler.getSimUnit(this.getReceivingUnit().unitNo);
		
		road.incrementPromisedToEnterRoad(); // this will be decremented in enter road
		road.setTimeOfLastEnteringVehicle(scheduler.simTime);
		road.removeFirstDeadlockPreventionMessage(this);
		road.removeFromInterestedInEnteringRoad();
		
		vehicle.scheduleEnterRoadMessage(scheduler.simTime, road);
		//System.out.println("Deadlock prevention happend");
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
