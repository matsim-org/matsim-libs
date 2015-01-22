package playground.dhosse.prt.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.Vehicle;

public class PrtEventsHandler implements ActivityEndEventHandler, ActivityStartEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
	LinkEnterEventHandler {

	int counter = 0;
	int diff = 0;
	double max = 0;
	double min = Double.MAX_VALUE;
	private Map<Id<Person>, Double> ptInteractionEnded = new HashMap<Id<Person>, Double>();
	
	List<Id<Person>> personIds = new ArrayList<Id<Person>>();
	
	int passengerCounts = 0;
	
	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(ptInteractionEnded.containsKey(event.getPersonId())){
			counter++;
			diff += event.getTime() - ptInteractionEnded.get(event.getPersonId());
			max = event.getTime() - ptInteractionEnded.get(event.getPersonId()) > max ? event.getTime() - ptInteractionEnded.get(event.getPersonId()) : max;
			min = event.getTime() - ptInteractionEnded.get(event.getPersonId()) < min ? event.getTime() - ptInteractionEnded.get(event.getPersonId()) : min;
		}
		if(event.getVehicleId().toString().equals("1314_1")){
			if(!event.getPersonId().toString().contains("_")){
			System.out.println("veh 1314_1:" + passengerCounts + "->" + (passengerCounts+1));
			passengerCounts++;
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(event.getVehicleId().toString().equals("1314_1")){
			if(!event.getPersonId().toString().contains("_")){
			System.out.println("veh 1314_1:" + passengerCounts + "->" + (passengerCounts-1));
			passengerCounts--;
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if(event.getActType().equals("pt interaction")){
			ptInteractionEnded.put(event.getPersonId(), event.getTime());
		}
	}

}
