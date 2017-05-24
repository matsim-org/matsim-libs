package playground.dziemke.analysis.general;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author gthunig on 04.04.2017.
 */
class TripCalculator {
    private final static Logger log = Logger.getLogger(TripCalculator.class);

    static Map<Integer, Double> createTripDurationMap(List<Trip> trips, int binWidthDuration_min) {
        Map<Integer, Double> tripDurationMap = new TreeMap<>();
        for (Trip trip : trips) {
            if ((trip.getArrivalTime_s() >= 0) && (trip.getDepartureTime_s() >= 0) && (trip.getDuration_s() >= 0)) {
                double tripDuration_min = (trip.getDuration_s() / 60.);
                TripUtils.addToMapIntegerKeyCeiling(tripDurationMap, tripDuration_min, binWidthDuration_min, trip.getWeight());
            }
        }
        return tripDurationMap;
    }

    static double calculateAverageTripDuration_min(List<Trip> trips) {
        double sumOfAllDurations_min = 0.;
        double sumOfAllWeights = 0.;
        for (Trip trip : trips) {
            if ((trip.getArrivalTime_s() >= 0) && (trip.getDepartureTime_s() >= 0) && (trip.getDuration_s() >= 0)) {
                sumOfAllDurations_min += (trip.getDuration_s() / 60. * trip.getWeight());
                sumOfAllWeights += trip.getWeight();
            }
        }
        return sumOfAllDurations_min / sumOfAllWeights;
    }

    static double countWeightOfTripsWithNonNegativeTimesAndDurations(List<Trip> trips) {
        double aggregatedWeight = 0;
        for (Trip trip : trips) {
            if ((trip.getArrivalTime_s() >= 0) && (trip.getDepartureTime_s() >= 0) && (trip.getDuration_s() >= 0)) {
                aggregatedWeight += trip.getWeight();
            }
        }
        return aggregatedWeight;
    }

    static Map<Integer, Double> createDepartureTimeMap(List<Trip> trips, int binWidthTime_h) {
        Map<Integer, Double> departureTimeMap = new TreeMap<>();
        for (Trip trip : trips) {
            if ((trip.getArrivalTime_s() >= 0) && (trip.getDepartureTime_s() >= 0) && (trip.getDuration_s() >= 0)) {
                double departureTime_h = (trip.getDepartureTime_s() / 3600.);
        		/* Note: Here, "floor" is used instead of "ceiling". A departure at 6:43 should go into the 6.a.m. bin. */
                TripUtils.addToMapIntegerKeyFloor(departureTimeMap, departureTime_h, binWidthTime_h, trip.getWeight());
            }
        }
        return departureTimeMap;
    }

    static Map<String, Double> createActivityTypeMap(List<Trip> trips) {
        Map<String, Double> activityTypeMap = new TreeMap<>();
        for (Trip trip : trips) {
            String activityType = trip.getActivityTypeAfterTrip();
            TripUtils.addToMapStringKey(activityTypeMap, activityType, trip.getWeight());
        }
        return activityTypeMap;
    }

    static Map<Integer, Double> createTripDistanceBeelineMap(List<Trip> trips, int binWidthDistance_km) {
        Map<Integer, Double> tripDistanceBeelineMap = new TreeMap<>();
        boolean distanceBelowZeroWarningPrinted = false;
        for (Trip trip : trips) {
            double tripDistanceBeeline_km = (trip.getDistanceBeeline_m() / 1000.);
            if (tripDistanceBeeline_km < 0) {
                if (!distanceBelowZeroWarningPrinted) {
                    log.warn("Beeline distance is below zero. This trip will not be considered.");
                    distanceBelowZeroWarningPrinted = true;
                }
            } else {
                TripUtils.addToMapIntegerKeyCeiling(tripDistanceBeelineMap, tripDistanceBeeline_km, binWidthDistance_km, trip.getWeight());
            }
        }
        return tripDistanceBeelineMap;
    }

    static double calculateAverageTripDistanceBeeline_km(List<Trip> trips) {
        double sumOfAllDistancesBeeline_km = 0.;
        double sumOfAllWeights = 0.;
        for (Trip trip : trips) {
            sumOfAllDistancesBeeline_km += (trip.getDistanceBeeline_m() / 1000. * trip.getWeight());
            sumOfAllWeights += trip.getWeight();
        }
        return sumOfAllDistancesBeeline_km / sumOfAllWeights;
    }

    static Map<Integer, Double> createTripDistanceRoutedMap(List<Trip> trips, int binWidthDistance_km) {
        Map<Integer, Double> tripDistanceRoutedMap = new TreeMap<>();
        boolean distanceBelowZeroWarningPrinted = false;
        for (Trip trip : trips) {
            double tripDistanceRouted_km = (trip.getDistanceRouted_m() / 1000.);
            if (tripDistanceRouted_km < 0) {
                if (!distanceBelowZeroWarningPrinted) {
                    log.warn("Routed distance is below zero. This trip will not be considered.");
                    distanceBelowZeroWarningPrinted = true;
                }
            } else {
                TripUtils.addToMapIntegerKeyCeiling(tripDistanceRoutedMap, tripDistanceRouted_km, binWidthDistance_km, trip.getWeight());
            }
        }
        return tripDistanceRoutedMap;
    }

