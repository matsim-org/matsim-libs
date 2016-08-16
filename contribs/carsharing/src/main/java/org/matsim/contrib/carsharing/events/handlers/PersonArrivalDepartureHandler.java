package org.matsim.contrib.carsharing.events.handlers;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.CSPersonVehicle;
import org.matsim.contrib.carsharing.manager.CarsharingManager;
import org.matsim.contrib.carsharing.qsim.CarSharingVehiclesNew;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;

import com.google.inject.Inject;
/** 
 * 
 * @author balac
 */
public class PersonArrivalDepartureHandler implements PersonDepartureEventHandler, PersonLeavesVehicleEventHandler, 
	PersonArrivalEventHandler, PersonEntersVehicleEventHandler {
	public static final String STAGE_FF = "ff_interaction";
	public static final String STAGE_OW = "ow_interaction";

	@Inject	private CarsharingManager carsharingManager;
	@Inject private CSPersonVehicle csPersonVehicles;
	@Inject private CarSharingVehiclesNew carsharingStationsData;

	Map<Id<Person>, String> personVehicleArrival = new HashMap<Id<Person>, String>();
	Map<Id<Person>, Id<Link>> personArrivalMap = new HashMap<Id<Person>, Id<Link>>();
	
	@Override
	public void reset(int iteration) {
		personVehicleArrival = new HashMap<Id<Person>, String>();
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String legMode = event.getLegMode();
		if (legMode.equals("egress_walk_ff")) {
			String vehId = personVehicleArrival.get(event.getPersonId());
			Id<Link> linkId = personArrivalMap.get(event.getPersonId());

			carsharingManager.returnCarsharingVehicle(event.getPersonId(), event.getLinkId(), event.getTime(), vehId);
			this.csPersonVehicles.getVehicleLocationForType(event.getPersonId(), "freefloating").remove(linkId);

		}
		else if (legMode.equals("egress_walk_ow")) {
			String vehId = personVehicleArrival.get(event.getPersonId());
			Id<Link> linkId = personArrivalMap.get(event.getPersonId());

			carsharingManager.returnCarsharingVehicle(event.getPersonId(), event.getLinkId(), event.getTime(), vehId);
			this.csPersonVehicles.getVehicleLocationForType(event.getPersonId(), "oneway").remove(linkId);
			
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		String vehId = event.getVehicleId().toString();
		if (vehId.startsWith("FF") || 
				vehId.startsWith("OW")) {			
			
			personVehicleArrival.put(event.getPersonId(), vehId);
			String type = "";
			if (vehId.startsWith("FF"))
				type = "freefloating";
			else
				type = "oneway";
			CSVehicle vehicle = this.carsharingStationsData.getFfvehicleIdMap().get(vehId);
			
			Id<Link> linkId = personArrivalMap.get(event.getPersonId());
			if (this.csPersonVehicles.getVehicleLocationForType(event.getPersonId(), type) != null)
				this.csPersonVehicles.getVehicleLocationForType(event.getPersonId(), type).put(linkId, vehicle);
			else {				
				this.csPersonVehicles.addNewPersonInfo(event.getPersonId());
				this.csPersonVehicles.getVehicleLocationForType(event.getPersonId(), type).put(linkId, vehicle);

			}
			
		}
	}	

	@Override
	public void handleEvent(PersonArrivalEvent event) {

		personArrivalMap.put(event.getPersonId(), event.getLinkId());
		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {

		String vehId = event.getVehicleId().toString();
		if (vehId.startsWith("OW")) {
			Id<Link> linkId = personArrivalMap.get(event.getPersonId());

			this.carsharingManager.freeParkingSpot(linkId);
		
		}
	}
	
}
