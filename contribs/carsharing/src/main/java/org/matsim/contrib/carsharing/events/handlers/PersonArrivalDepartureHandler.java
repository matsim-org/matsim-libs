package org.matsim.contrib.carsharing.events.handlers;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.events.EndRentalEvent;
import org.matsim.contrib.carsharing.manager.CarsharingManagerNew;
import org.matsim.contrib.carsharing.manager.demand.CurrentTotalDemand;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyContainer;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;
/** 
 * 
 * @author balac
 */
public class PersonArrivalDepartureHandler implements PersonDepartureEventHandler, PersonLeavesVehicleEventHandler, 
	PersonArrivalEventHandler, PersonEntersVehicleEventHandler {	

	@Inject	private CarsharingManagerNew carsharingManager;
	//@Inject private CSPersonVehicle csPersonVehicles;
	@Inject private CurrentTotalDemand currentDemand;
	@Inject private CarsharingSupplyContainer carsharingSupply;
	@Inject EventsManager eventsManager;
	@Inject Scenario scenario;
	
	Map<Id<Person>, String> personVehicleArrival = new HashMap<Id<Person>, String>();
	Map<Id<Person>, Id<Link>> personArrivalMap = new HashMap<Id<Person>, Id<Link>>();
	Map<Id<Person>, Id<Vehicle>> personLeavesVehicleMap = new HashMap<Id<Person>, Id<Vehicle>>();

	@Override
	public void reset(int iteration) {
		personLeavesVehicleMap = new HashMap<Id<Person>, Id<Vehicle>>();
		personArrivalMap = new HashMap<Id<Person>, Id<Link>>();
		personVehicleArrival = new HashMap<Id<Person>, String>();
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Network network = scenario.getNetwork();
		String legMode = event.getLegMode();
		if (legMode.equals("egress_walk_ff")) {
			String vehId = personLeavesVehicleMap.get(event.getPersonId()).toString();
			Id<Link> linkId = personArrivalMap.get(event.getPersonId());
			carsharingManager.parkVehicle(vehId, linkId);
			Link link = network.getLinks().get(linkId);
			this.currentDemand.removeVehicle(event.getPersonId(), link, carsharingSupply.getAllVehicles().get(vehId), "freefloating");
			eventsManager.processEvent(new EndRentalEvent(event.getTime(), linkId, event.getPersonId(), vehId));

		}
		else if (legMode.equals("egress_walk_ow")) {
			String vehId = personLeavesVehicleMap.get(event.getPersonId()).toString();
			Id<Link> linkId = personArrivalMap.get(event.getPersonId());
			carsharingManager.parkVehicle(vehId, linkId);
			Link link = network.getLinks().get(linkId);
			this.currentDemand.removeVehicle(event.getPersonId(), link, carsharingSupply.getAllVehicles().get(vehId), "oneway");
			eventsManager.processEvent(new EndRentalEvent(event.getTime(), linkId, event.getPersonId(), vehId));

		}
		else if (legMode.equals("egress_walk_tw")) {
			String vehId = personLeavesVehicleMap.get(event.getPersonId()).toString();
			Id<Link> linkId = personArrivalMap.get(event.getPersonId());
			carsharingManager.parkVehicle(vehId, linkId);
			Link link = network.getLinks().get(linkId);
			this.currentDemand.removeVehicle(event.getPersonId(), link, carsharingSupply.getAllVehicles().get(vehId), "twoway");
			eventsManager.processEvent(new EndRentalEvent(event.getTime(), linkId, event.getPersonId(), vehId));

		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		personLeavesVehicleMap.put(event.getPersonId(), event.getVehicleId());
	
	}	

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Network network = scenario.getNetwork();
		String mode = event.getLegMode();
		String[] modeCut = mode.split("_");
		if (mode.startsWith("free") || 
				mode.startsWith("one") || mode.startsWith("two")) {			
			
			String vehId = personLeavesVehicleMap.get(event.getPersonId()).toString();
			
			CSVehicle vehicle = this.carsharingSupply.getAllVehicles().get(vehId);
			
			Id<Link> linkId = event.getLinkId();
			Link link = network.getLinks().get(linkId);

			this.currentDemand.addVehicle(event.getPersonId(), link, vehicle, modeCut[0]);
		//	if (this.csPersonVehicles.getVehicleLocationForType(event.getPersonId(), modeCut[0]) != null)
		//		this.csPersonVehicles.getVehicleLocationForType(event.getPersonId(), modeCut[0]).put(linkId, vehicle);
		//	else {				
		//		this.csPersonVehicles.getVehicleLocationForType(event.getPersonId(), modeCut[0]).put(linkId, vehicle);

		//	}			
		}		
		personArrivalMap.put(event.getPersonId(), event.getLinkId());
		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {

		String vehId = event.getVehicleId().toString();
		if (vehId.startsWith("OW")) {
			Id<Link> linkId = personArrivalMap.get(event.getPersonId());

			this.carsharingManager.freeParkingSpot(vehId, linkId);
		
		}
	}
	
}
