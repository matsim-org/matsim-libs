package saleem.gaming.scenariobuilding;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

/**
 * A class to enforce PT fare
 * 
 * @author Mohammad Saleem
 */
public class FareControlHandler implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler{
	private Set<Id<Person>>  transitDrivers = new HashSet<>();
	private Set<Id<Vehicle>> transitVehicles = new HashSet<>();
	private Set<Id<Person>>  passengers = new HashSet<>();
	EventsManager eventsmanager=null;
	public FareControlHandler(EventsManager eventsmanager){
		this.eventsmanager=eventsmanager;
	}
	@Override
	public void reset(int iteration) {
		if(this.transitDrivers==null) {
			this.transitDrivers = new HashSet<Id<Person>>();
		}else{
			this.transitDrivers.clear();
		}
		if(this.transitVehicles==null) {
			this.transitVehicles = new HashSet<Id<Vehicle>>();
		}else{
			this.transitVehicles.clear();
		}
		if(this.passengers==null) {
			this.passengers = new HashSet<Id<Person>>();
		}else{
			this.passengers.clear();		
		}
	}
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId()) || 
				this.passengers.contains(event.getPersonId()) || !this.transitVehicles.contains(event.getVehicleId())) {
			return; 
			// ignore transit drivers or persons entering non-transit vehicles or passengers who have already paid
		}
		double amount = -40;
//		if(event.getPersonId().toString().contains("a")){
//			amount -=40; 
//		}
		PersonMoneyEvent fareevent = new PersonMoneyEvent(event.getTime(), event.getPersonId(), amount);//Monthly Pass of 800/20
		
		this.eventsmanager.processEvent(fareevent);
		this.passengers.add(event.getPersonId());
	}
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.transitDrivers.add(event.getDriverId());
		this.transitVehicles.add(event.getVehicleId());

		
	}
	
}
