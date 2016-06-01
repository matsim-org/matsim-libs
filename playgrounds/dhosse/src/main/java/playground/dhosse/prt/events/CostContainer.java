package playground.dhosse.prt.events;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.population.Person;

public class CostContainer {
	
	private CostContainerHandler handler;
	private double personMetersTravelled = 0.0;
	private double meterTravelled = 0.0;
	
	private List<Id<Person>> passengers;
	private List<Id<Person>> agentsServed;
	
	private double cummulatedCosts = 0.;
	
	public CostContainer(CostContainerHandler handler){
		
		this.handler = handler;
		this.cummulatedCosts += this.handler.costPerDay;
		this.agentsServed = new ArrayList<Id<Person>>();
		this.passengers = new ArrayList<Id<Person>>();
		
	}
	
	protected void handlePersonEntersVehicle(PersonEntersVehicleEvent event){
		
		if(!event.getVehicleId().toString().equals(event.getPersonId().toString())){
			
			this.passengers.add(event.getPersonId());
			
		}
		
	}
	
	protected void handlePersonLeavesVehicle(PersonLeavesVehicleEvent event){
		
		if(!event.getVehicleId().toString().equals(event.getPersonId().toString())){
			
			this.agentsServed.add(event.getPersonId());
			this.passengers.remove(event.getPersonId());
			
		}
		
	}
	
	protected void addTravelDistance(double distance){
		
		this.meterTravelled += distance;
		this.cummulatedCosts += distance * this.handler.costPerMeter;
		
		if(!this.passengers.isEmpty()){
			
			this.personMetersTravelled += this.passengers.size() * distance;
			
		}
		
	}
	
	public List<Id<Person>> getAgentsServed(){
		
		return this.agentsServed;
		
	}
	
	public int getTasksServed(){
		
		return this.agentsServed.size();
		
	}
	
	public double getCummulatedCosts(){
		
		return this.cummulatedCosts;
		
	}
	
	public double getMeterTravelled() {
		
		return this.meterTravelled;
		
	}
	
	public double getPersonMetersTravelled(){
		
		return this.personMetersTravelled;
		
	}

}
