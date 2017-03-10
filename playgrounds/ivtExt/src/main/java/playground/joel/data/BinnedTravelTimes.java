package playground.joel.data;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.HelperPredicates;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by Joel on 28.02.2017.
 */
class BinnedTravelTimes extends AbstractData {
    // TODO: implement recording of rebalancing journeys for visualization

    //NavigableMap<String, NavigableMap<Double, AVStatus>> travelTimes = new TreeMap<>();
    NavigableMap<String, NavigableMap<Double, Double>> travelTimes = new TreeMap<>();
    NavigableMap<String, Double> traveledTime = new TreeMap<>();
    NavigableMap<String, Double> binnedData = new TreeMap<>();
    HashMap<String, Set<Id<Person>>> vehicleCustomers = new HashMap<>();
    HashMap<String, Double> avTripStart = new HashMap<>();
    NavigableMap<Double, String> keyMap = new TreeMap<>();
    double totalTimeWithCust = 0;
    double totalTimeRatio;
    int numAVs = 0;
    double binSize = 600;

    //equalize length of all key elements
    DecimalFormat keyForm = new DecimalFormat("#000000");



    double getBinStart(double time) {
        return keyMap.floorKey(time);
    }

    String writeKey(double binStart) {
        String key = String.valueOf(keyForm.format(binStart)) + " - " + String.valueOf(keyForm.format(binStart + binSize));
        return key;
    }

    String getKey(double time) {
        return keyMap.get(keyMap.floorKey(time));
    }


    // TODO: fix this and use in initialize or work around with calculateTimeRatio later
    void getNumAVs() {
        SAXBuilder builder = new SAXBuilder();
        File readFile = new File(EventFileToDataXML.path, "av.xml");
        //File readFile = new File("C:/Users/Joel/Documents/Studium/ETH/Bachelorarbeit/Simulation_Data/2017_02_07_Sioux_onlyUnitCapacityAVs/av.xml");
        System.out.println("trying to read " + EventFileToDataXML.path + "\\av.xml");
        try {
            Document document = (Document) builder.build(readFile);
            Element rootNode = document.getRootElement();
            Element avXML = rootNode.getChild("operator");
            double numAV = Integer.parseInt(avXML.getChild("generator").getChild("param").getAttributeValue("value"));
            System.out.println("found " + numAV + " with getNumAVs()");
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void calculateTimeRatio() {
       if(!(numAVs == 0)) {
            // get number of AVs with avTripStart.size()
            totalTimeRatio = totalTimeWithCust / (numAVs * 108000);
        } else System.out.println("no AVs found while calculating the time ratio");
    }

    double calculateTimeRatio(double deltaT) {
        double ratio = 0;
        if(!(numAVs == 0)) {
            // get number of AVs with avTripStart.size()
            ratio = deltaT / (numAVs * binSize);
        } // else System.out.println("no AVs found while calculating the time ratio");
        return ratio;
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

    void checkFull(NavigableMap<String, Double> map) {
        double binStart = 0;
        boolean full = true;
        while (binStart < 108000) {
            if (!map.containsKey(getKey(binStart))) full = false;
            binStart += binSize;
        }
        if (!full) System.out.println("not all time bins contain values\n" +
                "\t- created bins are not continuous\n" +
                "\t- increasing binSize could resolve this");
    }

    private void put(String vehicle, double startTime, double endTime) {
        if (!travelTimes.containsKey(vehicle))
            travelTimes.put(vehicle, new TreeMap<>());

        if(endTime <= getBinStart(startTime) + binSize) {
            travelTimes.get(vehicle).put(startTime, endTime);
            putAdd(traveledTime, getKey(startTime), endTime - startTime);
        } else {
            double nextBin = getBinStart(startTime) + binSize;
            travelTimes.get(vehicle).put(startTime, nextBin);
            putAdd(traveledTime, getKey(startTime), nextBin - startTime);
            while (endTime > nextBin + binSize) {
                travelTimes.get(vehicle).put(nextBin, nextBin + binSize);
                putAdd(traveledTime, getKey(nextBin), binSize);
                nextBin += binSize;
            }
            travelTimes.get(vehicle).put(nextBin, endTime);
            putAdd(traveledTime, getKey(nextBin), endTime - nextBin);
        }
        if (endTime <= 108000) {
            totalTimeWithCust += endTime - startTime;
        } else{
            totalTimeWithCust += 108000 - startTime;
        }
        //System.out.println("time with customer: " + totalTimeWithCust);
    }

    private void putAdd(NavigableMap<String, Double> map, String key, double value) {
        double currValue = 0;
        if (map.containsKey(key)) currValue = map.get(key);
        map.put(key, currValue + value);
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
            // initialize keyMap
            double binStart = 0;
            while (binStart < 108000) {
                keyMap.put(binStart, writeKey(binStart));
                binStart += binSize;
            }

            //getNumAVs();


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

        File fileExport1 = new File(directory, "binnedTravelTimes.xml");
        File fileExport2 = new File(directory, "binnedTimeRatios.xml");

        // export to node-based XML file
        new TravelTimesXML().generate(travelTimes, fileExport1);

        //System.out.println("combined time with customers: " + totalTimeWithCust);

        numAVs = avTripStart.size();
        System.out.println("number of AVs: " + numAVs);
        calculateTimeRatio();
        //System.out.println("total time ratio: " + totalTimeRatio);

        // work around since getNumAVs does not work
        for(String key: traveledTime.keySet()) {

            GlobalAssert.that(traveledTime.get(key) <= numAVs*binSize);

            //System.out.println("traveled time in bin " + key +": " + traveledTime.get(key) + "; of maximally: " + numAVs*binSize);
            binnedData.put(key, calculateTimeRatio(traveledTime.get(key)));
        }

        // export to node-based XML file
        NavigableMap<String, NavigableMap<String, Double>> binnedRatios = new TreeMap<>();
        checkFull(binnedData);
        binnedRatios.put("bins", binnedData);
        new BinnedRatiosXML("binnedTimeRatio").generate(binnedRatios, fileExport2);

        // export to time series diagram PNG
        TimeDiagramCreator diagram = new TimeDiagramCreator();
        try{
            diagram.createDiagram(directory, "binnedTimeRatios", "current time ratios", binnedData);
        }catch (Exception e){
            System.out.println("Error creating the diagram");
        }

    }
}
