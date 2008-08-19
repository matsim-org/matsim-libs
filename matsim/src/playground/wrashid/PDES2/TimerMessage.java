package playground.wrashid.PDES2;

public class TimerMessage extends SelfhandleMessage {

	@Override
	public void selfhandleMessage() {
		Road road = (Road) receivingUnit;
		road.scheduleNextZoneBorderMessage(messageArrivalTime);
	}

	@Override
	public void printMessageLogString() {

	}

	
	


}
