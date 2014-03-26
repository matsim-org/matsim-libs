package playground.jbischoff.taxi.evaluation;

import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;

public class TaxiCustomerDebugger implements PersonDepartureEventHandler,
		PersonEntersVehicleEventHandler, PersonArrivalEventHandler {

	public int departures=0;
	public int arrivals=0;
	public int vehicleEnts=0;
	
	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (!event.getPersonId().toString().startsWith("t_"))
		this.arrivals++;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!event.getPersonId().toString().startsWith("t_"))
 		this.vehicleEnts++;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!event.getPersonId().toString().startsWith("t_"))

		this.departures++;
	}

}
