package playground.wrashid.PDES2;

public class EnterRequestMessage extends SelfhandleMessage {

	Vehicle vehicle;
	
	@Override
	public void selfhandleMessage() {
		
		System.out.println("enter Request message");
		
		Vehicle vehicle=this.vehicle;
		Road toRoad=(Road) receivingUnit;
		
		toRoad.enterRequest(vehicle);
	}

	@Override
	public void printMessageLogString() {
		// TODO Auto-generated method stub
		
	}
	
	public EnterRequestMessage(Vehicle vehicle){
		super();
		this.vehicle=vehicle;
	}

	@Override
	public void recycleMessage() {
		MessageFactory.disposeEnterRequestMessage(this);
	}

	
	
	
}
