package playground.sebhoerl.ant;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;

import com.fasterxml.jackson.databind.ObjectMapper;

import playground.sebhoerl.ant.handlers.CountsHandler;
import playground.sebhoerl.ant.handlers.DistanceHandler;
import playground.sebhoerl.ant.handlers.IdleHandler;
import playground.sebhoerl.ant.handlers.OccupancyHandler;
import playground.sebhoerl.ant.handlers.TimeHandler;
import playground.sebhoerl.av_paper.BinCalculator;

public class AnalysisRunner implements Runnable {
    final private BinCalculator binCalculator;
    final private Network network;
    final private String eventsPath;
    final private String outputPath;

    final private DataFrame dataFrame;

    public AnalysisRunner(BinCalculator binCalculator, Network network, String eventsPath, String outputPath) {
        this.binCalculator = binCalculator;
        this.network = network;
        this.eventsPath = eventsPath;
        this.outputPath = outputPath;

        this.dataFrame = new DataFrame(binCalculator);
    }

    @Override
    public void run() {
        EventsManagerImpl events = new EventsManagerImpl();
        MatsimEventsReader reader = new MatsimEventsReader(events);

        events.addHandler(new CountsHandler(dataFrame));
        events.addHandler(new DistanceHandler(dataFrame, network));
        events.addHandler(new IdleHandler(dataFrame));
        events.addHandler(new OccupancyHandler(dataFrame));
        events.addHandler(new TimeHandler(dataFrame));

        reader.readFile(eventsPath);
        events.resetHandlers(0);

        try {
            (new ObjectMapper()).writeValue(new File(outputPath), dataFrame);
        } catch (IOException e) {}
    }
}
