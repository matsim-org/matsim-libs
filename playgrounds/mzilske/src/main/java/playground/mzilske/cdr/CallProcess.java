package playground.mzilske.cdr;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Steppable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CallProcess implements ActivityStartEventHandler, ActivityEndEventHandler, Steppable {

	public class CallingAgent {

		public int nCalls;

	}

	private Population population;

	private Map<Id, CallingAgent> agents = new HashMap<Id, CallingAgent>();

	private EventsManager eventsManager;

	List<Sighting> sightings = new ArrayList<Sighting>();

	final private ZoneTracker zoneTracker;

	private double lastTime;

	private CallBehavior callBehavior;

	public CallProcess(EventsManager events, Population population, final ZoneTracker zoneTracker, CallBehavior callBehavior) {
		this.population = population;
		this.eventsManager = events;
		this.zoneTracker = zoneTracker;
		this.callBehavior = callBehavior;
		for (Person p : population.getPersons().values()) {
			agents.put(p.getId(), new CallingAgent());
		}
	}

	public void dump() {
		for (Sighting sighting : sightings) {
			System.out.println(sighting);
		}

		for (Person p : population.getPersons().values()) {
            if (callBehavior.makeACallAtMorningAndNight(p.getId())) {
                handleNight(p);
            }
			System.out.println(agents.get(p.getId()).nCalls);
		}

	}

	public void doSimStep(double time) {
        if (time == 0.0) {
                for (Person p : population.getPersons().values()) {
                    if (callBehavior.makeACallAtMorningAndNight(p.getId())) {
				    handleMorning(p);
                }
			}
		}
		for (Person p : population.getPersons().values()) {
			CallingAgent agent = agents.get(p.getId());
			if (callBehavior.makeACall(p.getId(), time)) { // Let's make a call!
				agent.nCalls++;
				call(time, p.getId());
			}
		}
		lastTime = time;
	}

	private void handleNight(Person p) {
		call(lastTime, p.getId());
	}

	private void handleMorning(Person p) {
		call(0.0, p.getId());
	}

	private void call(double time, Id personId) {
		String cellId = null;
		if (zoneTracker != null) {
			cellId = zoneTracker.getZoneForPerson(personId).toString();
		}
		Sighting sighting = new Sighting(personId, (long) time, cellId);
		sightings.add(sighting);
		if (eventsManager != null) {
			eventsManager.processEvent(sighting);
		}
        CallingAgent agent = agents.get(personId);
        agent.nCalls++;
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
		if (callBehavior.makeACall(event)) {
			call(event.getTime(), event.getPersonId());
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (callBehavior.makeACall(event)) {
			call(event.getTime(), event.getPersonId());
		}
	}

}
