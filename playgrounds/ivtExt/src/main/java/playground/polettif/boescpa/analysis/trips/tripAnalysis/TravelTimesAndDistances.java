package playground.polettif.boescpa.analysis.trips.tripAnalysis;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import playground.polettif.boescpa.analysis.trips.Trip;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Analyzes trips for distances and durations.
 *
 * @author boescpa
 */
public class TravelTimesAndDistances {
    private static Logger log = Logger.getLogger(TravelTimesAndDistances.class);

    /**
     * Calculates the total travel distance and travel time per mode
     * for a given population based on a given events file.
     *
     * The inputs are:
     * 	- tripData: A TripHandler containing the trips read from an events file
     * 	- network: The network used for the simulation
     * 	- outFile: Path to the File where the calculated values will be written
     * 		IMPORTANT: outFile will be overwritten if already existing.
     *
     * If an agent doesn't finish its trip (endLink = null), this trip is not considered
     * for the total travel distances and times.
     *
     * If an agent is a pt-driver ("pt" part of id), the agent is not considered in the calculation.
     *
     * @return HashMap with (String, key) mode and (Double[], value) [time,distance] per mode
     */
    public static HashMap<String, Double[]> calcTravelTimeAndDistance(List<Trip> trips, String pathToValueFile) {
        HashMap<String, Double> timeMode = new HashMap<>();
        HashMap<String, Double> distMode = new HashMap<>();

        log.info("Analyzing trips...");
        for (Trip tempTrip : trips) {
            if (tempTrip.endLinkId != null) {
                String mode = tempTrip.mode;

                // travel time per mode [seconds]
                double travelTime = tempTrip.duration;

                // distance per mode [meters]
                double travelDistance = tempTrip.distance;

                // store new values
                if (timeMode.containsKey(mode)) {
                    travelTime = timeMode.get(mode) + travelTime;
                    travelDistance = distMode.get(mode) + travelDistance;
                }
                timeMode.put(mode, travelTime);
                distMode.put(mode, travelDistance);
            }
        }

        // ----------- Write Output -----------
        // Write logging:
        log.info("Travel times per mode:");
        for (String mode : timeMode.keySet()) {
            log.info("Mode " + mode + ": " + String.valueOf(timeMode.get(mode)) + " min");
        }
        log.info("Travel distances per mode:");
        for (String mode : distMode.keySet()) {
            log.info("Mode " + mode + ": " + String.valueOf(distMode.get(mode)) + " m");
        }
        // Write to file:
        try {
            final BufferedWriter out = IOUtils.getBufferedWriter(pathToValueFile);
            out.write("Travel times per mode:"); out.newLine();
            for (String mode : timeMode.keySet()) {
                out.write(" - Mode " + mode + ": " + String.valueOf(timeMode.get(mode)) + " min");
                out.newLine();
            }
            out.write("Travel distances per mode:"); out.newLine();
            for (String mode : distMode.keySet()) {
                out.write(" - Mode " + mode + ": " + String.valueOf(distMode.get(mode)) + " m");
                out.newLine();
            }
            out.close();
        } catch (IOException e) {
            log.info("IOException. Could not write topdad-analysis summary to file.");
        }

        log.info("Analyzing trips... done.");
        HashMap<String, Double[]> result = new HashMap<>();
        for (String mode : distMode.keySet()) {
            Double[] val = {timeMode.get(mode), distMode.get(mode)};
            result.put(mode, val);
        }
        return result;
    }

}
