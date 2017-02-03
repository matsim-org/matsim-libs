package playground.clruch.export;

import java.io.File;
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
class VehicleStatus extends AbstractExport {

    List<Event> enterTrafficEvents = new ArrayList<>();
    Map<String, NavigableMap<Double, IdAVStatus>> vehicleStatus = new TreeMap<>();
    NavigableMap<Double, Event> relevantEvents = new TreeMap<>();

    @Override
    void initialize(EventsManager events) {
        // add handlers to read vehicle status
        {
            // activitystart
            events.addHandler(new ActivityStartEventHandler() {
                @Override
                public void handleEvent(ActivityStartEvent event) {
                    if (!HelperFunction.isPerson(event.getPersonId())) {
                        relevantEvents.put(event.getTime(), event);
                    }
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // activityend
            events.addHandler(new ActivityEndEventHandler() {
                @Override
                public void handleEvent(ActivityEndEvent event) {
                    if (HelperFunction.isPerson(event.getPersonId())) {
                        relevantEvents.put(event.getTime(), event);
                    }
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // departureevent
            events.addHandler(new PersonDepartureEventHandler() {
                @Override
                public void handleEvent(PersonDepartureEvent event) {
                    if (!HelperFunction.isPerson(event.getPersonId())) {
                        relevantEvents.put(event.getTime(), event);
                    }
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // personentersvehicle
            events.addHandler(new PersonEntersVehicleEventHandler() {
                @Override
                public void handleEvent(PersonEntersVehicleEvent event) {
                    if (!HelperFunction.isPerson(event.getPersonId())) {
                        relevantEvents.put(event.getTime(), event);
                    }
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // personleavesvehicle
            events.addHandler(new PersonLeavesVehicleEventHandler() {
                @Override
                public void handleEvent(PersonLeavesVehicleEvent event) {
                    if (!HelperFunction.isPerson(event.getPersonId())) {
                        relevantEvents.put(event.getTime(), event);
                    }
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // vehicleenterstraffic
            events.addHandler(new VehicleEntersTrafficEventHandler() {
                @Override
                public void handleEvent(VehicleEntersTrafficEvent event) {
                    if (!HelperFunction.isPerson(event.getPersonId())) {
                        relevantEvents.put(event.getTime(), event);
                    }
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // vehicleleavestraffic
            events.addHandler(new VehicleLeavesTrafficEventHandler() {
                @Override
                public void handleEvent(VehicleLeavesTrafficEvent event) {
                    if (!HelperFunction.isPerson(event.getPersonId())) {
                        relevantEvents.put(event.getTime(), event);
                    }
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // leftlink
            events.addHandler(new LinkLeaveEventHandler() {
                @Override
                public void handleEvent(LinkLeaveEvent event) {
//                    if (!HelperFunction.isPerson(event.getVehicleId())) { //  FIXME
//                        relevantEvents.put(event.getTime(), event);
//                    }
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // entered link
            events.addHandler(new LinkEnterEventHandler() {
                @Override
                public void handleEvent(LinkEnterEvent event) {
//                    if (!HelperFunction.isPerson(event.getVehicleId())) { // FIXME
//                        relevantEvents.put(event.getTime(), event);
//                    }
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // arrival
            events.addHandler(new PersonArrivalEventHandler() {
                @Override
                public void handleEvent(PersonArrivalEvent event) {
                    if (!HelperFunction.isPerson(event.getPersonId())) {
                        relevantEvents.put(event.getTime(), event);
                    }
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // departure
            events.addHandler(new PersonDepartureEventHandler() {
                @Override
                public void handleEvent(PersonDepartureEvent event) {
                    if (!HelperFunction.isPerson(event.getPersonId())) {
                        relevantEvents.put(event.getTime(), event);
                    }
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

        Set<String> vehicles = vehicleStatus.keySet();
        for (String id : vehicles) {
            System.out.println("AV with id: " + id + "in file. ");
        }

        File fileExport = new File(directory, "vehicleStatus.xml");

        // export to node-based XML file
        (new VehicleStatusEventXML(vehicleStatus, relevantEvents)).generate3(fileExport);

    }
}

// GRAVEYARD

/*
 * old implementation
 * events.addHandler(new ActivityStartEventHandler() {
 * 
 * @Override
 * public void handleEvent(ActivityStartEvent event) {
 * relevantEvents.add(event);
 * //check if it is of actType "AVStay"
 * // TODO: change frmo string to reference for that string AVStay
 * if (event.getActType().equals("AVStay")) {
 * // if the vehicle was not yet recorded, add element to vehicleStatus
 * String vehicleID = event.getPersonId().toString();
 * if (!vehicleStatus.containsKey(vehicleID)) {
 * vehicleStatus.put(vehicleID, new TreeMap<>());
 * }
 * double time = event.getTime();
 * IdAVStatus idAVStatus = new IdAVStatus();
 * idAVStatus.id = event.getLinkId().toString();
 * idAVStatus.avStatus = event.getActType();
 * vehicleStatus.get(vehicleID).put(time, idAVStatus);
 * }
 * }
 * 
 * 
 * @Override
 * public void reset(int iteration) {
 * 
 * 
 * }
 * });
 */