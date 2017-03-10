package playground.joel.data;


import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.clruch.utils.HelperPredicates;


/**
 * Created by Joel on 10.03.2017.
 */
public class BinnedWaitingTimes extends AbstractData {

    // From the existing output_events file, load the data
    List<Event> relevantEvents = new ArrayList<>();
    Map<String, NavigableMap<Double, Integer>> waitDelta = new TreeMap<>();

    NavigableMap<Double, String> keyMap = new TreeMap<>();
    NavigableMap<String, NavigableMap<String, Double>> waitingTimes = new TreeMap<>(); // <bin, <deltaKey, waitingTime>>
    HashMap<String, Double> waitStart = new HashMap<>(); // <customerId, waitingStart>
    NavigableMap<String, Double> quantile50 = new TreeMap<>();
    NavigableMap<String, Double> quantile95 = new TreeMap<>();
    NavigableMap<String, Double> mean = new TreeMap<>();

    double totalQuantile50 = 0;
    double totalQuantile95 = 0;
    double totalMean = 0;
    double binSize = 600;

    //equalize length of all key elements
    DecimalFormat keyForm = new DecimalFormat("#000000");
    // cut the total quantiles and mean
    DecimalFormat valueForm = new DecimalFormat("#.####");



    String writeKey(double binStart) {
        String key = String.valueOf(keyForm.format(binStart)) + " - " + String.valueOf(keyForm.format(binStart + binSize));
        return key;
    }

    String getKey(double time) {
        return keyMap.get(keyMap.floorKey(time));
    }

    String getDeltaKey(String key, double deltaT) {
        // necessary in case multiple waiting times of the same length exist
        int i = 1;
        String deltaKey = String.valueOf(keyForm.format(deltaT)) + "_" + String.valueOf(i);
        while (waitingTimes.get(key).containsKey(deltaKey)) {
            i++;
            deltaKey = String.valueOf(keyForm.format(deltaT)) + "_" + String.valueOf(i);
        }
        return deltaKey;
    }

    // gives the i_th entry of waitingTimes.get(key) for the quantile
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
    }

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



    @Override
    void initialize(EventsManager events) {
        // add handlers to read waiting customer queues
        {
            // initialize keyMap
            double binStart = 0;
            while (binStart < 108000) {
                keyMap.put(binStart, writeKey(binStart));
                binStart += binSize;
            }


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
                    if (HelperPredicates.isHuman(idRaw)) {
                        waitStart.put(id, event.getTime());
                        //System.out.println("added person " + id + " to waitStart");
                    }
                }
            });

            /*
            // read the events when agent arrives at destination
            events.addHandler(new PersonArrivalEventHandler() {

                @Override
                public void reset(int iteration) {
                    // empty content
                }

                @Override
                public void handleEvent(PersonArrivalEvent event) {
                    relevantEvents.add(event);
                }
            });
            */

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
                        //System.out.println("person " + id + " enters vehicle");
                        String key = getKey(waitStart.get(id));
                        if (!waitingTimes.containsKey("all")) waitingTimes.put("all", new TreeMap<>());
                        if (!waitingTimes.containsKey( key ))  waitingTimes.put(key, new TreeMap<>());

                        double deltaT = event.getTime() - waitStart.get(id);
                        waitingTimes.get("all").put(getDeltaKey("all", deltaT), deltaT);
                        waitingTimes.get(key).put(getDeltaKey(key, deltaT), deltaT);

                        waitStart.remove(id);
                        //System.out.println("removed person " + id + " from waitStart");
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
        checkFull(quantile50);
        checkFull(quantile95);
        checkFull(mean);
        quantiles50.put("bins", quantile50);
        quantiles95.put("bins", quantile95);
        means.put("bins", mean);
        new BinnedRatiosXML("binnedQuantiles50", "quantiles50").generate(quantiles50, fileExport1);
        new BinnedRatiosXML("binnedQuantiles95", "quantiles95").generate(quantiles95, fileExport2);
        new BinnedRatiosXML("binnedMeans", "means").generate(means, fileExport3);

        // export to time series diagram PNG
        TimeDiagramCreator diagram = new TimeDiagramCreator();
        try{
            diagram.createDiagram(directory, "binnedWaitingTimes", "current quantiles and mean", quantile50, quantile95, mean);
        }catch (Exception e){
            System.out.println("Error creating the diagram");
        }

    }
}
