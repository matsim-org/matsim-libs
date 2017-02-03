package playground.clruch.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;

/**
 * Created by Claudio on 2/2/2017.
 */
class VehicleStatusLab extends AbstractExport {

    List<Event> events = new ArrayList<>();


    void dump(Event event) {
        events.add(event);
    }

    void dumpIfRelevant(Event event) {
        if (0 <= event.toString().indexOf("av_av_op1_174")) {
            dump(event);
        }
    }

    @Override
    void initialize(EventsManager events) {

        // add handlers to read vehicle status
        {
            // activitystart
            events.addHandler(new ActivityStartEventHandler() {
                @Override
                public void handleEvent(ActivityStartEvent event) {
                    dumpIfRelevant(event);
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // activityend
            events.addHandler(new ActivityEndEventHandler() {
                @Override
                public void handleEvent(ActivityEndEvent event) {
                    dumpIfRelevant(event);
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // departureevent
            events.addHandler(new PersonDepartureEventHandler() {
                @Override
                public void handleEvent(PersonDepartureEvent event) {
                    dumpIfRelevant(event);
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // personentersvehicle
            events.addHandler(new PersonEntersVehicleEventHandler() {
                @Override
                public void handleEvent(PersonEntersVehicleEvent event) {
                    dumpIfRelevant(event);
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // personleavesvehicle
            events.addHandler(new PersonLeavesVehicleEventHandler() {
                @Override
                public void handleEvent(PersonLeavesVehicleEvent event) {
                    dumpIfRelevant(event);
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // vehicleenterstraffic
            events.addHandler(new VehicleEntersTrafficEventHandler() {
                @Override
                public void handleEvent(VehicleEntersTrafficEvent event) {
                    dumpIfRelevant(event);
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // vehicleleavestraffic
            events.addHandler(new VehicleLeavesTrafficEventHandler() {
                @Override
                public void handleEvent(VehicleLeavesTrafficEvent event) {
                    dumpIfRelevant(event);
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // leftlink
            events.addHandler(new LinkLeaveEventHandler() {
                @Override
                public void handleEvent(LinkLeaveEvent event) {
                    dumpIfRelevant(event);
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // entered link
            events.addHandler(new LinkEnterEventHandler() {
                @Override
                public void handleEvent(LinkEnterEvent event) {
                    dumpIfRelevant(event);
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // arrival
            events.addHandler(new PersonArrivalEventHandler() {
                @Override
                public void handleEvent(PersonArrivalEvent event) {
                    dumpIfRelevant(event);
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // departure
            events.addHandler(new PersonDepartureEventHandler() {
                @Override
                public void handleEvent(PersonDepartureEvent event) {
                    dumpIfRelevant(event);
                }

                @Override
                public void reset(int iteration) {

                }
            });


            // TODO: implement recording of rebalancing journeys for visualization

        }

    }

    @Override
    void writeXML(File directory) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
                    new File(directory, "sometText.xml")));
            for (Event e : events)
                bufferedWriter.write(e.toString() + "\n");
            bufferedWriter.close();
        } catch (Exception e
                ) {
        }
    }
}
