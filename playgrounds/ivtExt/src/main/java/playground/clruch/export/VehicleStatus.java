package playground.clruch.export;

import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.core.api.experimental.events.EventsManager;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Claudio on 2/2/2017.
 */
public class VehicleStatus extends AbstractExport {

    List<Event> enterTrafficEvents = new ArrayList<>();
    Map<String, NavigableMap<Double, IdAVStatus>> vehicleStatus = new TreeMap<>();
    NavigableMap<Double,Event> relevantEvents = new TreeMap<>();

    @Override
    void initialize(EventsManager events) {
        // add handlers to read vehicle status
        {
            // activitystart
            events.addHandler(new ActivityStartEventHandler() {
                @Override
                public void handleEvent(ActivityStartEvent event) {
                    if(!EventFileToProcessingXML.isPerson(event.getPersonId().toString())){
                        relevantEvents.put(event.getTime(),event);
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
                    if(EventFileToProcessingXML.isPerson(event.getPersonId().toString())){
                        relevantEvents.put(event.getTime(),event);
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
                    if(!EventFileToProcessingXML.isPerson(event.getPersonId().toString())){
                        relevantEvents.put(event.getTime(),event);
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
                    if(!EventFileToProcessingXML.isPerson(event.getPersonId().toString())){
                        relevantEvents.put(event.getTime(),event);
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
                    if(!EventFileToProcessingXML.isPerson(event.getPersonId().toString())){
                        relevantEvents.put(event.getTime(),event);
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
                    if(!EventFileToProcessingXML.isPerson(event.getPersonId().toString())){
                        relevantEvents.put(event.getTime(),event);
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
                    if(!EventFileToProcessingXML.isPerson(event.getPersonId().toString())){
                        relevantEvents.put(event.getTime(),event);
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
                    if(!EventFileToProcessingXML.isPerson(event.getVehicleId().toString())){
                        relevantEvents.put(event.getTime(),event);
                    }
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // entered link
            events.addHandler(new LinkEnterEventHandler() {
                @Override
                public void handleEvent(LinkEnterEvent event) {
                    if(!EventFileToProcessingXML.isPerson(event.getVehicleId().toString())){
                        relevantEvents.put(event.getTime(),event);
                    }
                }

                @Override
                public void reset(int iteration) {

                }
            });

            // arrival
            events.addHandler(new PersonArrivalEventHandler() {
                @Override
                public void handleEvent(PersonArrivalEvent event) {
                    if(!EventFileToProcessingXML.isPerson(event.getPersonId().toString())){
                        relevantEvents.put(event.getTime(),event);
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
                    if(!EventFileToProcessingXML.isPerson(event.getPersonId().toString())){
                        relevantEvents.put(event.getTime(),event);
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
        (new VehicleStatusEventXML(vehicleStatus,relevantEvents)).generate3(fileExport);

    }
}



// GRAVEYARD

 /* old implementation
            events.addHandler(new ActivityStartEventHandler() {
                @Override
                public void handleEvent(ActivityStartEvent event) {
                    relevantEvents.add(event);
                    //check if it is of actType "AVStay"
                    // TODO: change frmo string to reference for that string AVStay
                    if (event.getActType().equals("AVStay")) {
                        // if the vehicle was not yet recorded, add element to vehicleStatus
                        String vehicleID = event.getPersonId().toString();
                        if (!vehicleStatus.containsKey(vehicleID)) {
                            vehicleStatus.put(vehicleID, new TreeMap<>());
                        }
                        double time = event.getTime();
                        IdAVStatus idAVStatus = new IdAVStatus();
                        idAVStatus.id = event.getLinkId().toString();
                        idAVStatus.avStatus = event.getActType();
                        vehicleStatus.get(vehicleID).put(time, idAVStatus);
                    }
                }


                @Override
                public void reset(int iteration) {


                }
            });
            */