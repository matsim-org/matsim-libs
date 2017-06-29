package playground.clruch.trb18.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import playground.sebhoerl.av_paper.BinCalculator;

import java.io.File;
import java.io.IOException;

public class RunAnalysis {
    static public void main(String[] args) throws IOException {
        String networkInputPath = args[0];
        String eventsInputPath = args[1];
        String outputPath = args[2];

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkInputPath);

        BinCalculator binCalculator = BinCalculator.createByInterval(0.0, 30.0 * 3600.0, 300.0);
        DataFrame dataFrame = new DataFrame(binCalculator);

        EventsManager eventsManager = EventsUtils.createEventsManager();

        DistanceHandler distanceHandler = new DistanceHandler(dataFrame, binCalculator, network);
        PassengerHandler passengerHandler = new PassengerHandler(dataFrame, binCalculator);

        eventsManager.addHandler(distanceHandler);
        eventsManager.addHandler(passengerHandler);

        new MatsimEventsReader(eventsManager).readFile(eventsInputPath);
        passengerHandler.finish();

        new ObjectMapper().writeValue(new File(outputPath), dataFrame);
    }
}
