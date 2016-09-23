package playground.sebhoerl.analysis.aggregate_events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class DefaultAggregateHandler implements AggregateHandler {
    @Override
    public void handleTrip(Trip trip) {}

    @Override
    public void handleWaiting(Id<Person> person, Id<Person> av, double start, double end) {}

    @Override
    public void handleAVState(Id<Person> av, double start, double end, String state) {}

    @Override
    public void handleAVDispatcherMode(String mode, double start, double end) {}
}
