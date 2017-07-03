package playground.clruch.trb18.analysis;

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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class ReferenceReader {
    static public Map<Id<Person>, Queue<Double>> getReferenceTravelTimes(String eventsPath) {
        Map<Id<Person>, Queue<Double>> data = new HashMap<>();

        EventsManager eventsManager = EventsUtils.createEventsManager();

        TravelTimeHandler handler = new TravelTimeHandler(data);
        eventsManager.addHandler(handler);

        new MatsimEventsReader(eventsManager).readFile(eventsPath);
        return data;
    }

    static class TravelTimeHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler {
        final private Map<Id<Person>, Queue<Double>> data;

        final private Map<Id<Person>, Double> departureTimes = new HashMap<>();
        final private Map<Id<Person>, Double> arrivalTimes = new HashMap<>();

        public TravelTimeHandler(Map<Id<Person>, Queue<Double>> data) {
            this.data = data;
        }

        @Override
        public void handleEvent(ActivityStartEvent event) {
            if (!event.getActType().equals("pt interaction")) {
                Double departureTime = departureTimes.remove(event.getPersonId());
                Double arrivalTime = arrivalTimes.remove(event.getPersonId());

                if (departureTime != null && arrivalTime != null) {
                    if (!data.containsKey(event.getPersonId())) data.put(event.getPersonId(), new LinkedList<>());
                    data.get(event.getPersonId()).add(arrivalTime - departureTime);
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
        }
    }
}
