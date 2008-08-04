package playground.wrashid.PDES1;

public class EnterRequestMessage extends SelfhandleMessage {

	@Override
	public void selfhandleMessage() {
		Vehicle vehicle=(Vehicle) sendingUnit;
		Road toRoad=(Road) receivingUnit;
		
		toRoad.enterRequest(vehicle);
	}

	@Override
	public void printMessageLogString() {
		// TODO Auto-generated method stub
		
	}
	
	public EnterRequestMessage(){
		super();
	}

	@Override
	public void recycleMessage() {
		MessageFactory.disposeEnterRequestMessage(this);
	}

	
	
	
}
