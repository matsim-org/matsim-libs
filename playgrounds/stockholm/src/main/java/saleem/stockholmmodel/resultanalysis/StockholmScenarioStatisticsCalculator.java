package saleem.stockholmmodel.resultanalysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.scoring.EventsToScore;
/**
 * This event handler class is used to calculates different relevant statistics about Stockholm scenario.
 * 
 * @author Mohammad Saleem
 *
 */
public class StockholmScenarioStatisticsCalculator implements PersonDepartureEventHandler, PersonArrivalEventHandler{//first order second order effects of P0
	private Map<Id<Person>, Double> times = new HashMap<>();
	private Map<Id<Person>, ? extends Person> persons = new HashMap<>();
	EventsToScore scoring;

	double totaltrips = 0, totaltime=0;
	public StockholmScenarioStatisticsCalculator(Map<Id<Person>, ? extends Person> persons) {
		this.persons=persons;
		// TODO Auto-generated constructor stub
	}
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if(persons.containsKey(event.getPersonId())){
			if(event.getLegMode().toString()!="transit_walk"){
				totaltime += (event.getTime()-times.get(event.getPersonId()));
				totaltrips++;
				times.remove(event.getPersonId());
			}
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// TODO Auto-generated method stub
		if(persons.containsKey(event.getPersonId())){
			times.put(event.getPersonId(), event.getTime());
		}
			}
	public double getTT(){//get travel time
		System.out.println("Average Travel Time: " + totaltime/totaltrips);
		return totaltime/totaltrips;
	}
	public double getTotalrips(){//get total trips
		System.out.println("Total Trips: " + totaltrips);
		return totaltrips;
	}
}
