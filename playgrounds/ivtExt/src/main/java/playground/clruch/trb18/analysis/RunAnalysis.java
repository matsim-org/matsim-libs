package playground.clruch.trb18.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import playground.sebhoerl.av_paper.BinCalculator;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Queue;

public class RunAnalysis {
    static public void main(String[] args) throws IOException {
        String networkInputPath = args[0];
        String baselineEventsInputPath = args[1];
        String eventsInputPath = args[2];
        String outputPath = args[3];

        Map<Id<Person>, Queue<ReferenceReader.ReferenceTrip>> referenceTrips = ReferenceReader.getReferenceTravelTimes(baselineEventsInputPath);

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkInputPath);

        BinCalculator binCalculator = BinCalculator.createByInterval(0.0, 30.0 * 3600.0, 300.0);

        DataFrame dataFrame = new DataFrame(binCalculator);

        EventsManager eventsManager = EventsUtils.createEventsManager();

        DistanceHandler distanceHandler = new DistanceHandler(dataFrame, binCalculator, network);
        PassengerHandler passengerHandler = new PassengerHandler(dataFrame, binCalculator);
        DelayHandler delayHandler = new DelayHandler(dataFrame, binCalculator, referenceTrips);

        eventsManager.addHandler(distanceHandler);
        eventsManager.addHandler(passengerHandler);
        eventsManager.addHandler(delayHandler);

        new MatsimEventsReader(eventsManager).readFile(eventsInputPath);
        passengerHandler.finish();
        delayHandler.finish();

        new ObjectMapper().writeValue(new File(outputPath), dataFrame);
    }
}
