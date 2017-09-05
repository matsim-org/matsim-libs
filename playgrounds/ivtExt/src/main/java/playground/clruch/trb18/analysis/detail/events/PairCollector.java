package playground.clruch.trb18.analysis.detail.events;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class PairCollector {
    final public TripCollector.Handler referenceHandler;
    final public TripCollector.Handler scenarioHandler;

    final private Collection<Handler> handlers = new HashSet<>();

    public interface Handler {
        void handlePair(TripCollector.Trip referenceTrip, TripCollector.Trip scenarioTrip);
    }

    public void addHandler(Handler handler) {
        this.handlers.add(handler);
    }

    public PairCollector() {
        this.referenceHandler = new TripCollector.Handler() {
            @Override
            public void handleTrip(TripCollector.Trip trip) {
                synchronized (PairCollector.this) {
                    handleReferenceTrip(trip);
                }
            }
        };

        this.scenarioHandler = new TripCollector.Handler() {
            @Override
            public void handleTrip(TripCollector.Trip trip) {
                synchronized (PairCollector.this) {
                    handleScenarioTrip(trip);
                }
            }
        };
    }

    final private Map<Id<Person>, Queue<TripCollector.Trip>> referenceTrips = new HashMap<>();
    final private Map<Id<Person>, Queue<TripCollector.Trip>> scenarioTrips = new HashMap<>();

    private Queue<TripCollector.Trip> getTrips(Map<Id<Person>, Queue<TripCollector.Trip>> map, Id<Person> personId) {
        if (!map.containsKey(personId)) {
            map.put(personId, new LinkedList<>());
        }

        return map.get(personId);
    }

    public void handleReferenceTrip(TripCollector.Trip trip) {
        getTrips(referenceTrips, trip.agentId).add(trip);
        checkPair(trip.agentId);
    }

    public void handleScenarioTrip(TripCollector.Trip trip) {
        getTrips(scenarioTrips, trip.agentId).add(trip);
        checkPair(trip.agentId);
    }

    private void checkPair(Id<Person> personId) {
        Queue<TripCollector.Trip> referenceQueue = getTrips(referenceTrips, personId);
        Queue<TripCollector.Trip> scenarioQueue = getTrips(scenarioTrips, personId);

        while (referenceQueue.size() > 0 && scenarioQueue.size() > 0) {
            for (Handler handler : handlers) {
                handler.handlePair(referenceQueue.poll(), scenarioQueue.poll());
            }
        }
    }
}
