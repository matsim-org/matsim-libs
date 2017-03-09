package playground.joel.data;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.HelperPredicates;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Joel on 28.02.2017.
 */
class BinnedTravelDistances extends AbstractData {

    NavigableMap<String, NavigableMap<String, Double>> travelDistances = new TreeMap<>();
    NavigableMap<String, Double> traveledDistance = new TreeMap<>();
    NavigableMap<String, Double> traveledDistanceWithCust = new TreeMap<>();
    NavigableMap<String, Double> binnedData = new TreeMap<>();
    NavigableMap<String, Integer> vehicleStatus = new TreeMap<>(); // 0 -> empty, 1 -> occupied
    NavigableMap<String, Double> linkLengths = new TreeMap<>();
    HashMap<String, Set<Id<Person>>> vehicleCustomers = new HashMap<>();
    HashMap<String, Double> avLinkStart = new HashMap<>();
    NavigableMap<Double, String> keyMap = new TreeMap<>();
    double totalDistance = 0;
    double totalDistanceWithCust = 0;
    double totalDistanceRatio;
    double binSize = 300;

    //equalize length of all key elements
    DecimalFormat keyForm = new DecimalFormat("#000000");
    // cut the total distance ratio
    DecimalFormat ratioForm = new DecimalFormat("#.####");



    double getLinkLength(Id<Link> linkId) {
        return linkLengths.get(linkId.toString());
    }

