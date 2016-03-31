package playground.dhosse.prt.events;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.data.Vehicle;

public class CostContainerHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
	LinkEnterEventHandler {

	private Network network;
	protected final double costPerDay;
	protected final double costPerMeter;
	
	protected double farePerPassenger;
	protected double farePerKm;
	protected double costsPerDay;
	protected int tasksServed;
	protected double metersTravelled;
	protected double personMetersTravelled;
	
	Map<Id<Vehicle>, CostContainer> vehicleId2TaskContainers = new HashMap<Id<Vehicle>, CostContainer>();
	
	public CostContainerHandler(Network network, double costPerDay, double costPerMeter){
		
		this.network = network;
		this.costPerDay = costPerDay;
		this.costPerMeter = costPerMeter;
		
	}
	
	@Override
	public void reset(int iteration) {
		
		this.vehicleId2TaskContainers.clear();
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		
		Link link = this.network.getLinks().get(event.getLinkId());
		Id<Vehicle> vehicleId = Id.create(event.getVehicleId().toString(), Vehicle.class);
		
		if(!this.vehicleId2TaskContainers.containsKey(vehicleId)){

			CostContainer container = new CostContainer(this);
			this.vehicleId2TaskContainers.put(vehicleId, container);
			
		}
		
		this.vehicleId2TaskContainers.get(vehicleId).addTravelDistance(link.getLength());
		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		
		Id<Vehicle> vehicleId = Id.create(event.getVehicleId().toString(), Vehicle.class);
		
		if(!this.vehicleId2TaskContainers.containsKey(vehicleId)){

			CostContainer container = new CostContainer(this);
			this.vehicleId2TaskContainers.put(vehicleId, container);
			
		}
		
		this.vehicleId2TaskContainers.get(vehicleId).handlePersonEntersVehicle(event);
		
	}
	
	public Map<Id<Vehicle>,CostContainer> getTaskContainersByVehicleId(){
		return this.vehicleId2TaskContainers;
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		
		Id<Vehicle> vehicleId = Id.create(event.getVehicleId().toString(), Vehicle.class);
		
		if(!this.vehicleId2TaskContainers.containsKey(vehicleId)){

			CostContainer container = new CostContainer(this);
			this.vehicleId2TaskContainers.put(vehicleId, container);
			
		}
		
		this.vehicleId2TaskContainers.get(vehicleId).handlePersonLeavesVehicle(event);
		
	}
	
	public double getFarePerPassenger() {
		return farePerPassenger;
	}

	public void setFarePerPassenger(double farePerPassenger) {
		this.farePerPassenger = farePerPassenger;
	}

	public double getFarePerKm() {
		return farePerKm;
	}

	public void setFarePerKm(double farePerKm) {
		this.farePerKm = farePerKm;
	}

	public double getCostsPerDay() {
		return costsPerDay;
	}

	public void setCostsPerDay(double costsPerDay) {
		this.costsPerDay = costsPerDay;
	}

	public int getTasksServed() {
		return tasksServed;
	}

	public void setTasksServed(int tasksServed) {
		this.tasksServed = tasksServed;
	}

	public double getMetersTravelled() {
		return metersTravelled;
	}

	public void setMetersTravelled(double metersTravelled) {
		this.metersTravelled = metersTravelled;
	}

	public double getPersonMetersTravelled() {
		return personMetersTravelled;
	}

	public void setPersonMetersTravelled(double personMetersTravelled) {
		this.personMetersTravelled = personMetersTravelled;
	}

}
