package playground.wrashid.parkingChoice.priceoptimization.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.parkingchoice.PC2.simulation.ParkingArrivalEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;


public class ModeSplit implements BasicEventHandler {

	public int count = 0;

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(Event event) {

		if (event.getEventType().equalsIgnoreCase(ParkingArrivalEvent.EVENT_TYPE)
				) {
			
			Id<PC2Parking> parkingId = ParkingArrivalEvent.getParkingId(event.getAttributes());
			
			if ( parkingId.toString().contains("gp") ||parkingId.toString().contains("stp") )
				count++;
			
			
		}
		
	}
	
	public static void main(String[] args) {

		EventsManager events = EventsUtils.createEventsManager();	
		
		ModeSplit occ = new ModeSplit();
		events.addHandler(occ); 

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.readFile(args[0]);
		System.out.println(occ.count);
		
	}

}
