package playground.mzilske.cdr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;

import d4d.Sighting;

public class CallProcess {
	
	public class CallingAgent {

		public int nCalls;

	}

	private Population population;
	
	private Map<Id, CallingAgent> agents = new HashMap<Id, CallingAgent>();
	
	private final int dailyRate = 20;
	private final double secondlyProbability = dailyRate / (double) (24*60*60);

	private EventsManager eventsManager;

	List<Sighting> sightings = new ArrayList<Sighting>();

	private ZoneTracker zoneTracker;
	
	public CallProcess(EventsManager events, Population population, ZoneTracker zoneTracker) {
		this.population = population;
		this.eventsManager = events;
		this.zoneTracker = zoneTracker;

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

	public void tick(double time) {
		for (Person p : population.getPersons().values()) {
			CallingAgent agent = agents.get(p.getId());
			if (Math.random() < secondlyProbability) { // Let's make a call!
				agent.nCalls++;
				String cellId = null;
				if (zoneTracker != null) {
					cellId = zoneTracker.getZoneForPerson(p.getId()).toString();
				}
				Sighting sighting = new Sighting(p.getId(), (long) time, cellId);
				sightings.add(sighting);
				eventsManager.processEvent(sighting);
			}
		}
	}

	public List<Sighting>  getSightings() {
		return sightings;
	}

}
