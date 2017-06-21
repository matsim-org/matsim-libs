package playground.dziemke.analysis.general.matsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gthunig on 06.04.2017.
 */
public class Events2TripsParser {
    public static final Logger log = Logger.getLogger(Events2TripsParser.class);

    private Network network;
    private List<FromMatsimTrip> trips;

    private int noPreviousEndOfActivityCounter;

    public Events2TripsParser(String configFile, String eventsFile, String networkFile) {
        parse(configFile, eventsFile, networkFile);
    }

    private void parse(String configFile, String eventsFile, String networkFile) {
        /* Events infrastructure and reading the events file */
        EventsManager eventsManager = EventsUtils.createEventsManager();
        TripHandler tripHandler = new TripHandler();
        eventsManager.addHandler(tripHandler);
        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        eventsReader.readFile(eventsFile);
        noPreviousEndOfActivityCounter = tripHandler.getNoPreviousEndOfActivityCounter();
        log.info("Events file read!");

	    /* Get network, which is needed to calculate distances */
        network = NetworkUtils.createNetwork();
        MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
        networkReader.readFile(networkFile);

        List<FromMatsimTrip> trips = new ArrayList<>(tripHandler.getTrips().values());

        Config config = ConfigUtils.createConfig();
        ConfigReader configReader = new ConfigReader(config);
        configReader.readFile(configFile);

        TripInformationCalculator.calculateInformation(trips, network, config.plansCalcRoute().getNetworkModes());

        this.trips = trips;
    }

    public Network getNetwork() {
        return network;
    }

    public List<FromMatsimTrip> getTrips() {
        return trips;
    }

    public int getNoPreviousEndOfActivityCounter() {
        return noPreviousEndOfActivityCounter;
    }
}
