package playground.dziemke.analysis.general.matsim;

import org.matsim.api.core.v01.network.Network;

import java.util.List;

/**
 * @author gthunig on 06.04.2017.
 */
public class TripInformationCalculator {

    public static void calculateInformation(List<FromMatsimTrip> trips, Network network) {
        for (FromMatsimTrip trip : trips)
            calculateInformation(trip, network);
    }

    private static void calculateInformation(FromMatsimTrip trip, Network network) {
        trip.setDuration_s(FromMatsimTripUtils.getDurationByCalculation_s(trip));
        trip.setDistanceBeeline_m(FromMatsimTripUtils.calculateBeelineDistance_m(trip, network));
        //TODO calculate fastest instead of shortest route, dziemke/gthunig 04.17
        trip.setDistanceRouted_m(FromMatsimTripUtils.getDistanceRoutedByCalculation_m(trip, network));
    }
}
