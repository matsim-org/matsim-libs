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

public class TwoWayEventsHandler implements  PersonLeavesVehicleEventHandler, PersonEntersVehicleEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler, LinkLeaveEventHandler {

	private HashMap<Id<Person>, ArrayList<RentalInfo>> twRentalsStats = new HashMap<Id<Person>, ArrayList<RentalInfo>>();
	private HashMap<Id<Person>, String> arrivals = new HashMap<Id<Person>, String>();
	private ArrayList<RentalInfo> arr = new ArrayList<RentalInfo>();
	private HashMap<Id<Vehicle>, Id<Person>> personVehicles = new HashMap<Id<Vehicle>, Id<Person>>();
	private Network network;
	public TwoWayEventsHandler(Network network) {
		
		this.network = network;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		twRentalsStats = new HashMap<Id<Person>, ArrayList<RentalInfo>>();
		arrivals = new HashMap<Id<Person>, String>();
		arr = new ArrayList<RentalInfo>();
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
		if (event.getVehicleId().toString().startsWith("TW"))
			personVehicles.put(event.getVehicleId(), event.getPersonId());
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// TODO Auto-generated method stub
		if (event.getVehicleId().toString().startsWith("TW")) {
			Id<Person> perid = personVehicles.get(event.getVehicleId());
			
			RentalInfo info = twRentalsStats.get(perid).get(twRentalsStats.get(perid).size() - 1);
			info.vehId = event.getVehicleId();
			info.distance += network.getLinks().get(event.getLinkId()).getLength();
			
		}
				
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		// TODO Auto-generated method stub
		if (arrivals.get(event.getPersonId()) != null && arrivals.get(event.getPersonId()).equals("twowaycarsharing")) {
			
			if (event.getLegMode().equals("walk_rb")) {
				RentalInfo info = twRentalsStats.get(event.getPersonId()).get(twRentalsStats.get(event.getPersonId()).size() - 1);
				
				info.egressEndTime = event.getTime();
				
				twRentalsStats.get(event.getPersonId()).remove(twRentalsStats.get(event.getPersonId()).size() - 1);
				arr.add(info);
			}
		}
		arrivals.put(event.getPersonId(), event.getLegMode());
		
		
	}

	public ArrayList<RentalInfo> rentals() {
		
		return arr;
	}
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// TODO Auto-generated method stub
		
		if (event.getLegMode().equals("twowaycarsharing")) {
			if (arrivals.get(event.getPersonId()).equals("walk_rb")) {
				RentalInfo info = twRentalsStats.get(event.getPersonId()).get(twRentalsStats.get(event.getPersonId()).size() - 1);
				
				info.startTime = event.getTime();
				info.startLinkId = event.getLinkId();
				info.accessEndTime = event.getTime();
				
			}
			
			
		}
		
		else if (event.getLegMode().equals("walk_rb")) {
			
				if (arrivals.get(event.getPersonId()) != null && arrivals.get(event.getPersonId()).equals("twowaycarsharing")) {
			
					RentalInfo info = twRentalsStats.get(event.getPersonId()).get(twRentalsStats.get(event.getPersonId()).size() - 1);
					info.endTime = event.getTime();
					info.egressStartTime = event.getTime();
					
					
				}
				else {
					RentalInfo info = new RentalInfo();
					info.accessStartTime = event.getTime();
					info.personId = event.getPersonId();
					ArrayList<RentalInfo> temp1 = new ArrayList<RentalInfo>();
					if (twRentalsStats.get(event.getPersonId()) == null) {
						
						temp1.add(info);
						twRentalsStats.put(event.getPersonId(), temp1);

					}
					else {
						twRentalsStats.get(event.getPersonId()).add(info);
						
					}
					
					
				}
			
		}
		
	}
	
	public class RentalInfo {
		private Id<Person> personId = null;
		private double startTime = 0.0;
		private double endTime = 0.0;
		private Id<Link> startLinkId = null;
		private double distance = 0.0;
		private double accessStartTime = 0.0;
		private double accessEndTime = 0.0;
		private double egressStartTime = 0.0;
		private double egressEndTime = 0.0;
		private Id<Vehicle> vehId = null;
		public String toString() {
			
			return personId + " " + Double.toString(startTime) + " " + Double.toString(endTime) + " " +
			startLinkId.toString() + " " + Double.toString(distance)+ " " + Double.toString(accessEndTime - accessStartTime)
			+ " " + Double.toString(egressEndTime - egressStartTime) + " " + vehId;
		}
	}

	

}
