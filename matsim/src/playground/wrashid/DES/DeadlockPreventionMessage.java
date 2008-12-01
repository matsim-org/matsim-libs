package playground.wrashid.DES;

public class DeadlockPreventionMessage extends EventMessage {

	@Override
	// let enter the car into the road immediatly
	public void handleMessage() {
		
		Road road=(Road)this.getReceivingUnit();
		
		road.incrementPromisedToEnterRoad(); // this will be decremented in enter road
		road.setTimeOfLastEnteringVehicle(scheduler.getSimTime());
		road.removeFirstDeadlockPreventionMessage(this);
		road.removeFromInterestedInEnteringRoad();
		
		vehicle.scheduleEnterRoadMessage(scheduler.getSimTime(), road);
		//System.out.println("Deadlock prevention happend");
	}



	public DeadlockPreventionMessage(Scheduler scheduler,Vehicle vehicle) {
		super(scheduler,vehicle);
	}
	
	public void processEvent() {
		// don't do anything
	}

}
