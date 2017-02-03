package playground.clruch.export;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;

/**
 * Created by Claudio on 2/2/2017.
 */
class VehicleStatus extends AbstractExport {
    // TODO: implement recording of rebalancing journeys for visualization

    NavigableMap<String, NavigableMap<Double, AVStatus>> vehicleStatus = new TreeMap<>();
    HashMap<String, Set<Id<Person>>> vehicleCustomers = new HashMap<>();

    private void put(String vehicle, double time, AVStatus avStatus) {
        if (!vehicleStatus.containsKey(vehicle))
            vehicleStatus.put(vehicle, new TreeMap<>());
        vehicleStatus.get(vehicle).put(time, avStatus);
        System.out.println("vehicle=" + vehicle + " time=" + time + " status=" + avStatus);
    }

    private void putDriveWithCustomer(PersonEntersVehicleEvent event) {
        put(event.getVehicleId().toString(), event.getTime(), AVStatus.DRIVEWITHCUSTOMER);
    }

    private void putDriveToCustomer(PersonDepartureEvent event) {
        put(event.getPersonId().toString(), event.getTime(), AVStatus.DRIVETOCUSTMER);
    }

    private Set<Id<Person>> getCustomerSet(String vehicle) {
        if (!vehicleCustomers.containsKey(vehicle))
            vehicleCustomers.put(vehicle, new HashSet<>());

        return vehicleCustomers.get(vehicle);
    }

    private PersonEntersVehicleEvent firstCustomerEntersVehicleEvent = null;
    private PersonDepartureEvent potentialEmptyDriveEvent = null;

    @Override
    void initialize(EventsManager events) {
        // add handlers to read vehicle status
        {
            // activitystart
            events.addHandler(new ActivityStartEventHandler() {
                // <event time="0.0" type="actstart" person="av_av_op1_174" link="237756569_3" actType="AVStay" />
                @Override
                public void handleEvent(ActivityStartEvent event) {
                    if (event.getActType().equals("AVStay")) {
                        final String vehicle = event.getPersonId().toString(); // = vehicle id!
                        final double time = event.getTime();
                        put(vehicle, time, AVStatus.STAY);
                    }
                }

                @Override
                public void reset(int iteration) {
                    // intentionally empty
                }
            });

            // ===========================================

            // personentersvehicle
            events.addHandler(new PersonEntersVehicleEventHandler() {
                // <event time="21574.0" type="PersonEntersVehicle" person="27114_1" vehicle="av_av_op1_174" />
                // <event time="21589.0" type="PersonEntersVehicle" person="av_av_op1_174" vehicle="av_av_op1_174" />
                @Override
                public void handleEvent(PersonEntersVehicleEvent event) {
                    final String vehicle = event.getVehicleId().toString();
                    final Id<Person> person = event.getPersonId();
                    if (HelperFunction.isPerson(person)) {
                        Set<Id<Person>> set = getCustomerSet(vehicle);
                        if (set.isEmpty()) { // if car is empty
                            firstCustomerEntersVehicleEvent = event; // mark as beginning of DRIVE WITH CUSTOMER STATUS
                        }
                        set.add(person);
                    }
                }

                @Override
                public void reset(int iteration) {
                    // intentionally empty
                }
            });

            // personleavesvehicle
            events.addHandler(new PersonLeavesVehicleEventHandler() {
                // <event time="21796.0" type="PersonLeavesVehicle" person="27114_1" vehicle="av_av_op1_174" />
                @Override
                public void handleEvent(PersonLeavesVehicleEvent event) {
                    final String vehicle = event.getVehicleId().toString();
                    final Id<Person> person = event.getPersonId();
                    if (HelperFunction.isPerson(person)) {
                        Set<Id<Person>> set = getCustomerSet(vehicle);
                        set.remove(person);
                        if (set.isEmpty()) { // last customer has left the car
                            if (firstCustomerEntersVehicleEvent != null) {
                                putDriveWithCustomer(firstCustomerEntersVehicleEvent);
                            } else {
                                new RuntimeException("this should not be null").printStackTrace();

                            }
                        }
                    }
                }

                @Override
                public void reset(int iteration) {
                    // intentionally empty

                }
            });

            // departure
            events.addHandler(new PersonDepartureEventHandler() {
                // <event time="21484.0" type="departure" person="av_av_op1_174" link="237756569_3" legMode="car" />

                @Override
                public void handleEvent(PersonDepartureEvent event) {
                    potentialEmptyDriveEvent = event;
                }

                @Override
                public void reset(int iteration) {
                    // intentionally empty

                }
            });

            // arrival
            events.addHandler(new PersonArrivalEventHandler() {
                // <event time="21574.0" type="arrival" person="av_av_op1_174" link="236382034_0" legMode="car" />
                @Override
                public void handleEvent(PersonArrivalEvent event) {
                    final String vehicle = event.getPersonId().toString();
                    if (!vehicleCustomers.containsKey(vehicle) || vehicleCustomers.get(vehicle).isEmpty()) {
                        // this was an empty drive
                        putDriveToCustomer(potentialEmptyDriveEvent);
                    }
                }

                @Override
                public void reset(int iteration) {
                    // intentionally empty

                }
            });

        }

    }

    @Override
    void writeXML(File directory) {

        File fileExport = new File(directory, "vehicleStatus.xml");

        // export to node-based XML file
        new VehicleStatusEventXML().generate(vehicleStatus, fileExport);

    }
}
