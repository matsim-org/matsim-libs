package playground.sebhoerl.analysis.aggregate_events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public interface AggregateHandler {
    public void handleTrip(Trip trip);
    public void handleWaiting(Id<Person> person, Id<Person> av, double start, double end);
    public void handleAVState(Id<Person> av, double start, double end, String state);
    public void handleAVDispatcherMode(String mode, double start, double end);
}
