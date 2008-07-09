package playground.wrashid.DES;

public abstract class EventMessage extends SelfhandleMessage {
	Vehicle vehicle;
	String eventType="";
	Scheduler scheduler;
	boolean logMessage=true;
	
	public EventMessage(Scheduler scheduler,Vehicle vehicle) {
		super();
		this.vehicle = vehicle;
		this.scheduler=scheduler;
	}
	
	public void printMessageLogString() {
		//System.out.println("arrivalTime="+this.getMessageArrivalTime() + "; VehicleId=" + vehicle.getOwnerPerson().getId().toString() + "; LinkId=" + vehicle.getCurrentLink().getId().toString() + "; Description=" + eventType );
		if (logMessage){
			SimulationParameters.eventOutputLog.add(new EventLog(this.getMessageArrivalTime(),Integer.parseInt(vehicle.getOwnerPerson().getId().toString()),vehicle.getLegIndex()-1,Integer.parseInt(vehicle.getCurrentLink().getId().toString()),Integer.parseInt(vehicle.getCurrentLink().getFromNode().getId().toString()),Integer.parseInt(vehicle.getCurrentLink().getToNode().getId().toString()),eventType));
		}
	}
	
	public static void printLogMessage(double messageArrivalTime, int vehicleId, int legIndex, int currentLinkId, int fromNodeId, int toNodeId, String eventType) {
		//System.out.println("arrivalTime="+this.getMessageArrivalTime() + "; VehicleId=" + vehicle.getOwnerPerson().getId().toString() + "; LinkId=" + vehicle.getCurrentLink().getId().toString() + "; Description=" + eventType );
			SimulationParameters.eventOutputLog.add(new EventLog(messageArrivalTime,vehicleId,legIndex,currentLinkId,fromNodeId,toNodeId,eventType));
	}
	

}
