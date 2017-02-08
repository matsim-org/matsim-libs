package playground.clruch.export;

import java.io.File;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
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




