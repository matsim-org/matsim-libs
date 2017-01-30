package playground.clruch;

/**
 * Comments for class EventFileToProcessingXML
 *
 * @author Claudio Ruch
 * @version 1.0
 */


import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Class for handling intervals
 */
class DoubleInterval {
    double start;
    double end;

    /**
     * Checks if value is in interval [start,end)
     *
     * @param value double value to be checked
     * @return boolean value
     */
    public boolean isInside(double value) {
        return start <= value && value < end;
    }

    @Override
    public String toString() {
        return start + " " + end;
    }
}

/**
 * Class for storing a <ID,numCust> pair
 */
class IdNumCust {
    String id;
    int numberCust;

}


/**
 * Read an Event file and generate appropriate processing file for network visualization
 */

public class EventFileToProcessingXML {
    /**
     * checks if person with id is a person or an "av-driver", i.e. a virtual agent
     * the virtual agents start with "av".
     *
     * @param id
     * @return
     */
    public static boolean isPerson(String id) {
        return !id.startsWith("av_");
    }

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


    /**
     * reads an output events file from a matsim simulation and outputs an XML file to be read by
     * processing
     *
     * @param args the path of the project folder
     */
    public static void main(String[] args) {

        // read an event output file given String[] args
        final File dir = new File(args[0]);
        File fileImport = new File(dir, "output/output_events.xml");
        File fileExport = new File(dir, "output/output_processing.xml");
        File fileExport2 = new File(dir, "output/output_processing_timeseries.xml");
        File fileExport3 = new File(dir, "output/output_processing_timeseries_full.xml");
        System.out.println("Is directory?  " + dir.isDirectory());

        // From the existing output_events file, load the data
        List<Event> relevantEvents = new ArrayList<>();
        Map<String, PersonDepartureEvent> deptEvent = new HashMap<>();
        Map<String, List<DoubleInterval>> linkOccupation = new HashMap<>();
        Map<String, NavigableMap<Double, Integer>> waitDelta = new TreeMap<>();
        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(i -> System.out.println("" + i));

        // read the events when person calls AVs
        events.addHandler(new PersonDepartureEventHandler() {

            @Override
            public void reset(int iteration) {
                // empty content
            }

            @Override
            public void handleEvent(PersonDepartureEvent event) {
                relevantEvents.add(event);
                final String id = event.getPersonId().toString();
                if (isPerson(id)) {
//                   System.out.println("dept " + id);
                    String linkId = event.getLinkId().toString();
                    deptEvent.put(id, event);
                    if (!waitDelta.containsKey(linkId))
                        waitDelta.put(linkId, new TreeMap<>());
                    waitDelta.get(linkId).put(event.getTime(),//
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
//                System.out.println("ARRIVE "+event.getTime()+" "+event.getPersonId());
//                System.out.println("arrival "+event.getPersonId());
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
                final String id = event.getPersonId().toString();
                if (isPerson(id)) {
                    double wait = event.getTime() - deptEvent.get(id).getTime();
                    String linkId = deptEvent.get(id).getLinkId().toString();

                    System.out.println("enter " + id + "  " + deptEvent.get(id).getTime() + " - " + event.getTime() + " =" + wait + " " + linkId);
                    if (!linkOccupation.containsKey(linkId))
                        linkOccupation.put(linkId, new ArrayList<>());
                    DoubleInterval doubleInterval = new DoubleInterval();
                    doubleInterval.start = deptEvent.get(id).getTime();
                    doubleInterval.end = event.getTime();
                    linkOccupation.get(linkId).add(doubleInterval);

                    waitDelta.get(linkId).put(event.getTime(),//
                            waitDelta.get(linkId).containsKey(event.getTime()) ? //
                                    waitDelta.get(linkId).get(event.getTime()) - 1 : -1);


                }
            }
        });
        new MatsimEventsReader(events).readFile(fileImport.toString());

        // process events list to fill XML
        System.out.println("========== " + fileImport.getAbsoluteFile());


        // integrate customer number changes to get to waiting customers step function
        //
        Map<String, NavigableMap<Double, Integer>> waitStepFctn;
        {
            waitStepFctn = waitDelta.entrySet().stream() //
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> integrate(entry.getValue())));
            System.out.println("==========");
            waitStepFctn.entrySet().stream().forEach(System.out::println);
        }

        // export to node-based XML file
        new NodeBasedEventXML().generate(waitStepFctn, fileExport);

        // export to time-based XML file with only changing information at every time step
        new TimeBasedChangeEventXML().generate(waitStepFctn, fileExport2);

        System.out.println("routine finished successfully");
    }
}
