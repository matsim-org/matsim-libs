package playground.clruch.trb18.traveltime.reloading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Counter;
import org.matsim.vehicles.Vehicle;

public class TravelTimeReader {
    private class LoadedTravelTime implements TravelTime {
        @Override
        public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
            time = Math.min(Math.max(0.0, time), maximumMeasurementTime - 1e-10);
            int index = (int) Math.floor(time / measurementInterval);
            return data.get(link.getId()).get(index);
        }
    }

    final private double measurementInterval;
    final private double maximumMeasurementTime;
    final private Map<Id<Link>, List<Double>> data = new HashMap<>();

    public TravelTimeReader(double measurementInterval, double maximumMeasurementTime) {
        this.maximumMeasurementTime = maximumMeasurementTime;
        this.measurementInterval = measurementInterval;
    }

    public TravelTime readTravelTimes(File path) {
        Counter counter = new Counter("Travel times for ", " links read");

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path))));
            String line = null;

            while ((line = reader.readLine()) != null) {
                String[] elements = line.split(" ");

                Id<Link> linkId = Id.createLinkId(elements[0]);
                List<Double> travelTimes = Arrays.stream(elements).skip(1).map(Double::parseDouble).collect(Collectors.toList());

                data.put(linkId, travelTimes);
                counter.incCounter();
            }

            return new LoadedTravelTime();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