    static double calculateAverageTripDistanceRouted_km(List<Trip> trips) {
        double sumOfAllDistancesRouted_km = 0.;
        double sumOfAllWeights = 0.;
        for (Trip trip : trips) {
            sumOfAllDistancesRouted_km += (trip.getDistanceRouted_m() / 1000. * trip.getWeight());
            sumOfAllWeights += trip.getWeight();
        }
        return sumOfAllDistancesRouted_km / sumOfAllWeights;
    }

    static double countWeightOfTripsWithCalculableSpeedBeeline(List<Trip> trips) {
        double aggregatedWeight = 0;
        for (Trip trip : trips) {
            if ((trip.getDuration_s() > 0.) && (trip.getDistanceBeeline_m()) >= 0.) {
                aggregatedWeight += trip.getWeight();
            }
        }
        return aggregatedWeight;
    }

    static Map<Integer, Double> createAverageTripSpeedBeelineMap(List<Trip> trips, int binWidthSpeed_km_h) {
        Map<Integer, Double> averageTripSpeedBeelineMap = new TreeMap<>();
        for (Trip trip : trips) {
            double tripDuration_h = (trip.getDuration_s() / 3600.);
            double tripDistanceBeeline_km = (trip.getDistanceBeeline_m() / 1000.);
            if ((tripDuration_h > 0.) && (tripDistanceBeeline_km >= 0.)) {
                TripUtils.addToMapIntegerKeyCeiling(averageTripSpeedBeelineMap, (tripDistanceBeeline_km / tripDuration_h), binWidthSpeed_km_h, trip.getWeight());
            }
        }
        return averageTripSpeedBeelineMap;
    }

    static double calculateAverageOfAverageTripSpeedsBeeline_km_h(List<Trip> trips) {
        double sumOfAllAverageSpeedsBeeline_km_h = 0.;
        double sumOfAllWeights = 0.;
        for (Trip trip : trips) {
            double tripDuration_h = trip.getDuration_s() / 3600.;
            double tripDistanceBeeline_km = (trip.getDistanceBeeline_m() / 1000.);
            if ((tripDuration_h > 0.) && (tripDistanceBeeline_km >= 0.)) {
                sumOfAllAverageSpeedsBeeline_km_h += (tripDistanceBeeline_km / tripDuration_h * trip.getWeight());
                sumOfAllWeights += trip.getWeight();
            }
        }
        return sumOfAllAverageSpeedsBeeline_km_h / sumOfAllWeights;
    }

    static double countWeightOfTripsWithCalculableSpeedRouted(List<Trip> trips) {
        double aggregatedWeight = 0;
        for (Trip trip : trips) {
            if ((trip.getDuration_s() > 0.) && (trip.getDistanceRouted_m()) >= 0.) {
                aggregatedWeight += trip.getWeight();
            }
        }
        return aggregatedWeight;
    }

    static Map<Integer, Double> createAverageTripSpeedRoutedMap(List<Trip> trips, int binWidthSpeed_km_h) {
        Map<Integer, Double> averageTripSpeedRoutedMap = new TreeMap<>();
        for (Trip trip : trips) {
            double tripDuration_h = (trip.getDuration_s() / 3600.);
            double tripDistanceRouted_km = (trip.getDistanceRouted_m() / 1000.);
            if ((tripDuration_h > 0.) && (tripDistanceRouted_km >= 0.)) {
                TripUtils.addToMapIntegerKeyCeiling(averageTripSpeedRoutedMap, (tripDistanceRouted_km / tripDuration_h), binWidthSpeed_km_h, trip.getWeight());
            }
        }
        return averageTripSpeedRoutedMap;
    }

    static double calculateAverageOfAverageTripSpeedsRouted_km_h(List<Trip> trips) {
        double sumOfAllAverageSpeedsRouted_km_h = 0.;
        double sumOfAllWeights = 0.;
        for (Trip trip : trips) {
            double tripDuration_h = (trip.getDuration_s() / 3600.);
            double tripDistanceRouted_km = (trip.getDistanceRouted_m() / 1000.);
            if ((tripDuration_h > 0.) && (tripDistanceRouted_km >= 0.)) {
                sumOfAllAverageSpeedsRouted_km_h += (tripDistanceRouted_km / tripDuration_h * trip.getWeight());
                sumOfAllWeights += trip.getWeight();
            }
        }
        return sumOfAllAverageSpeedsRouted_km_h / sumOfAllWeights;
    }

    static double countWeightOfTripsWithNonnegativeDistanceBeeline(List<Trip> trips) {
        double aggregatedWeight = 0.;
        for (Trip trip : trips) {
            if (trip.getDistanceBeeline_m() >= 0) {
                aggregatedWeight += trip.getWeight();
            }
        }
        return aggregatedWeight;
    }

    static double countWeightOfTripsWithNonnegativeDistanceRouted(List<Trip> trips) {
        double aggregatedWeight = 0.;
        for (Trip trip : trips) {
            if (trip.getDistanceRouted_m() >= 0) {
                aggregatedWeight += trip.getWeight();
            }
        }
        return aggregatedWeight;
    }
}
