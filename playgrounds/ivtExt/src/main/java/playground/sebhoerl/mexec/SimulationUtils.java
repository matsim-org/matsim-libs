package playground.sebhoerl.mexec;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;

import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class SimulationUtils {
    static public void processEvents(EventsManager eventsManager, Simulation simulation, Long iteration ) {
        EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
        try {
            reader.parse(new GZIPInputStream(simulation.getEvents(iteration)));
        } catch (IOException e) {
            throw new RuntimeException("Error while parsing GZIP");
        }
    }

    static public void processEvents(EventsManager eventsManager, Simulation simulation ) {
        processEvents(eventsManager, simulation, null);
    }
}
