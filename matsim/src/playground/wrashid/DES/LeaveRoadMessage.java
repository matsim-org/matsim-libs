package playground.wrashid.DES;

public class LeaveRoadMessage extends EventMessage {

	@Override
	public void selfhandleMessage() {
		Road road=(Road)scheduler.getSimUnit(this.receivingUnit.unitNo);
		road.leaveRoad(vehicle);
	}

	public LeaveRoadMessage(Scheduler scheduler,Vehicle vehicle) {
		super(scheduler,vehicle);
		eventType=SimulationParameters.LEAVE_LINK;
	}
	
	public void printMessageLogString() {
		//System.out.println("arrivalTime="+this.getMessageArrivalTime() + "; VehicleId=" + vehicle.getOwnerPerson().getId().toString() + "; LinkId=" + vehicle.getCurrentLink().getId().toString() + "; Description=" + eventType );
		
		Road road=(Road)scheduler.getSimUnit(this.receivingUnit.unitNo);
		
		if (logMessage){
			SimulationParameters.eventOutputLog.add(new EventLog(this.getMessageArrivalTime(),Integer.parseInt(vehicle.getOwnerPerson().getId().toString()),vehicle.getLegIndex()-1,Integer.parseInt(road.getLink().getId().toString()),Integer.parseInt(vehicle.getCurrentLink().getFromNode().getId().toString()),Integer.parseInt(vehicle.getCurrentLink().getToNode().getId().toString()),eventType));
		}
	}
	
}
