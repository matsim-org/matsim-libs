package playground.wrashid.PDES2;

public class EnterRequestMessage extends EventMessage {
	
	@Override
	public void selfhandleMessage() {
		
		//System.out.println("enter Request message");
		
		
		
		Vehicle vehicle=this.vehicle;
		Road toRoad=(Road) receivingUnit;
		
		
		//System.out.println("enter request road: " + toRoad.getLink().getId() + "; vehicle: " + vehicle.getOwnerPerson().getId());
		//System.out.println(messageId);
		synchronized (toRoad){
			//toRoad.simTime=messageArrivalTime;
			toRoad.enterRequest(vehicle,messageArrivalTime);
		}
	}

	
	
	public EnterRequestMessage(Scheduler scheduler,Vehicle vehicle){
		super(scheduler,vehicle);
		eventType="";
		logMessage=false;
		assert(vehicle!=null);
		assert(this.vehicle!=null);
	}
	


	@Override
	public void logEvent() {
		// TODO Auto-generated method stub
		
	}

	
	
	
}
