package playground.polettif.boescpa.analysis.trips;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a trip file and returns a list containing the trips.
 *
 * @author boescpa
 */
public class TripReader {
    private static Logger log = Logger.getLogger(TripReader.class);

    private static boolean alreadyWarned = false;

    /**
     * Reads a trip file into the memory and provides the trips in the form of a List.
     *
     * @param sourceTripFile
     * @return HashMap<Long,Trip> - Key: TripId, Value: Trip
     */
    public static List<Trip> createTripCollection(String sourceTripFile) {
        List<Trip> tripCollection = new ArrayList<>();
        FileReader reader;
        try {
            reader = new FileReader(sourceTripFile);
            BufferedReader readsLines = new BufferedReader(reader);
            String newLine = readsLines.readLine(); // Header is read...
            newLine = readsLines.readLine();
            while (newLine != null) {
                try {
                    tripCollection.add(Trip.parseTrip(newLine));
                } catch (Exception e) {
                    if (!alreadyWarned) {
                        log.error("Trip file contains entries that can not be read.");
                        alreadyWarned = true;
                    }
                }
                newLine = readsLines.readLine();
            }
            readsLines.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tripCollection;
    }
}
