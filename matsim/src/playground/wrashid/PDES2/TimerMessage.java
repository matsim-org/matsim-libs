package playground.wrashid.PDES2;

import java.util.Collections;

public class TimerMessage extends SelfhandleMessage {

	@Override
	public void selfhandleMessage() {
		Road road = (Road) receivingUnit;
		road.lookahead.removeAll(Collections.singletonList(this));
		
		if (road.lookahead.peek().messageArrivalTime<messageArrivalTime){
			road.scheduleNextZoneBorderMessage(messageArrivalTime);
		} else {
			road.scheduleZoneBorderMessage(road.lookahead.peek().messageArrivalTime);
		}
		
	}

	@Override
	public void printMessageLogString() {

	}

	
	


}
