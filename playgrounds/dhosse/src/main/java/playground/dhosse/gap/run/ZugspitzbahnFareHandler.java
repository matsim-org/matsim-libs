package playground.dhosse.gap.run;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.vehicles.Vehicle;

public class ZugspitzbahnFareHandler implements PersonArrivalEventHandler, PersonEntersVehicleEventHandler {

	private final double amount = -30.5;
	private final Controler controler;
	
	private Set<Id<Person>> passengers = new HashSet<>();
	
	private Set<Id<Vehicle>> vehicleIds = new HashSet<>();
	
	public ZugspitzbahnFareHandler(final Controler controler){
		
		this.controler = controler;
		
		this.vehicleIds.add(Id.create("677", Vehicle.class));
		this.vehicleIds.add(Id.create("678", Vehicle.class));
		this.vehicleIds.add(Id.create("679", Vehicle.class));
		this.vehicleIds.add(Id.create("680", Vehicle.class));
		this.vehicleIds.add(Id.create("681", Vehicle.class));
		this.vehicleIds.add(Id.create("682", Vehicle.class));
		this.vehicleIds.add(Id.create("683", Vehicle.class));
		this.vehicleIds.add(Id.create("684", Vehicle.class));
		this.vehicleIds.add(Id.create("685", Vehicle.class));
		this.vehicleIds.add(Id.create("686", Vehicle.class));
		this.vehicleIds.add(Id.create("687", Vehicle.class));
		this.vehicleIds.add(Id.create("688", Vehicle.class));
		this.vehicleIds.add(Id.create("689", Vehicle.class));
		this.vehicleIds.add(Id.create("690", Vehicle.class));
		this.vehicleIds.add(Id.create("691", Vehicle.class));
		this.vehicleIds.add(Id.create("692", Vehicle.class));
		this.vehicleIds.add(Id.create("693", Vehicle.class));
		
	}

	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		
		if(event.getLegMode().equals(TransportMode.pt)){
			
			if(this.passengers.contains(event.getPersonId())){
				controler.getEvents().processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), this.amount));
				this.passengers.remove(event.getPersonId());
			}
			
			
		}
		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		
		if(this.vehicleIds.contains(event.getVehicleId())){
			
			if(!event.getVehicleId().equals(event.getPersonId())){
				
				this.passengers.add(event.getPersonId());
				
			}
			
		}
		
	}
	
}
