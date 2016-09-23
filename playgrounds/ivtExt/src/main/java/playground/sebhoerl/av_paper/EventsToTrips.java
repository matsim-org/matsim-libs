package playground.sebhoerl.av_paper;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.PersonExperiencedActivity;
import org.matsim.core.scoring.PersonExperiencedLeg;

public class EventsToTrips implements EventsToLegs.LegHandler, EventsToActivities.ActivityHandler {
    final private HashMap<Id<Person>, LinkedList<Leg>> ongoing = new HashMap<>();
    final private LinkedList<TripHandler> handlers = new LinkedList<>();
    
    static public interface TripHandler {
    	void handleTrip(PersonExperiencedTrip trip);
    }
    
    public EventsToTrips(EventsToLegs events2legs, EventsToActivities events2activities) {
    	events2legs.addLegHandler(this);
    	events2activities.addActivityHandler(this);
    }
	
	@Override
	public void handleActivity(PersonExperiencedActivity experiencedActivity) {
        if (experiencedActivity.getActivity().getType().equals("pt interaction")) {
            return;
        }
        
        Id<Person> agentId = experiencedActivity.getAgentId();
        finishTrip(agentId);
	}

	@Override
	public void handleLeg(PersonExperiencedLeg experiencedLeg) {
        Leg leg = experiencedLeg.getLeg();
        Id<Person> agentId = experiencedLeg.getAgentId();
        
        if (!ongoing.containsKey(agentId)) {
            ongoing.put(agentId, new LinkedList<Leg>());
        }
        
        ongoing.get(agentId).add(leg);
	}
	
    public void finalize() {
        LinkedList<Id<Person>> remaining = new LinkedList<>();
        remaining.addAll(ongoing.keySet());
        
        for (Id<Person> agentId : remaining) {
            finishTrip(agentId);
        }
    }
    
    private void finishTrip(Id<Person> agentId) {
        if (ongoing.containsKey(agentId)) {
        	PersonExperiencedTrip trip = PersonExperiencedTrip.create(agentId, ongoing.get(agentId));
        	
        	for (TripHandler handler : handlers) {
        		handler.handleTrip(trip);
        	}
            
            ongoing.remove(agentId);
        }
    }
    
    public void addTripHandler(TripHandler handler) {
    	handlers.add(handler);
    }
}
