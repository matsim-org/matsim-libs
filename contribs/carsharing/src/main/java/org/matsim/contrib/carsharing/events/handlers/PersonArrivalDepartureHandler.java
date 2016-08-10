package org.matsim.contrib.carsharing.events.handlers;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.CarsharingManager;

import com.google.inject.Inject;
/** 
 * 
 * @author balac
 */
public class PersonArrivalDepartureHandler implements PersonDepartureEventHandler, PersonLeavesVehicleEventHandler {

	@Inject
	private CarsharingManager carsharingManager;
	
	Map<Id<Person>, String> personVehicleArrival = new HashMap<Id<Person>, String>();
	
	@Override
	public void reset(int iteration) {
		personVehicleArrival = new HashMap<Id<Person>, String>();
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		
		if (event.getLegMode().equals("egress_walk_ff")) {
			String vehId = personVehicleArrival.get(event.getPersonId());
			carsharingManager.returnCarsharingVehicle(event.getPersonId(), event.getLinkId(), event.getTime(), vehId);
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {

		if (event.getVehicleId().toString().startsWith("FF")) {			
			personVehicleArrival.put(event.getPersonId(), event.getVehicleId().toString());
		}
	}

}
