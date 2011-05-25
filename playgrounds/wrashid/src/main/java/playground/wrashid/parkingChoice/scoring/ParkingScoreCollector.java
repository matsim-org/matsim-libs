package playground.wrashid.parkingChoice.scoring;

import playground.wrashid.parkingChoice.events.ParkingArrivalEvent;
import playground.wrashid.parkingChoice.events.ParkingDepartureEvent;
import playground.wrashid.parkingChoice.handler.ParkingArrivalEventHandler;
import playground.wrashid.parkingChoice.handler.ParkingDepartureEventHandler;

//TODO: I could just collect the walking distances/time which needs to be deduced from score here.
//TODO: => make use of this wisly, as this is invoked a lot of times...
//TODO: in a similar handler I could log all the data during the simulation

public class ParkingScoreCollector implements ParkingArrivalEventHandler,ParkingDepartureEventHandler {

	
	
	@Override
	public void handleEvent(ParkingArrivalEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ParkingDepartureEvent event) {
		// TODO Auto-generated method stub
		
	}

}
