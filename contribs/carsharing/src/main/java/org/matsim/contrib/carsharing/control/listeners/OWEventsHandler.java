package org.matsim.contrib.carsharing.control.listeners;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class OWEventsHandler implements PersonLeavesVehicleEventHandler, PersonEntersVehicleEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler, LinkLeaveEventHandler{
	private HashMap<Id<Person>, RentalInfoOW> owRentalsStats = new HashMap<Id<Person>, RentalInfoOW>();
	private ArrayList<RentalInfoOW> arr = new ArrayList<RentalInfoOW>();
	private HashMap<Id<Person>, Boolean> inVehicle = new HashMap<Id<Person>, Boolean>();
	private HashMap<Id<Vehicle>, Id<Person>> personVehicles = new HashMap<Id<Vehicle>, Id<Person>>();

	private Network network;
	public OWEventsHandler(Network network) {
		
		this.network = network;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		owRentalsStats = new HashMap<Id<Person>, RentalInfoOW>();
		arr = new ArrayList<RentalInfoOW>();
		inVehicle = new HashMap<Id<Person>, Boolean>();
		personVehicles = new HashMap<Id<Vehicle>, Id<Person>>();

	}
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// TODO Auto-generated method stub
		personVehicles.remove(event.getVehicleId());
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// TODO Auto-generated method stub
		if (event.getVehicleId().toString().startsWith("OW"))
			personVehicles.put(event.getVehicleId(), event.getPersonId());
	}
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getVehicleId().toString().startsWith("OW")) {
			Id<Person> perid = personVehicles.get(event.getVehicleId());
			
			RentalInfoOW info = owRentalsStats.get(perid);
			info.vehId = event.getVehicleId();
			info.distance += network.getLinks().get(event.getLinkId()).getLength();
			
		}
		
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// TODO Auto-generated method stub
		inVehicle.put(event.getPersonId(), false);
		if (event.getLegMode().equals("walk_ow_sb")) {
			
			RentalInfoOW info = new RentalInfoOW();
			info.accessStartTime = event.getTime();
			info.personId = event.getPersonId();
			
			if (owRentalsStats.get(event.getPersonId()) == null) {
				
				owRentalsStats.put(event.getPersonId(), info);

			}
			else {
				
				RentalInfoOW info1 = owRentalsStats.get(event.getPersonId());
				info1.egressStartTime = event.getTime();				
				
			}
			
		}
		else if (event.getLegMode().equals("onewaycarsharing")) {
			inVehicle.put(event.getPersonId(), true);

			RentalInfoOW info = owRentalsStats.get(event.getPersonId());
			
			info.startTime = event.getTime();
			info.startLinkId = event.getLinkId();
			info.accessEndTime = event.getTime();
		}
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		// TODO Auto-generated method stub
		
		if (event.getLegMode().equals("onewaycarsharing")) {
			RentalInfoOW info = owRentalsStats.get(event.getPersonId());
			info.endTime = event.getTime();
			info.endLinkId = event.getLinkId();
			
		}
		else if (event.getLegMode().equals("walk_ow_sb")) {
			if (owRentalsStats.get(event.getPersonId()) != null && owRentalsStats.get(event.getPersonId()).accessEndTime != 0.0) {
				RentalInfoOW info = owRentalsStats.remove(event.getPersonId());
				info.egressEndTime = event.getTime();
				arr.add(info);	
			}
		}
		
	}
	
	public ArrayList<RentalInfoOW> rentals() {
		
		return arr;
	}
	
	public class RentalInfoOW {
		private Id<Person> personId = null;
		private double startTime = 0.0;
		private double endTime = 0.0;
		private Id<Link> startLinkId = null;
		private Id<Link> endLinkId = null;
		private double distance = 0.0;
		private double accessStartTime = 0.0;
		private double accessEndTime = 0.0;
		private double egressStartTime = 0.0;
		private double egressEndTime = 0.0;
		private Id<Vehicle> vehId = null;

		public String toString() {
			
			return personId + " " + Double.toString(startTime) + " " + Double.toString(endTime) + " " +
			startLinkId.toString() + " " +	endLinkId.toString()+ " " + Double.toString(distance)+ " " +
					Double.toString(accessEndTime - accessStartTime)+ " " + Double.toString(egressEndTime - egressStartTime) + " " +
			vehId;
		}
	}

}
