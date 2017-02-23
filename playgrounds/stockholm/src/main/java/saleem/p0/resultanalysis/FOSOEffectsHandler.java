package saleem.p0.resultanalysis;

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
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
/**
 * An event handling class to help analyse first order second order effects of P0.
 * I.e. for vehicles passing through signlised junctions (first order), and vehicle not passing through junctions (second order)
 * 
 * @author Mohammad Saleem
 */
public class FOSOEffectsHandler implements BasicEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler{//first order second order effects of P0
	private Map<Id<Person>, Double> times = new HashMap<>();
	private Map<Id<Person>, ? extends Person> persons = new HashMap<>();
	EventsToScore scoring;

	double totaltrips = 0, totaltime=0;
	public FOSOEffectsHandler(EventsToScore scoring, Map<Id<Person>, ? extends Person> persons, EventsManager manager) {
		this.persons=persons;
		this.scoring = scoring;
		// TODO Auto-generated constructor stub
	}
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if(persons.containsKey(event.getPersonId())){
		// TODO Auto-generated method stub
			totaltime += (event.getTime()-times.get(event.getPersonId()));
			totaltrips++;
			times.remove(event.getPersonId());
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
	public double getTotalrips(){//get travel time
		System.out.println("Total Trips: " + totaltrips);
		return totaltrips;
	}
	@Override
	public void handleEvent(Event event) {
		scoring.handleEvent(event);

	}
}
