package org.matsim.contrib.carsharing.control.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.events.EndRentalEvent;
import org.matsim.contrib.carsharing.events.StartRentalEvent;
import org.matsim.contrib.carsharing.events.handlers.EndRentalEventHandler;
import org.matsim.contrib.carsharing.events.handlers.StartRentalEventHandler;
import org.matsim.vehicles.Vehicle;

public class OWEventsHandler implements PersonLeavesVehicleEventHandler, 
PersonEntersVehicleEventHandler, LinkLeaveEventHandler, StartRentalEventHandler, EndRentalEventHandler{
	private Map<String, RentalInfoOW> statsPerVehicle = new HashMap<String, RentalInfoOW>();
	
	private ArrayList<RentalInfoOW> arr = new ArrayList<RentalInfoOW>();
	private Map<Id<Person>, Double> enterVehicleTimes = new HashMap<Id<Person>, Double>();

	private Network network;
	public OWEventsHandler(Network network) {
		
		this.network = network;
	}
	
	@Override
	public void reset(int iteration) {
		arr = new ArrayList<RentalInfoOW>();

	}
	
	@Override
	public void handleEvent(StartRentalEvent event) {
		if (event.getvehicleId().startsWith("OW")){
			RentalInfoOW info = new RentalInfoOW();
			info.accessStartTime = event.getTime();
			info.startTime = event.getTime();
			info.personId = event.getPersonId();
			info.originLinkId = event.getOriginLinkId();
			info.pickupLinkId = event.getPickuplinkId();
					
			this.statsPerVehicle.put(event.getvehicleId(), info);
		}

	}
	
	@Override
	public void handleEvent(EndRentalEvent event) {
		if (event.getvehicleId().startsWith("OW")){

			RentalInfoOW info = this.statsPerVehicle.get(event.getvehicleId());
			this.statsPerVehicle.remove(event.getvehicleId());
			info.endTime = event.getTime();
			info.endLinkId = event.getLinkId();		
			arr.add(info);		
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// === calculate in-vehicle time and add it to the total  in-vehicle time ===
		if (event.getVehicleId().toString().startsWith("OW")){
			double enterTime = this.enterVehicleTimes.get(event.getPersonId());
			
			double totalTime = event.getTime() - enterTime;
			RentalInfoOW info = this.statsPerVehicle.get(event.getVehicleId().toString());
			info.inVehicleTime += totalTime;
			
		}
			
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// === store the enter vehicle time ===
		if (event.getVehicleId().toString().startsWith("OW")) {
			this.enterVehicleTimes.put(event.getPersonId(), event.getTime());
			RentalInfoOW info = this.statsPerVehicle.get(event.getVehicleId().toString());			
			if (info.accessEndTime == 0.0)
				info.accessEndTime = event.getTime();
			

		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getVehicleId().toString().startsWith("OW")) {
			
			RentalInfoOW info = this.statsPerVehicle.get(event.getVehicleId().toString());
			info.vehId = event.getVehicleId();
			info.distance += network.getLinks().get(event.getLinkId()).getLength();
			
		}
		
	}
	
	public ArrayList<RentalInfoOW> rentals() {
		
		return arr;
	}
	
	public class RentalInfoOW {
		private Id<Person> personId = null;
		private double startTime = 0.0;
		private double endTime = 0.0;
		private Id<Link> originLinkId = null;
		private Id<Link> pickupLinkId = null;
		//private Id<Link> dropoffLinkId = null;

		private Id<Link> endLinkId = null;
		private double distance = 0.0;
		private double inVehicleTime = 0.0;
		private double accessStartTime = 0.0;
		private double accessEndTime = 0.0;
		private double egressStartTime = 0.0;
		private double egressEndTime = 0.0;
		private Id<Vehicle> vehId = null;
		public String toString() {
			
			return personId + " " + Double.toString(startTime) + " " + Double.toString(endTime) + " " +
					originLinkId.toString() + " " + pickupLinkId.toString() + " " +	endLinkId.toString() + " " + 
					Double.toString(distance) + " " + Double.toString(inVehicleTime) + " " +
					Double.toString(accessEndTime - accessStartTime)+ " " + Double.toString(egressEndTime - egressStartTime) +
			" " + vehId;
		}
	}

}
