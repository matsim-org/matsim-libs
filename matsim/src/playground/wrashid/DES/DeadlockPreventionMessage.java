package playground.wrashid.DES;

public class DeadlockPreventionMessage extends EventMessage {

	@Override
	// let enter the car into the road immediatly
	public void handleMessage() {
		
		Road road=(Road)this.getReceivingUnit();
		
		road.incrementPromisedToEnterRoad(); // this will be decremented in enter road
		road.setTimeOfLastEnteringVehicle(getMessageArrivalTime());
		road.removeFirstDeadlockPreventionMessage(this);
		road.removeFromInterestedInEnteringRoad();
		
		vehicle.scheduleEnterRoadMessage(getMessageArrivalTime(), road);
	}



	public DeadlockPreventionMessage(Scheduler scheduler,Vehicle vehicle) {
		super(scheduler,vehicle);
	}
	
	public void processEvent() {
		// don't do anything
	}

}
