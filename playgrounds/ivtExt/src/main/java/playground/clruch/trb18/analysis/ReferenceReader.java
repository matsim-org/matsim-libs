package playground.clruch.trb18.analysis;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class ReferenceReader {
    static public class ReferenceTrip {
        public double travelTime;
        public String mode;

        public ReferenceTrip(double travelTime, String mode) {
            this.travelTime = travelTime;
            this.mode = mode;
        }
    }

    static public Map<Id<Person>, Queue<ReferenceTrip>> getReferenceTravelTimes(String eventsPath) {
        Map<Id<Person>, Queue<ReferenceTrip>> referenceTrips = new HashMap<>();

        EventsManager eventsManager = EventsUtils.createEventsManager();

        TravelTimeHandler handler = new TravelTimeHandler(referenceTrips);
        eventsManager.addHandler(handler);

        new MatsimEventsReader(eventsManager).readFile(eventsPath);
        return referenceTrips;
    }

    static class TravelTimeHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler {
        final private Map<Id<Person>, Queue<ReferenceTrip>> referenceTrips;

        final private Map<Id<Person>, Double> departureTimes = new HashMap<>();
        final private Map<Id<Person>, Double> arrivalTimes = new HashMap<>();
        final private Map<Id<Person>, String> legModes = new HashMap<>();

        public TravelTimeHandler(Map<Id<Person>, Queue<ReferenceTrip>> referenceTrips) {
            this.referenceTrips = referenceTrips;
        }

        @Override
        public void handleEvent(ActivityStartEvent event) {
            if (!event.getActType().equals("pt interaction")) {
                Double departureTime = departureTimes.remove(event.getPersonId());
                Double arrivalTime = arrivalTimes.remove(event.getPersonId());
                String legMode = legModes.remove(event.getPersonId());

                if (departureTime != null && arrivalTime != null && legMode != null && (legMode.equals("car") || legMode.equals("pt") || legMode.equals("transit_walk"))) {
                    if (!referenceTrips.containsKey(event.getPersonId())) {
                        referenceTrips.put(event.getPersonId(), new LinkedList<>());
                    }

                    referenceTrips.get(event.getPersonId()).add(new ReferenceTrip(arrivalTime - departureTime, legMode.equals("transit_walk") ? "pt" : legMode));
                }
            }
        }

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            if (!departureTimes.containsKey(event.getPersonId())) {
                departureTimes.put(event.getPersonId(), event.getTime());
            }
        }

        @Override
        public void reset(int iteration) {

        }

        @Override
        public void handleEvent(PersonArrivalEvent event) {
            arrivalTimes.put(event.getPersonId(), event.getTime());
            legModes.put(event.getPersonId(), event.getLegMode());
        }
    }
}
