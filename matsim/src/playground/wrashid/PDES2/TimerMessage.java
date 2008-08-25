package playground.wrashid.PDES2;

import java.util.Collections;

public class TimerMessage extends SelfhandleMessage {

	@Override
	public void selfhandleMessage() {
		Road road = (Road) receivingUnit;
		
		if (road.lookahead.size()==1){
			// this means, that the last timer (+ inf) is invoked
			//System.out.print(".");
			return;
		}
		
		road.lookahead.remove(this);
		
		
		
		
		if (road.lookahead.peek().messageArrivalTime<messageArrivalTime){
			road.scheduleNextZoneBorderMessage(messageArrivalTime);
		} else {
			road.scheduleZoneBorderMessage(road.lookahead.peek());
		}
		
	}

	@Override
	public void printMessageLogString() {

	}

	
	


}
