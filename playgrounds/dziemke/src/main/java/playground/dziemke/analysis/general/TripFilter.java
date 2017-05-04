package playground.dziemke.analysis.general;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gthunig on 04.04.2017.
 */
public interface TripFilter {

    public List<? extends Trip> filter(List<? extends Trip> tripMap);

    public static List<Trip> castTrips(List<? extends Trip> trips) {
        return new ArrayList<>(trips);
    }
}
