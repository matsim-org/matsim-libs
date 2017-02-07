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
import java.util.stream.Collectors;

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

import playground.clruch.utils.HelperPredicates;

/**
 * Created by Claudio on 2/2/2017.
 */
class VehicleLocation extends AbstractExport {

    Map<String, NavigableMap<Double, String>> vehicleLocations = new TreeMap<>();


    @Override
    void initialize(EventsManager events) {


        // add handlers to read vehicle status
        {


            // the start location of the vehicles is recorded using the event:
            // <event time="0.0" type="actend" person="av_av_op1_174" link="237756569_3" actType="BeforeVrpSchedule"  />

            // activityend
            events.addHandler(new ActivityEndEventHandler() {
                @Override
                public void handleEvent(ActivityEndEvent event) {
                    // check if itis an AV event
                    if (HelperPredicates.isPersonAV(event.getPersonId()) && event.getActType().equals("BeforeVrpSchedule")) {

                        // if AV not recorded, add map
                        if (!vehicleLocations.containsKey(event.getPersonId().toString())) {
                            vehicleLocations.put(event.getPersonId().toString(), new TreeMap<>());
                        }

                        vehicleLocations.get(event.getPersonId().toString()).put(event.getTime(), event.getLinkId().toString());
                    }
                }


                @Override
                public void reset(int iteration) {

                }
            });


            // every subsequent transition will be recorded in a enterslink event
            events.addHandler(new LinkEnterEventHandler() {
                @Override
                public void handleEvent(LinkEnterEvent event) {
                    if (HelperPredicates.isVehicleAV(event.getVehicleId())) {

                        // if AV not recorded, add map
                        if (!vehicleLocations.containsKey(event.getVehicleId().toString())) {
                            vehicleLocations.put(event.getVehicleId().toString(), new TreeMap<>());
                        }

                        vehicleLocations.get(event.getVehicleId().toString()).put(event.getTime(), event.getLinkId().toString());
                    }

                }

                @Override
                public void reset(int iteration) {

                }
            });

        }

    }


    @Override
    void writeXML(File directory) {

        File fileExport = new File(directory, "vehicleLocations.xml");

        // export to node-based XML file
        new VehicleLocationEventXML().generate(vehicleLocations, fileExport);

    }
}




