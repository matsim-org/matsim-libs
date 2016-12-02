package playground.santiago.colectivos;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;

public class ColectivoTraveltimesEvaluator implements TransitDriverStartsEventHandler, PersonArrivalEventHandler {

	private final Map<Id<Person>,Double> startTimes = new HashMap<>();
	private final Map<Id<Person>,Double> travelTimes = new HashMap<>();
	private int counter =0;
	
	public ColectivoTraveltimesEvaluator() {
	}

	public void handleEvent(TransitDriverStartsEvent event) {
		if (event.getTransitLineId().toString().contains("co")){
		startTimes.put(event.getDriverId(), event.getTime());
						
		}
	}

	public void handleEvent(PersonArrivalEvent event) {
		if (startTimes.containsKey(event.getPersonId())){
			double endTime = event.getTime();
			double travelTime = endTime-startTimes.get(event.getPersonId());
			travelTimes.put(event.getPersonId(), travelTime);
		}
		
	
		
	}
	
	public Map<Id<Person>,Double> getTravelTimes(){
		return travelTimes;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
}
