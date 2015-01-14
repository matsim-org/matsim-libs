package playground.dhosse.prt.analysis;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;

public class PrtEventsHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
	LinkEnterEventHandler {

	int counter = 0;
	List<Id<Person>> personIds = new ArrayList<Id<Person>>();
	
	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(event.getTime()>=6*3600&&event.getTime()<7*3600){
		if(event.getVehicleId().toString().equals("1314_1")){
			if(!event.getPersonId().equals("1314_1")){
				counter++;
				personIds.add(event.getPersonId());
				System.out.println(event.getTime() + " entered "+counter);
			}
		}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(event.getTime()>=6*3600&&event.getTime()<7*3600){
		if(event.getVehicleId().toString().equals("1314_1")){
			if(!event.getPersonId().equals("1314_1")){
				counter--;
				personIds.remove(event.getPersonId());
				System.out.println(event.getTime() + " left "+counter);
			}
		}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
//		event.g
//		System.out.println();
	}

}
