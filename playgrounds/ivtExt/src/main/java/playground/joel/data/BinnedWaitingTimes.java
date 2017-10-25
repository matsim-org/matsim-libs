package playground.joel.data;


import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.clruch.utils.HelperPredicates;
import playground.joel.helpers.CSVcreator;
import playground.joel.helpers.KeyMap;
import playground.joel.helpers.BinnedHelper;


/**
 * Created by Joel on 10.03.2017.
 */
class BinnedWaitingTimes extends AbstractData {

    // From the existing output_events file, load the data
    List<Event> relevantEvents = new ArrayList<>();
    Map<String, NavigableMap<Double, Integer>> waitDelta = new TreeMap<>();

    NavigableMap<String, NavigableMap<String, Double>> waitingTimes = new TreeMap<>(); // <bin, <deltaKey, waitingTime>>
    HashMap<String, Double> waitStart = new HashMap<>(); // <customerId, waitingStart>
    NavigableMap<String, Double> quantile50 = new TreeMap<>();
    NavigableMap<String, Double> quantile95 = new TreeMap<>();
    NavigableMap<String, Double> mean = new TreeMap<>();

    static double totalQuantile50 = 0;
    static double totalQuantile95 = 0;
    static double totalMean = 0;
    double binSize = 600;

    double maxWait = 5000; // maximally expected waiting time to have plots of the same range

    KeyMap keyMap = new KeyMap(binSize);

    // cut the total quantiles and mean
    DecimalFormat valueForm = new DecimalFormat("#.####");

    
    String getDeltaKey(String key, double deltaT) {
        // necessary in case multiple waiting times of the same length exist
        int i = 1;
        String deltaKey = String.valueOf(keyMap.keyForm.format(deltaT)) + "_" + String.valueOf(i);
        while (waitingTimes.get(key).containsKey(deltaKey)) {
            i++;
            deltaKey = String.valueOf(keyMap.keyForm.format(deltaT)) + "_" + String.valueOf(i);
        }
        return deltaKey;
    }

    double getEntry(String key, int i) {
        Double value = 0.0;
        int j = 0;
        for (String deltaKey : waitingTimes.get(key).keySet()) {
            if (j++ == i) {
                if (waitingTimes.get(key).containsKey(deltaKey))
                    value = waitingTimes.get(key).get(deltaKey);
                else System.out.println("could not find any value for deltaKey " + deltaKey + " in bin " + key);
            }
        }
        return value;
    } // gives the i_th entry of waitingTimes.get(key) for the quantile

    void writeQuantiles() {
        for (String bin : waitingTimes.keySet()) {
            // calculate the quantiles
            int size = waitingTimes.get(bin).size();
            int element50 = (int) (size * 0.5);
            int element95 = (int) (size * 0.95);
            double sum = 0;

            for (String wait : waitingTimes.get(bin).keySet()) sum += waitingTimes.get(bin).get(wait);

            if (!(bin == "all")) {
                quantile50.put(bin, getEntry(bin, element50));
                quantile95.put(bin, getEntry(bin, element95));
                mean.put(bin, sum/size);
            } else {
                totalQuantile50 = Double.parseDouble(valueForm.format(getEntry(bin, element50)));
                totalQuantile95 = Double.parseDouble(valueForm.format(getEntry(bin, element95)));
                totalMean = Double.parseDouble(valueForm.format(sum/size));
            }
        }
    }



    @Override
    void initialize(EventsManager events) {
        // add handlers to read waiting customer queues
        {
            keyMap.initialize();

            // read the events when person calls AVs
            events.addHandler(new PersonDepartureEventHandler() {

                @Override
                public void reset(int iteration) {
                    // empty content
                }

                @Override
                public void handleEvent(PersonDepartureEvent event) {
                    final Id<Person> idRaw = event.getPersonId();
                    final String id = idRaw.toString();
                    if (HelperPredicates.isHuman(idRaw))
                        waitStart.put(id, event.getTime());
                }
            });

            // read the events when AV arrives at agent's location
            events.addHandler(new PersonEntersVehicleEventHandler() {

                @Override
                public void reset(int iteration) {

                }

                @Override
                public void handleEvent(PersonEntersVehicleEvent event) {
                    relevantEvents.add(event);
                    final Id<Person> idRaw = event.getPersonId();
                    final String id = idRaw.toString();

                    if (HelperPredicates.isHuman(idRaw)) {
                        String key = keyMap.getKey(waitStart.get(id));
                        if (!waitingTimes.containsKey("all")) waitingTimes.put("all", new TreeMap<>());
                        if (!waitingTimes.containsKey( key ))  waitingTimes.put(key, new TreeMap<>());

                        double deltaT = event.getTime() - waitStart.get(id);
                        waitingTimes.get("all").put(getDeltaKey("all", deltaT), deltaT);
                        waitingTimes.get(key).put(getDeltaKey(key, deltaT), deltaT);

                        waitStart.remove(id);
                    }
                }
            });
        }
    }


    @Override
    void writeXML(File directory) {
        File fileExport1 = new File(directory, "binnedQuantiles50.xml");
        File fileExport2 = new File(directory, "binnedQuantiles95.xml");
        File fileExport3 = new File(directory, "binnedMeans.xml");

        writeQuantiles();

        // export to node-based XML file
        NavigableMap<String, NavigableMap<String, Double>> quantiles50 = new TreeMap<>();
        NavigableMap<String, NavigableMap<String, Double>> quantiles95 = new TreeMap<>();
        NavigableMap<String, NavigableMap<String, Double>> means = new TreeMap<>();
        BinnedHelper.checkFull(quantile50, binSize, keyMap);
        BinnedHelper.checkFull(quantile95, binSize, keyMap);
        BinnedHelper.checkFull(mean, binSize, keyMap);
        quantiles50.put("bins", quantile50);
        quantiles95.put("bins", quantile95);
        means.put("bins", mean);
        new BinnedRatiosXML("binnedQuantiles50", "quantiles50").generate(quantiles50, fileExport1);
        new BinnedRatiosXML("binnedQuantiles95", "quantiles95").generate(quantiles95, fileExport2);
        new BinnedRatiosXML("binnedMeans", "means").generate(means, fileExport3);

        // export to time series diagram PNG
        try{
            TimeDiagramCreator.createDiagram(directory, "binnedWaitingTimes", "waiting time", quantile50, quantile95, mean, maxWait);
        }catch (Exception e){
            System.out.println("Error creating the diagram");
        }

        // export to CSV
        CSVcreator csv = new CSVcreator();
        try{
            csv.createCSV(quantile50, quantile95, mean, directory, "binnedWaitingTimes");
        }catch (Exception e){
            System.out.println("Error creating the csv file");
        }

        if (waitStart.size() != 0) System.out.println("there remain " + waitStart.size() + " waiting customers after the simulation has ended");
        else System.out.println("no remaining customers waiting at the end of the simulation");
    }
}
