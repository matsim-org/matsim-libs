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
import playground.joel.helpers.*;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by Joel on 28.02.2017.
 */
class BinnedTravelTimes extends AbstractData {

    NavigableMap<String, NavigableMap<Double, Double>> travelTimes = new TreeMap<>();
    NavigableMap<String, Double> traveledTime = new TreeMap<>();
    NavigableMap<String, Double> binnedData = new TreeMap<>();
    HashMap<String, Double> avTripStart = new HashMap<>();
    double totalTimeWithCust = 0;
    double totalTimeRatio;
    int numAVs = 0;
    double binSize = 600;

    KeyMap keyMap = new KeyMap(binSize);

    // cut the total time ratio
    DecimalFormat ratioForm = new DecimalFormat("#.####");


    void calculateTimeRatio() {
       if(!(numAVs == 0)) {
            // get number of AVs with avTripStart.size()
            totalTimeRatio = Double.parseDouble(ratioForm.format(totalTimeWithCust / (numAVs * 108000)));
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

    private double getStartTime(PersonLeavesVehicleEvent event){
        return avTripStart.get(event.getVehicleId().toString());
    }

    private void put(String vehicle, double startTime, double endTime) {
        if (!travelTimes.containsKey(vehicle))
            travelTimes.put(vehicle, new TreeMap<>());

        if(endTime <= keyMap.getBinStart(startTime) + binSize) {
            travelTimes.get(vehicle).put(startTime, endTime);
            binnedHelper.putAdd(traveledTime, keyMap.getKey(startTime), endTime - startTime);
        } else {
            double nextBin = keyMap.getBinStart(startTime) + binSize;
            travelTimes.get(vehicle).put(startTime, nextBin);
            binnedHelper.putAdd(traveledTime, keyMap.getKey(startTime), nextBin - startTime);
            while (endTime > nextBin + binSize) {
                travelTimes.get(vehicle).put(nextBin, nextBin + binSize);
                binnedHelper.putAdd(traveledTime, keyMap.getKey(nextBin), binSize);
                nextBin += binSize;
            }
            travelTimes.get(vehicle).put(nextBin, endTime);
            binnedHelper.putAdd(traveledTime, keyMap.getKey(nextBin), endTime - nextBin);
        }
        if (endTime <= 108000) {
            totalTimeWithCust += endTime - startTime;
        } else totalTimeWithCust += 108000 - startTime;
    }

    @Override
    void initialize(EventsManager events) {
        // add handlers to read vehicle status
        {
            keyMap.initialize();

            // personentersvehicle
            events.addHandler(new PersonEntersVehicleEventHandler() {
                // <event time="21574.0" type="PersonEntersVehicle" person="27114_1" vehicle="av_av_op1_174" />
                // <event time="21589.0" type="PersonEntersVehicle" person="av_av_op1_174" vehicle="av_av_op1_174" />
                @Override
                public void handleEvent(PersonEntersVehicleEvent event) {
                    final Id<Person> person = event.getPersonId();
                    if (HelperPredicates.isHuman(person))
                        setStartTime(event); // remember startTime
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
                    if (HelperPredicates.isHuman(person))
                        put(vehicle, getStartTime(event), event.getTime()); // add both travel times
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

        numAVs = avTripStart.size();
        System.out.println("number of AVs: " + numAVs);
        calculateTimeRatio();

        for(String key: traveledTime.keySet()) {
            GlobalAssert.that(traveledTime.get(key) <= numAVs*binSize);
            binnedData.put(key, calculateTimeRatio(traveledTime.get(key)));
        }

        // export to node-based XML file
        NavigableMap<String, NavigableMap<String, Double>> binnedRatios = new TreeMap<>();
        binnedHelper.checkFull(binnedData, binSize, keyMap);
        binnedRatios.put("bins", binnedData);
        new BinnedRatiosXML("binnedTimeRatio").generate(binnedRatios, fileExport2);

        // export to time series diagram PNG
        TimeDiagramCreator diagram = new TimeDiagramCreator();
        try{
            diagram.createDiagram(directory, "binnedTimeRatios", "occupancy ratio", binnedData);
        }catch (Exception e){
            System.out.println("Error creating the diagram");
        }

        // export to CSV
        CSVcreator csv = new CSVcreator();
        try{
            csv.createCSV(binnedData, directory, "binnedTimeRatios");
        }catch (Exception e){
            System.out.println("Error creating the csv file");
        }

    }
}
