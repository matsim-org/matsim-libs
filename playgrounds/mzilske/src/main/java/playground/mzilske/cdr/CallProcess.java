package playground.mzilske.cdr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Steppable;

import d4d.Sighting;

public class CallProcess implements ActivityStartEventHandler, ActivityEndEventHandler, Steppable {
	
	public class CallingAgent {

		public int nCalls;

	}

	private Population population;
	
	private Map<Id, CallingAgent> agents = new HashMap<Id, CallingAgent>();
	
	private final int dailyRate;
	private final double secondlyProbability;

	private EventsManager eventsManager;

	List<Sighting> sightings = new ArrayList<Sighting>();

	final private ZoneTracker zoneTracker;
	
	public CallProcess(EventsManager events, Population population, final ZoneTracker zoneTracker, int dailyRate) {
		this.population = population;
		this.eventsManager = events;
		this.zoneTracker = zoneTracker;
		this.dailyRate = dailyRate;
		this.secondlyProbability = this.dailyRate / (double) (24*60*60);
		for (Person p : population.getPersons().values()) {
			agents.put(p.getId(), new CallingAgent());
		}
	}

	public void dump() {
		for (Sighting sighting : sightings) {
			System.out.println(sighting);
		}
		for (Person p : population.getPersons().values()) {
			System.out.println(agents.get(p.getId()).nCalls);
		}
	}

	public void doSimStep(double time) {
		for (Person p : population.getPersons().values()) {
			CallingAgent agent = agents.get(p.getId());
			if (Math.random() < secondlyProbability) { // Let's make a call!
				agent.nCalls++;
				call(time, p.getId());
			}
		}
	}

	public void call(double time, Id personId) {
		String cellId = null;
		if (zoneTracker != null) {
			cellId = zoneTracker.getZoneForPerson(personId).toString();
		}
		Sighting sighting = new Sighting(personId, (long) time, cellId);
		sightings.add(sighting);
		if (eventsManager != null) {
			eventsManager.processEvent(sighting);
		}
	}

	public List<Sighting>  getSightings() {
		return sightings;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		call(event.getTime(), event.getPersonId());
		Id cellId = zoneTracker.getZoneForPerson(event.getPersonId());
		if (!cellId.equals(event.getLinkId())) {
			throw new RuntimeException();
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		 call(event.getTime(), event.getPersonId());
	}

}
