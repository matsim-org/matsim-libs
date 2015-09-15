package saleem.stockholmscenario.teleportation;

import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
public class HandleStuckVehicles implements PersonStuckEventHandler, TransitDriverStartsEventHandler{
	int count = 0;int countveh=0;
	String persons="Stuck Persons Are: ";
	String vehicles="Stuck Vehicles Are: ";
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		count++;
		persons+= event.getPersonId() + ", ";
		
	}
	public void printStuckPersonsAndVehicles(){
		System.out.println(persons);
		System.out.println("Total Stuck Persons Are: "+ count);
		System.out.println(vehicles);
		System.out.println("Total Vehicles Are: "+ countveh);
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		// TODO Auto-generated method stub
		countveh++;
		vehicles+= event.getVehicleId() + ", ";
	}

}
