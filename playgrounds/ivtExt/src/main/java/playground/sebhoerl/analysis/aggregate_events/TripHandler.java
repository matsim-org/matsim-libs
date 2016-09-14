package playground.sebhoerl.analysis.aggregate_events;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.PersonExperiencedActivity;
import org.matsim.core.scoring.PersonExperiencedLeg;

public class TripHandler implements EventsToLegs.LegHandler, EventsToActivities.ActivityHandler {
    final private HashMap<Id<Person>, LinkedList<Leg>> ongoing = new HashMap<>();
    final private Writer writer;
    
    public TripHandler(Writer writer) {
        this.writer = writer;
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

    @Override
    public void handleActivity(PersonExperiencedActivity experiencedActivity) {
        if (experiencedActivity.getActivity().getType() == "pt interaction") {
            return;
        }
        
        Id<Person> agentId = experiencedActivity.getAgentId();
        finishTrip(agentId);
    }
    
    public void finishRemainingTrips() {
        LinkedList<Id<Person>> remaining = new LinkedList<>();
        remaining.addAll(ongoing.keySet());
        
        for (Id<Person> agentId : remaining) {
            finishTrip(agentId);
        }
    }
    
    void finishTrip(Id<Person> agentId) {
        if (ongoing.containsKey(agentId)) {
            handleTrip(Trip.create(agentId, ongoing.get(agentId)));
            ongoing.remove(agentId);
        }
    }
    
    public void handleTrip(Trip trip) {
        try {
            writer.write(trip.toString() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
