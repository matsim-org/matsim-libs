package playground.wrashid.PDES1;

public class EnterRequestMessage extends SelfhandleMessage {

	Vehicle vehicle;
	
	@Override
	public void selfhandleMessage() {
		
		System.out.println("enter Request message");
		
		Vehicle vehicle=(Vehicle) sendingUnit;
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
