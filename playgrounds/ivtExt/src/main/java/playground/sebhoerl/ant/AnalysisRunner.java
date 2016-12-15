package playground.sebhoerl.ant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import playground.sebhoerl.ant.handlers.*;
import playground.sebhoerl.av_paper.BinCalculator;
import playground.sebhoerl.mexec.Scenario;

import java.io.File;
import java.io.IOException;

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
        events.addHandler(new WaitingHandler(dataFrame));

        reader.readFile(eventsPath);
        events.resetHandlers(0);

        try {
            (new ObjectMapper()).writeValue(new File(outputPath), dataFrame);
        } catch (IOException e) {}
    }
}
