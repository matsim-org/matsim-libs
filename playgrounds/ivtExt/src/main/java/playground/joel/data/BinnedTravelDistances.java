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
import playground.joel.helpers.*;

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
    HashMap<String, Double> avLinkStart = new HashMap<>();
    double totalDistance = 0;
    double totalDistanceWithCust = 0;
    double totalDistanceRatio;
    double binSize = 600;

    KeyMap keyMap = new KeyMap(binSize);

    // cut the total distance ratio
    DecimalFormat valueForm = new DecimalFormat("#.####");


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

    void calculateDistanceRatio() {
        if (!(totalDistance == 0)) {
            totalDistanceRatio = Double.parseDouble(valueForm.format(totalDistanceWithCust / totalDistance));
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

    private void put(Id linkId, String vehicle, double startTime, double endTime) {
        totalDistance += getLinkLength(linkId);
        if (!vehicleStatus.containsKey(vehicle)) vehicleStatus.put(vehicle, 0); // replaces activitystart
        if (vehicleStatus.get(vehicle) == 1) totalDistanceWithCust += getLinkLength(linkId);

        if (!travelDistances.containsKey(vehicle))
            travelDistances.put(vehicle, new TreeMap<>());

        if(endTime <= keyMap.getBinStart(startTime) + binSize) {
            binnedHelper.putAdd(travelDistances.get(vehicle), keyMap.getKey(startTime), getLinkLength(linkId));
            binnedHelper.putAdd(traveledDistance, keyMap.getKey(startTime), getLinkLength(linkId));
            if (vehicleStatus.get(vehicle) == 1) binnedHelper.putAdd(traveledDistanceWithCust, keyMap.getKey(startTime), getLinkLength(linkId));
        } else {
            double nextBin = keyMap.getBinStart(startTime) + binSize;
            binnedHelper.putAdd(travelDistances.get(vehicle), keyMap.getKey(startTime), interpolateLinkLength(startTime, endTime, nextBin - startTime, linkId));
            binnedHelper.putAdd(traveledDistance, keyMap.getKey(startTime), interpolateLinkLength(startTime, endTime, nextBin - startTime, linkId));
            if (vehicleStatus.get(vehicle) == 1) binnedHelper.putAdd(traveledDistanceWithCust, keyMap.getKey(startTime), interpolateLinkLength(startTime, endTime, nextBin - startTime, linkId));

            while (endTime > nextBin + binSize) {
                binnedHelper.putAdd(travelDistances.get(vehicle), keyMap.getKey(nextBin), interpolateLinkLength(startTime, endTime, binSize, linkId));
                binnedHelper.putAdd(traveledDistance, keyMap.getKey(nextBin), interpolateLinkLength(startTime, endTime, binSize, linkId));
                if (vehicleStatus.get(vehicle) == 1) binnedHelper.putAdd(traveledDistanceWithCust, keyMap.getKey(nextBin), interpolateLinkLength(startTime, endTime, binSize, linkId));
                nextBin += binSize;
            }

            binnedHelper.putAdd(travelDistances.get(vehicle), keyMap.getKey(nextBin), interpolateLinkLength(startTime, endTime, endTime - nextBin, linkId));
            binnedHelper.putAdd(traveledDistance, keyMap.getKey(nextBin), interpolateLinkLength(startTime, endTime, endTime - nextBin, linkId));
            if (vehicleStatus.get(vehicle) == 1) binnedHelper.putAdd(traveledDistanceWithCust, keyMap.getKey(nextBin), interpolateLinkLength(startTime, endTime, endTime - nextBin, linkId));
        }
        //System.out.println("distance with customer: " + totalDistanceWithCust);
    }

    private void putDriveWithCustomer(PersonEntersVehicleEvent event) {
        if (HelperPredicates.isHuman(event.getPersonId()))
            vehicleStatus.put(event.getVehicleId().toString(), 1);
    } // switch vehicle status to 1

    private void putDriveToCustomer(PersonLeavesVehicleEvent event) {
        vehicleStatus.put(event.getVehicleId().toString(), 0);
    } // switch vehicle status to 0


    @Override
    void initialize(EventsManager events) {
        // add handlers to read vehicle status
        {
            keyMap.initialize();

            readLinkLengths();

            // personentersvehicle
            events.addHandler(new PersonEntersVehicleEventHandler() {
                // <event time="21574.0" type="PersonEntersVehicle" person="27114_1" vehicle="av_av_op1_174" />
                // <event time="21589.0" type="PersonEntersVehicle" person="av_av_op1_174" vehicle="av_av_op1_174" />
                @Override
                public void handleEvent(PersonEntersVehicleEvent event) {
                    final Id<Person> person = event.getPersonId();
                    if (HelperPredicates.isHuman(person))
                        putDriveWithCustomer(event); // switch vehicle status to 1
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
                    final Id<Person> person = event.getPersonId();
                    if (HelperPredicates.isHuman(person))
                        putDriveToCustomer(event); // switch vehicle status to 0
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

        }

    }

    @Override
    void writeXML(File directory) {

        File fileExport = new File(directory, "binnedDistanceRatios.xml");

        calculateDistanceRatio();

        for(String key: traveledDistanceWithCust.keySet()) {
            GlobalAssert.that(traveledDistance.get(key) != 0);
            binnedData.put(key, calculateDistanceRatio(key));
        }

        // export to node-based XML file
        NavigableMap<String, NavigableMap<String, Double>> binnedRatios = new TreeMap<>();
        binnedHelper.checkFull(binnedData, binSize, keyMap);
        binnedRatios.put("bins", binnedData);
        new BinnedRatiosXML("binnedTimeRatio").generate(binnedRatios, fileExport);

        // export to time series diagram PNG
        TimeDiagramCreator diagram = new TimeDiagramCreator();
        try{
            diagram.createDiagram(directory, "binnedDistanceRatios", "distance ratio", binnedData);
        }catch (Exception e){
            System.out.println("Error creating the diagram");
        }

        // export to CSV
        CSVcreator csv = new CSVcreator();
        try{
            csv.createCSV(binnedData, directory, "binnedDistanceRatios");
        }catch (Exception e){
            System.out.println("Error creating the csv file");
        }

    }
}
