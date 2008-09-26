package playground.wrashid.PDES2;

public class DeadlockPreventionMessage extends EventMessage {
	
	@Override
	// let enter the car into the road immediatly
	public void selfhandleMessage() {
		if (isAlive()){
			Road road=(Road)this.receivingUnit;
			
			synchronized (road){
				road.incrementPromisedToEnterRoad(); // this will be decremented in enter road
				road.setTimeOfLastEnteringVehicle(messageArrivalTime);
				road.removeDeadlockPreventionMessage(this);
			}
			//road.removeFromInterestedInEnteringRoad(vehicle);
			
			vehicle.scheduleEnterRoadMessage(messageArrivalTime, road);
			
			
			if (vehicle.getOwnerPerson().getId().toString().equalsIgnoreCase("483820")) {
				System.out.println("deadlock prevention: "+road.getLink().getId().toString());
			}
			//System.out.println("Deadlock prevention happend");
		}
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
