package playground.clruch.export;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.clruch.utils.HelperPredicates;

/**
 * Created by Claudio on 2/2/2017.
 */
class WaitingCustomers extends AbstractExport {

    // From the existing output_events file, load the data
    List<Event> relevantEvents = new ArrayList<>();
    // TODO key of deptEvent could be of type Id<Person>
    Map<String, PersonDepartureEvent> deptEvent = new HashMap<>();
    Map<String, List<DoubleInterval>> linkOccupation = new HashMap<>();
    Map<String, NavigableMap<Double, Integer>> waitDelta = new TreeMap<>();

    /**
     * integration function to get to step function from discrete set of events
     *
     * @param map navigable map with double times and int describing number of increasing / decreasing waiting customers
     * @return
     */
    public static NavigableMap<Double, Integer> integrate(NavigableMap<Double, Integer> map) {
        NavigableMap<Double, Integer> result = new TreeMap<>();
        int value = 0;
        result.put(0.0, value);
        for (Map.Entry<Double, Integer> entry : map.entrySet()) {
            value += entry.getValue();
            result.put(entry.getKey(), value);

        }
        return result;
    }

    @Override
    void initialize(EventsManager events) {
        // add handlers to read waiting customer queues
        {
            // read the events when person calls AVs
            events.addHandler(new PersonDepartureEventHandler() {

                @Override
                public void reset(int iteration) {
                    // empty content
                }

                @Override
                public void handleEvent(PersonDepartureEvent event) {
                    relevantEvents.add(event);
                    final Id<Person> idRaw = event.getPersonId();
                    final String id = idRaw.toString();
                    if (HelperPredicates.isHuman(idRaw)) {
                        String linkId = event.getLinkId().toString();
                        deptEvent.put(id, event);
                        if (!waitDelta.containsKey(linkId))
                            waitDelta.put(linkId, new TreeMap<>());
                        waitDelta.get(linkId).put(event.getTime(), //
                                waitDelta.get(linkId).containsKey(event.getTime()) ? //
                                        waitDelta.get(linkId).get(event.getTime()) + 1 : 1);
                    }
                }
            });
            // read the events when agend arrives at destination
            events.addHandler(new PersonArrivalEventHandler() {

                @Override
                public void reset(int iteration) {
                    // empty content
                }

                @Override
                public void handleEvent(PersonArrivalEvent event) {
                    relevantEvents.add(event);
                }
            });
            // read the events when AV arrives at agent's location
            events.addHandler(new PersonEntersVehicleEventHandler() {

                @Override
                public void reset(int iteration) {

                }

                @Override
                public void handleEvent(PersonEntersVehicleEvent event) {
                    relevantEvents.add(event);
                    final Id<Person> idRaw = event.getPersonId();
                    final String id = idRaw.toString();
                    if (HelperPredicates.isHuman(idRaw)) {
                        // double wait = event.getTime() - deptEvent.get(id).getTime();
                        String linkId = deptEvent.get(id).getLinkId().toString();
                        if (!linkOccupation.containsKey(linkId))
                            linkOccupation.put(linkId, new ArrayList<>());
                        DoubleInterval doubleInterval = new DoubleInterval();
                        doubleInterval.start = deptEvent.get(id).getTime();
                        doubleInterval.end = event.getTime();
                        linkOccupation.get(linkId).add(doubleInterval);

                        waitDelta.get(linkId).put(event.getTime(), //
                                waitDelta.get(linkId).containsKey(event.getTime()) ? //
                                        waitDelta.get(linkId).get(event.getTime()) - 1 : -1);

                    }
                }
            });
        }
    }

    @Override
    void writeXML(File directory) {
        File fileExport = new File(directory, "waitingCustomers.xml");

        // integrate customer number changes to get to waiting customers step function
        Map<String, NavigableMap<Double, Integer>> waitStepFctn;
        {
            waitStepFctn = waitDelta.entrySet().stream() //
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> integrate(entry.getValue())));
            System.out.println("==========");
        }

        // export to node-based XML file
        new NodeBasedEventXML().generate(waitStepFctn, fileExport);

    }
}
