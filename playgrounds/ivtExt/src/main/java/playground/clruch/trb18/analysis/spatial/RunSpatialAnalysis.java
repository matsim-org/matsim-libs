package playground.clruch.trb18.analysis.spatial;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import playground.clruch.trb18.analysis.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class RunSpatialAnalysis {
    static public void main(String[] args) throws IOException {
        String networkInputPath = args[0];
        String baselineEventsInputPath = args[1];
        String eventsInputPath = args[2];
        String outputPath = args[3];

        Map<Id<Person>, Queue<ReferenceReader.ReferenceTrip>> referenceTrips = ReferenceReader.getReferenceTravelTimes(baselineEventsInputPath);

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkInputPath);

        EventsManager eventsManager = EventsUtils.createEventsManager();

        SpatialHandler spatialHandler = new SpatialHandler(network, referenceTrips);
        eventsManager.addHandler(spatialHandler);

        new MatsimEventsReader(eventsManager).readFile(eventsInputPath);
        spatialHandler.write(new File(outputPath));
    }
}
