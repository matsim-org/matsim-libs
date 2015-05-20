package org.matsim.contrib.carsharing.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class CarsharingLegFinishedEvent extends Event{

	
	private final Id<Person> personId;
	
	private final Id<Vehicle> vehicleId;
	
	private final Leg leg;
	
	public static final String EVENT_TYPE = "Carsharing Leg Finished";
	
	
	public CarsharingLegFinishedEvent(double time, Id<Person> personId, Id<Vehicle> vehicleId, Leg leg) {
		super(time);
		
		this.personId = personId;
		
		this.vehicleId = vehicleId;
		
		this.leg = leg;
	}

	@Override
	public String getEventType() {
		// TODO Auto-generated method stub
		return EVENT_TYPE;
	}
	
	
	public Id<Person> getPersonId(){
		return this.personId;
	}
	
	public Id<Vehicle> getvehicleId(){
		return this.vehicleId;
	}
	
	public Leg getLeg() {
		
		return this.leg;
	}

}
