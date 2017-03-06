package playground.joel.data;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.text.*;

import org.jdom.input.SAXBuilder;
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

import playground.clruch.utils.HelperPredicates;

import playground.joel.data.EventFileToDataXML;
/**
 * Created by Joel on 28.02.2017.
 */
class TravelTimes extends AbstractData {
    // TODO: implement recording of rebalancing journeys for visualization

    //NavigableMap<String, NavigableMap<Double, AVStatus>> travelTimes = new TreeMap<>();
    NavigableMap<String, NavigableMap<Double, Double>> travelTimes = new TreeMap<>();
    HashMap<String, Set<Id<Person>>> vehicleCustomers = new HashMap<>();
    HashMap<String, Double> avTripStart = new HashMap<>();
    double totalTimeWithCust = 0;
    double totalTimeRatio;
    static int numAVs = 0;

    // cut the total time ratio
    DecimalFormat ratioForm = new DecimalFormat("#.####");


    void calculateTimeRatio() {
       if(!(numAVs == 0)) {
            // get number of AVs with avTripStart.size()
            //totalTimeRatio = totalTimeWithCust / (numAVs * 108000);
            totalTimeRatio = Double.parseDouble(ratioForm.format(totalTimeWithCust / (numAVs * 108000)));
        } else System.out.println("no AVs found while calculating the time ratio");

    }

    private void setStartTime(PersonEntersVehicleEvent event){
        avTripStart.put(event.getVehicleId().toString(), event.getTime());
    }

    private double getStartTime(PersonEntersVehicleEvent event){
        return avTripStart.get(event.getVehicleId().toString());
    }

    private double getStartTime(PersonLeavesVehicleEvent event){
        return avTripStart.get(event.getVehicleId().toString());
    }

    //private void put(String vehicle, double startTime, double endTime)
    private void put(String vehicle, double startTime, double endTime) {
        if (!travelTimes.containsKey(vehicle))
            travelTimes.put(vehicle, new TreeMap<>());
        //travelTimes.get(vehicle).put(startTime, avStatus);
        travelTimes.get(vehicle).put(startTime, endTime);
        if (endTime <= 108000) {
            totalTimeWithCust += endTime - startTime;
        } else{
            totalTimeWithCust += 108000 - startTime;
        }
        //System.out.println("time with customer: " + totalTimeWithCust);
    }

    private void putDriveWithCustomer(PersonEntersVehicleEvent event) {
        //put(event.getVehicleId().toString(), event.getTime(), AVStatus.DRIVEWITHCUSTOMER);
        setStartTime(event);
    }

    private void putDriveToCustomer(PersonDepartureEvent event) {
        //we only consider AVStatus.DRIVEWITHCUSTOMER
        //put(event.getPersonId().toString(), event.getTime(), AVStatus.DRIVETOCUSTMER);
    }

    private Set<Id<Person>> getCustomerSet(String vehicle) {
        if (!vehicleCustomers.containsKey(vehicle))
            vehicleCustomers.put(vehicle, new HashSet<>());

        return vehicleCustomers.get(vehicle);
    }

    private Map<String, PersonEntersVehicleEvent> firstCustomerEntersVehicleEvent = new HashMap<>();
    private Map<String, PersonDepartureEvent> potentialEmptyDriveEvent = new HashMap<>();

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
                        //we only consider AVStatus.DRIVEWITHCUSTOMER
                        //put(vehicle, time, AVStatus.STAY);
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
                    if (HelperPredicates.isHuman(person)) {
                        Set<Id<Person>> set = getCustomerSet(vehicle);
                        if (set.isEmpty()) { // if car is empty
                            firstCustomerEntersVehicleEvent.put(vehicle, event); // mark as beginning of DRIVE WITH CUSTOMER STATUS
                        }
                        set.add(person);
                        setStartTime(event); // remember startTime
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
                    if (HelperPredicates.isHuman(person)) {
                        Set<Id<Person>> set = getCustomerSet(vehicle);
                        set.remove(person);
                        put(vehicle, getStartTime(event), event.getTime()); // add both travel times
                        if (set.isEmpty()) { // last customer has left the car
                            if (firstCustomerEntersVehicleEvent.containsKey(vehicle)) {
                                putDriveWithCustomer(firstCustomerEntersVehicleEvent.get(vehicle));
                            } else {
                                new RuntimeException("this should have a value").printStackTrace();

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
                    // only departure events of avs are considered.
                    if(HelperPredicates.isPersonAV(event.getPersonId())){
                        potentialEmptyDriveEvent.put(event.getPersonId().toString(), event);
                    }
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
                    final String vehicle = event.getPersonId().toString(); // this is not always an ID to a vehicle
                    if (!vehicleCustomers.containsKey(vehicle) || vehicleCustomers.get(vehicle).isEmpty()) {
                        // this was an empty drive
                        if (potentialEmptyDriveEvent.containsKey(vehicle)) // only previously registered true vehicles are considered
                            putDriveToCustomer(potentialEmptyDriveEvent.get(vehicle));

                        //else
                        //    new RuntimeException("should have value").printStackTrace();
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

        File fileExport = new File(directory, "travelTimes.xml");

        // export to node-based XML file
        new TravelTimesXML().generate(travelTimes, fileExport);

        //System.out.println("combined time with customers: " + totalTimeWithCust);
        numAVs = avTripStart.size();
        //System.out.println("number of AVs: " + numAVs);
        calculateTimeRatio();
        //System.out.println("total time ratio: " + totalTimeRatio);
    }
}
