package org.matsim.contrib.carsharing.manager.demand;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.events.EndRentalEvent;
import org.matsim.contrib.carsharing.events.StartRentalEvent;
import org.matsim.contrib.carsharing.events.handlers.EndRentalEventHandler;
import org.matsim.contrib.carsharing.events.handlers.StartRentalEventHandler;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;
/** 
 * @author balac
 */
public class DemandHandler implements PersonLeavesVehicleEventHandler, 
PersonEntersVehicleEventHandler, LinkLeaveEventHandler, StartRentalEventHandler, EndRentalEventHandler {
	
	@Inject Scenario scenario;
	@Inject CarsharingSupplyInterface carsharingSupplyContainer;

	private Map<Id<Person>, AgentRentals> agentRentalsMap = new HashMap<Id<Person>, AgentRentals>();	

	private Map<Id<Vehicle>, VehicleRentals> vehicleRentalsMap = new HashMap<Id<Vehicle>, VehicleRentals>();

	private Map<Id<Vehicle>, Id<Person>> vehiclePersonMap = new HashMap<Id<Vehicle>, Id<Person>>();

	private Map<Id<Person>, Double> enterVehicleTimes = new HashMap<Id<Person>, Double>();	


	@Override
	public void reset(int iteration) {
		agentRentalsMap = new HashMap<Id<Person>, AgentRentals>();	

		vehicleRentalsMap = new HashMap<Id<Vehicle>, VehicleRentals>();

		vehiclePersonMap = new HashMap<Id<Vehicle>, Id<Person>>();

		enterVehicleTimes = new HashMap<Id<Person>, Double>();
	}

	@Override
	public void handleEvent(EndRentalEvent event) {
		
		AgentRentals agentRentals = this.agentRentalsMap.get(event.getPersonId());
		
		RentalInfo info = agentRentals.getStatsPerVehicle().get(event.getvehicleId());
		agentRentals.getStatsPerVehicle().remove(event.getvehicleId());
		info.setEndTime(event.getTime());
		info.setEndLinkId(event.getLinkId());

		agentRentals.getArr().add(info);

		Id<Vehicle> vehicleId = Id.create(event.getvehicleId(), Vehicle.class);
		if (!this.vehicleRentalsMap.containsKey(vehicleId)) {
			this.vehicleRentalsMap.put(vehicleId, new VehicleRentals(vehicleId));
		}

		this.vehicleRentalsMap.get(vehicleId).getRentals().add(info);
}

	@Override
	public void handleEvent(StartRentalEvent event) {
		RentalInfo info = new RentalInfo();
		info.setCarsharingType(event.getCarsharingType());
		info.setAccessStartTime(event.getTime());
		info.setStartTime(event.getTime());
		info.setOriginLinkId(event.getOriginLinkId());
		info.setPickupLinkId(event.getPickuplinkId());

		if (agentRentalsMap.containsKey(event.getPersonId())) {
			AgentRentals agentRentals = this.agentRentalsMap.get(event.getPersonId());
			agentRentals.getStatsPerVehicle().put(event.getvehicleId(), info);
			
		}
		else {
			AgentRentals agentRentals = new AgentRentals(event.getPersonId());
			agentRentalsMap.put(event.getPersonId(), agentRentals);			
			agentRentals.getStatsPerVehicle().put(event.getvehicleId(), info);			
		}		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (carsharingTrip(event.getVehicleId())) {
			Id<Person> personId = this.vehiclePersonMap.get(event.getVehicleId());
			Network network = this.scenario.getNetwork();

			if (agentRentalsMap.containsKey(personId)) {
				AgentRentals agentRentals = this.agentRentalsMap.get(personId);
				RentalInfo info = agentRentals.getStatsPerVehicle().get(event.getVehicleId().toString());
				info.setVehId(event.getVehicleId());
				info.setDistance(info.getDistance() + network.getLinks().get(event.getLinkId()).getLength());
			}
		}
		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (carsharingTrip(event.getVehicleId())) {
			this.vehiclePersonMap.put(event.getVehicleId(), event.getPersonId());
			this.enterVehicleTimes.put(event.getPersonId(), event.getTime());
			
			Id<Person> personId = this.vehiclePersonMap.get(event.getVehicleId());

			if (agentRentalsMap.containsKey(event.getPersonId())) {
				AgentRentals agentRentals = this.agentRentalsMap.get(personId);
				RentalInfo info = agentRentals.getStatsPerVehicle().get(event.getVehicleId().toString());

				if (info.getAccessEndTime() == 0.0)
					info.setAccessEndTime(event.getTime());
			}
		}
		
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		
		if (carsharingTrip(event.getVehicleId())) {
			double enterTime = this.enterVehicleTimes.get(event.getPersonId());	
			double totalTime = event.getTime() - enterTime;

			Id<Person> personId = this.vehiclePersonMap.get(event.getVehicleId());

			if (agentRentalsMap.containsKey(event.getPersonId())) {
				AgentRentals agentRentals = this.agentRentalsMap.get(personId);
				RentalInfo info = agentRentals.getStatsPerVehicle().get(event.getVehicleId().toString());
				info.setInVehicleTime(info.getInVehicleTime() + totalTime);
			}
		}
	}

	private boolean carsharingTrip(Id<Vehicle> vehicleId) {

		return this.carsharingSupplyContainer.getAllVehicles().containsKey(vehicleId.toString());		
	}

	public Map<Id<Person>, AgentRentals> getAgentRentalsMap() {
		return agentRentalsMap;
	}

	public Map<Id<Vehicle>, VehicleRentals> getVehicleRentalsMap() {
		return this.vehicleRentalsMap;
	}
}