    void readLinkLengths() {
        // open the network file and parse the parameter values
        SAXBuilder builder = new SAXBuilder();
        File file = new File(EventFileToDataXML.path, "network.xml");
        try {
            Document document = (Document) builder.build(file);
            Element rootNode = document.getRootElement();
            Element linksXML = rootNode.getChild("links");
            List<Element> linkXML = linksXML.getChildren("link");
            for (Element linkelem : linkXML) {
                String linkID = linkelem.getAttributeValue("id");
                Double length = Double.parseDouble(linkelem.getAttributeValue("length"));

                // find the link with the corresponding ID and assign the length to it.
                //linkLengths.put(linkXML.stream().filter(vl -> vl.getAttributeValue("id").equals(linkID)).findFirst().get().toString(), length);
                linkLengths.put(linkID, length);
                //System.out.println("added " + linkID + ", " + length + " to linkLengths");
            }
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    double interpolateLinkLength(double start, double end, double deltaT, Id<Link> linkId) {
        // interpolates the remaining link length before or after the next bin start
            return getLinkLength(linkId)*deltaT/(end - start);
    }

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



    void calculateDistanceRatio() {
        if (!(totalDistance == 0)) {
            totalDistanceRatio = Double.parseDouble(ratioForm.format(totalDistanceWithCust / totalDistance));
        } else System.out.println("total distance equals zero!");
    }

    double calculateDistanceRatio(String key) {
        double ratio = 0;
        if(traveledDistance.containsKey(key) && traveledDistanceWithCust.containsKey(key)) {
            ratio = traveledDistanceWithCust.get(key)/traveledDistance.get(key);
        } // else System.out.println("entry/entries not found while calculating the time ratio");
        return ratio;
    }

    private void setStartTime(LinkEnterEvent event) {
        avLinkStart.put(event.getVehicleId().toString(), event.getTime());
        //System.out.println("added " + event.getVehicleId().toString() + ", " + event.getTime() + " to avLinkStart");
    }

    private void setStartTime(VehicleEntersTrafficEvent event) {
        avLinkStart.put(event.getPersonId().toString(), event.getTime());
        //System.out.println("added " + event.getPersonId().toString() + ", " + event.getTime() + " to avLinkStart");
    }

    private double getStartTime(LinkLeaveEvent event) {
        if (avLinkStart.containsKey(event.getVehicleId().toString())) {
            return avLinkStart.get(event.getVehicleId().toString());
        } else {
            System.out.println("avLinkStart does not contain the key " + event.getVehicleId().toString());
            return 0;
        }
    }

    private double getStartTime(VehicleLeavesTrafficEvent event) {
        if (avLinkStart.containsKey(event.getVehicleId().toString())) {
            return avLinkStart.get(event.getVehicleId().toString());
        } else {
            System.out.println("avLinkStart does not contain the key " + event.getVehicleId().toString());
            return 0;
        }
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

    private void put(Id linkId, String vehicle, double startTime, double endTime) {
        totalDistance += getLinkLength(linkId);
        if (vehicleStatus.get(vehicle) == 1) totalDistanceWithCust += getLinkLength(linkId);

        if (!travelDistances.containsKey(vehicle))
            travelDistances.put(vehicle, new TreeMap<>());

        if(endTime <= getBinStart(startTime) + binSize) {
            travelDistances.get(vehicle).put(getKey(startTime), getLinkLength(linkId));
            putAdd(traveledDistance, getKey(startTime), getLinkLength(linkId));
            if (!vehicleStatus.containsKey(vehicle)) System.out.println("vehicleStatus does not contain the key " + vehicle);
            if (vehicleStatus.get(vehicle) == 1) putAdd(traveledDistanceWithCust, getKey(startTime), getLinkLength(linkId));
        } else {
            double nextBin = getBinStart(startTime) + binSize;
            travelDistances.get(vehicle).put(getKey(startTime), interpolateLinkLength(startTime, endTime, nextBin - startTime, linkId));
            putAdd(traveledDistance, getKey(startTime), interpolateLinkLength(startTime, endTime, nextBin - startTime, linkId));
            if (vehicleStatus.get(vehicle) == 1) putAdd(traveledDistanceWithCust, getKey(startTime), interpolateLinkLength(startTime, endTime, nextBin - startTime, linkId));

            while (endTime > nextBin + binSize) {
                travelDistances.get(vehicle).put(getKey(nextBin), interpolateLinkLength(startTime, endTime, binSize, linkId));
                putAdd(traveledDistance, getKey(nextBin), interpolateLinkLength(startTime, endTime, binSize, linkId));
                if (vehicleStatus.get(vehicle) == 1) putAdd(traveledDistanceWithCust, getKey(nextBin), interpolateLinkLength(startTime, endTime, binSize, linkId));
                nextBin += binSize;
            }

            travelDistances.get(vehicle).put(getKey(nextBin), interpolateLinkLength(startTime, endTime, endTime - nextBin, linkId));
            putAdd(traveledDistance, getKey(nextBin), interpolateLinkLength(startTime, endTime, endTime - nextBin, linkId));
            if (vehicleStatus.get(vehicle) == 1) putAdd(traveledDistanceWithCust, getKey(nextBin), interpolateLinkLength(startTime, endTime, endTime - nextBin, linkId));
        }
        //System.out.println("distance with customer: " + totalDistanceWithCust);
    }

    private void putAdd(NavigableMap<String, Double> map, String key, double value) {
        double currValue = 0;
        if (map.containsKey(key)) currValue = map.get(key);
        map.put(key, currValue + value);
    }

    private void putDriveWithCustomer(PersonEntersVehicleEvent event) {
        if (HelperPredicates.isHuman(event.getPersonId()))
            vehicleStatus.put(event.getVehicleId().toString(), 1);
    }

    private void putDriveToCustomer(PersonLeavesVehicleEvent event) {
        vehicleStatus.put(event.getVehicleId().toString(), 0);
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

            readLinkLengths();

            // activitystart
            events.addHandler(new ActivityStartEventHandler() {
                // <event time="0.0" type="actstart" person="av_av_op1_174" link="237756569_3" actType="AVStay" />
                @Override
                public void handleEvent(ActivityStartEvent event) {
                    if (event.getTime() == 0 && HelperPredicates.isPersonAV(event.getPersonId())) vehicleStatus.put(event.getPersonId().toString(), 0);

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
                        putDriveWithCustomer(event); // change vehicle status
                    }
                }

                @Override
                public void reset(int iteration) {
                    // intentionally empty
                }
            });

            // TODO: what is the second if for? Is putDriveWithCustomer right there?
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
                        putDriveToCustomer(event); // change vehicle status
                        if (set.isEmpty()) { // last customer has left the car
                            if (firstCustomerEntersVehicleEvent.containsKey(vehicle)) { // change vehicle status
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

            // linkenter
            events.addHandler(new LinkEnterEventHandler() {

                public void handleEvent(LinkEnterEvent event) {
                    setStartTime(event);
                }

                @Override
                public void reset(int iteration) {
                    // intentionally empty
                }
            });

            // linkleave
            events.addHandler(new LinkLeaveEventHandler() {

                public void handleEvent(LinkLeaveEvent event) {
                    final String vehicle = event.getVehicleId().toString();
                    put(event.getLinkId(), vehicle, getStartTime(event), event.getTime());
                }

                @Override
                public void reset(int iteration) {
                    // intentionally empty
                }
            });

            // trafficenter
            events.addHandler(new VehicleEntersTrafficEventHandler() {

                public void handleEvent(VehicleEntersTrafficEvent event) {
                    setStartTime(event);
                }

                @Override
                public void reset(int iteration) {
                    // intentionally empty
                }
            });

            // trafficleave
            events.addHandler(new VehicleLeavesTrafficEventHandler() {

                public void handleEvent(VehicleLeavesTrafficEvent event) {
                    final String vehicle = event.getVehicleId().toString();
                    put(event.getLinkId(), vehicle, getStartTime(event), event.getTime());
                }

                @Override
                public void reset(int iteration) {
                    // intentionally empty
                }
            });



            /* TODO: are these necessary?
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
            */


        }

    }

    @Override
    void writeXML(File directory) {

        File fileExport = new File(directory, "binnedDistanceRatios.xml");

        calculateDistanceRatio();
        //System.out.println("total distance: " + totalDistance);
        //System.out.println("total distance with customer: " + totalDistanceWithCust);
        //System.out.println("total distance ratio: " + totalDistanceRatio);

        // work around since getNumAVs does not work
        for(String key: traveledDistanceWithCust.keySet()) {

            GlobalAssert.that(traveledDistance.get(key) != 0);

            //System.out.println("traveled distance in bin " + key +": " + traveledDistance.get(key));
            //System.out.println("traveled distance with customer in bin " + key +": " + traveledDistanceWithCust.get(key));
            binnedData.put(key, calculateDistanceRatio(key));
        }

        // export to node-based XML file
        NavigableMap<String, NavigableMap<String, Double>> binnedRatios = new TreeMap<>();
        checkFull(binnedData);
        binnedRatios.put("bins", binnedData);
        new BinnedRatiosXML("binnedTimeRatio").generate(binnedRatios, fileExport);

        // export to time series diagram PNG
        TimeDiagramCreator diagram = new TimeDiagramCreator();
        try{
            diagram.createDiagram(directory, "binnedDistanceRatios", "current distance ratios", binnedData);
        }catch (Exception e){
            System.out.println("Error creating the diagram");
        }

    }
}
