package playground.santiago.colectivos;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;

public class ColectivoTraveltimesEvaluator implements TransitDriverStartsEventHandler, PersonArrivalEventHandler {

	private final Network network;
	private final Map<Id<Person>,Double> startTimes = new HashMap<>();
	private final Map<Id<Person>,Double> endTimes = new HashMap<>();
	private final Map<Id<Person>,Double> travelTimes = new HashMap<>();
	private int counter =0;
	
	public ColectivoTraveltimesEvaluator(Network network) {
		this.network = network;
	}

	public void handleEvent(TransitDriverStartsEvent event) {
		if (event.getTransitLineId().toString().contains("co")){
		startTimes.put(event.getDriverId(), event.getTime());
						
		}
	}

	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().equals("colectivo")){
			if (event.getPersonId().toString().contains("co")){
			endTimes.put(event.getPersonId(), event.getTime());
			}
		}
		
		for (Map.Entry<Id<Person>,Double> start : startTimes.entrySet()){
			for (Map.Entry<Id<Person>,Double> end : endTimes.entrySet()){
				if (start.getKey().equals(end.getKey())){
					double travelTime = end.getValue() - start.getValue();
					counter = counter ++;
					travelTimes.put(start.getKey(), travelTime);
					System.out.println(start.getKey());
					System.out.println(travelTime);
				}
			}
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
