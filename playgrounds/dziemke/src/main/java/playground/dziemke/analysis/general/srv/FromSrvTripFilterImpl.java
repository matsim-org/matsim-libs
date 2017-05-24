package playground.dziemke.analysis.general.srv;

import org.apache.log4j.Logger;
import playground.dziemke.analysis.general.Trip;
import playground.dziemke.analysis.general.TripFilter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author gthunig on 04.04.2017.
 */
public class FromSrvTripFilterImpl implements TripFilter {
    public static final Logger log = Logger.getLogger(FromSrvTripFilterImpl.class);

    // Parameters
    private boolean onlyAnalyzeTripsWithMode;
    private String mode;

    private boolean onlyAnalyzeTripInteriorOfArea; // formerly results labelled as "int"
    private boolean onlyAnalyzeTripsStartingOrEndingInArea; // formerly results labelled as "ber" (Berlin-based) <----------
    private String[] areaIds;

    private boolean onlyAnalyzeTripsInDistanceRange; // "dist"; usually varied for analysis // <----------
    private double minDistance_km = -1;
    private double maxDistance_km = -1;

    private boolean onlyAnalyzeTripsWithActivityTypeBeforeTrip;
    private String activityTypeBeforeTrip;
    private boolean onlyAnalyzeTripsWithActivityTypeAfterTrip;
    private String activityTypeAfterTrip;

    private boolean onlyAnalyzeTripsDoneByPeopleInAgeRange; // "age"; this requires setting a CEMDAP file
    private int minAge = -1; // typically "x0"
    private int maxAge = -1; // typically "x9"; highest number usually chosen is 119

    public void activateModeChoice(String mode) {
        onlyAnalyzeTripsWithMode = true;
        this.mode = mode;
    }

    public void activateInt(String... areIds) {
        this.onlyAnalyzeTripInteriorOfArea = true;
        this.areaIds = areIds;
    }

    public void activateSOE(String... areaIds) {
        onlyAnalyzeTripsStartingOrEndingInArea = true;
        this.areaIds = areaIds;
    }

    public void activateDist(double minDistance_km, double maxDistance_km) {
        onlyAnalyzeTripsInDistanceRange = true;
        this.minDistance_km = minDistance_km;
        this.maxDistance_km = maxDistance_km;
    }

    public void activateCertainActBefore(String activityTypeBeforeTrip) {
        onlyAnalyzeTripsWithActivityTypeBeforeTrip = true;
        this.activityTypeBeforeTrip = activityTypeBeforeTrip;
    }

    public void activateCertainActAfter(String activityTypeAfterTrip) {
        onlyAnalyzeTripsWithActivityTypeAfterTrip = true;
        this.activityTypeAfterTrip = activityTypeAfterTrip;
    }

    public void activateAge(int minAge, int maxAge) {
        onlyAnalyzeTripsDoneByPeopleInAgeRange = true;
        this.minAge = minAge;
        this.maxAge = maxAge;
    }

    public List<? extends Trip> filter(List<? extends Trip> inputTrips) {
        log.info("Unfiltered trips size: " + inputTrips.size());
        List<FromSrvTrip> filteredTrips = new LinkedList<>();
        boolean printedWarn1 = false;
        boolean printedWarn2 = false;

        for (Trip currentTrip : inputTrips) {
            FromSrvTrip trip = (FromSrvTrip)currentTrip;
            // Choose if trip will be considered
            if (onlyAnalyzeTripInteriorOfArea || onlyAnalyzeTripsStartingOrEndingInArea) {
                boolean startingInArea = Arrays.asList(areaIds).contains(trip.getDepartureZoneId().toString());
                boolean endingInArea = Arrays.asList(areaIds).contains(trip.getArrivalZoneId().toString());
                if (onlyAnalyzeTripsStartingOrEndingInArea) {
                    if (!startingInArea && !endingInArea)
                        continue;
                }
                if (onlyAnalyzeTripInteriorOfArea) {
                    if (onlyAnalyzeTripsStartingOrEndingInArea && !printedWarn1) {
                        log.warn("onlyAnalyzeTripInteriorOfArea and onlyAnalyzeTripsStartingOrEndingInArea activated at the same time!");
                        printedWarn1 = true;
                    }
                    if (!startingInArea || !endingInArea)
                        continue;
                }
            }

            if (onlyAnalyzeTripsWithMode) {
                if (!trip.getLegMode().equals(mode)) {
                    continue;
                }
            }

            if (onlyAnalyzeTripsInDistanceRange && (trip.getDistanceBeeline_m() / 1000.) > maxDistance_km) {
                continue;
            }
            if (onlyAnalyzeTripsInDistanceRange && (trip.getDistanceBeeline_m() / 1000.) < minDistance_km) {
                continue;
            }

            if (onlyAnalyzeTripsWithActivityTypeBeforeTrip && onlyAnalyzeTripsWithActivityTypeAfterTrip && !printedWarn2) {
                log.warn("onlyAnalyzeTripsWithActivityTypeBeforeTrip and onlyAnalyzeTripsWithActivityTypeAfterTrip activated at the same time."
                        + "This may lead to results that are hard to interpret: rather not use these options simultaneously.");
                printedWarn2 = true;
            }

            if (onlyAnalyzeTripsWithActivityTypeBeforeTrip) {
                if (!trip.getActivityTypeBeforeTrip().equals(activityTypeBeforeTrip)) {
                    continue;
                }
            }

            if (onlyAnalyzeTripsWithActivityTypeAfterTrip) {
                if (!trip.getActivityTypeAfterTrip().equals(activityTypeAfterTrip)) {
                    continue;
                }
            }

            // activity times and durations
            if ((trip.getArrivalTime_s() < 0) || (trip.getDepartureTime_s() < 0) || (trip.getDuration_s() < 0) ) {
                continue;
            }

			/* Only filteredTrips that fullfill all checked criteria are added; otherwise that loop would have been "continued" already */
            filteredTrips.add(trip);
        }

        log.info("Filtered trips size: " + filteredTrips.size());
        return filteredTrips;
    }

    public String adaptOutputDirectory(String outputDirectory) {
        if (onlyAnalyzeTripsWithMode) {
            outputDirectory = outputDirectory + "_" + mode;
        }
        if (onlyAnalyzeTripInteriorOfArea) {
            outputDirectory = outputDirectory + "_inside-" + areaIds[0];
        }
        if (onlyAnalyzeTripsStartingOrEndingInArea) {
            outputDirectory = outputDirectory + "_soe-in-" + areaIds[0];
        }
        if (onlyAnalyzeTripsInDistanceRange) {
            outputDirectory = outputDirectory + "_dist-" + minDistance_km + "-" + maxDistance_km;
        }
        if (onlyAnalyzeTripsWithActivityTypeBeforeTrip) {
            outputDirectory = outputDirectory + "_act-bef-" + activityTypeBeforeTrip;
        }
        if (onlyAnalyzeTripsWithActivityTypeAfterTrip) {
            outputDirectory = outputDirectory + "_act-aft-" + activityTypeAfterTrip;
        }
        if (onlyAnalyzeTripsDoneByPeopleInAgeRange) {
            outputDirectory = outputDirectory + "_age-" + minAge + "-" + maxAge;
        }
        return outputDirectory;
    }

}
