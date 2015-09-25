package cottbusAnalysis;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;

/**
 * 
 * @author tthunig
 * @deprecated
 */
public class TtCalculateComTravelTimes implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	Map<String, Double> commoditySumOfTravelTimes = new TreeMap<String, Double>();
	Map<Id, Double> personTravelTimes = new TreeMap<Id, Double>();
	
	Map<Id, Double> personDepartureTimes = new HashMap<Id, Double>();
	
	@Override
	public void reset(int iteration) {
		commoditySumOfTravelTimes = new TreeMap<String, Double>();
		personTravelTimes = new TreeMap<Id, Double>();
		personDepartureTimes = new HashMap<Id, Double>();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (personDepartureTimes.containsKey(event.getPersonId())){
			double departureTime = personDepartureTimes.remove(event.getPersonId());
			double travelTime = event.getTime() - departureTime;
			// the first five digits of the personId are the commodityId
			String comId = event.getPersonId().toString().substring(0, 5);
			if (!commoditySumOfTravelTimes.containsKey(comId)){
				commoditySumOfTravelTimes.put(comId, 0.);
			}
			// add the travel time of the person to the sum of travel times
			commoditySumOfTravelTimes.put(comId, commoditySumOfTravelTimes.get(comId) + travelTime);
			// every person arrives only once
			personTravelTimes.put(event.getPersonId(), travelTime);
		}
		else
			System.err.println("Something is wrong with the PersonArrivalEvent");		
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!personDepartureTimes.containsKey(event.getPersonId())){
			personDepartureTimes.put(event.getPersonId(), event.getTime());
		}
		else
			System.err.println("Something is wrong with the PersonDepartureEvent");
	}

	public Map<String, Double> getComTravelTimes(){
		return commoditySumOfTravelTimes;
	}
	
	public Map<Id, Double> getPersonTravelTimes(){
		return personTravelTimes;
	}
}
