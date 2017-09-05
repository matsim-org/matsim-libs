package playground.clruch.trb18.traveltime.reloading;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPOutputStream;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.router.util.TravelTime;

public class TravelTimeWriter implements IterationEndsListener {
    final private Network network;
    final private TravelTime travelTime;
    final private long iterationInterval;
    final private OutputDirectoryHierarchy outputDirectoryHierarchy;
    final private double measurementInterval;
    final private double maximumMeasurementTime;

    public TravelTimeWriter(OutputDirectoryHierarchy outputDirectoryHierarchy, Network network, TravelTime travelTime, long iterationInterval, double measurementInterval, double maximumMeasurementTime) {
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        this.network = network;
        this.travelTime = travelTime;
        this.iterationInterval = iterationInterval;
        this.measurementInterval = measurementInterval;
        this.maximumMeasurementTime = maximumMeasurementTime;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        try {
            if (event.getIteration() % iterationInterval == 0 || event.getIteration() == 0) {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "travelTimes.txt.gz")))));

                for (Link link : network.getLinks().values()) {
                    writer.write(link.getId().toString());

                    double time = 0.0;

                    while (time <= maximumMeasurementTime) {
                        writer.write(" ");
                        writer.write(String.valueOf(travelTime.getLinkTravelTime(link, time, null, null)));
                        time += measurementInterval;
                    }

                    writer.write("\n");
                    writer.flush();
                }

                writer.close();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
