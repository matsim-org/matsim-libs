package saleem.stockholmmodel.resultanalysis;

import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
/**
 * A class for stuck persons and vehicles analysis.
 * 
 * @author Mohammad Saleem
 *
 */
public class HandleStuckVehicles implements PersonStuckEventHandler, TransitDriverStartsEventHandler{
	int count = 0;int countveh=0;
	String persons="Stuck Persons Are: ";
	String vehicles="Vehicles Are: ";
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		count++;
		persons+= event.getPersonId() + ", ";
		
	}
	void initiate(){
		count = 0;
		countveh=0;
		persons="Stuck Persons Are: ";
		vehicles="Vehicles Are: ";
	}
	public void printStuckPersonsAndVehicles(){
		System.out.println(persons);//By ID
		System.out.println("Total Stuck Persons Are: "+ count);
		System.out.println("Total Vehicles Are: "+ countveh);
		initiate();
		
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		// TODO Auto-generated method stub
		countveh++;
		vehicles+= event.getVehicleId() + ", ";
	}

}
