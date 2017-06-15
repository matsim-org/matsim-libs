package playground.dziemke.analysis.general;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import playground.dziemke.analysis.GnuplotUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author on 04.04.2017.
 */
public class GeneralTripAnalyzer {
    public static final Logger log = Logger.getLogger(GeneralTripAnalyzer.class);

    private static final int binWidthDuration_min = 1;
    private static final int binWidthTime_h = 1;
    private static final int binWidthDistance_km = 1;
    private static final int binWidthSpeed_km_h = 1;

    private static Map<Id<Trip>, Double> distanceRoutedMap = new TreeMap<>();
    private static Map<Id<Trip>, Double> distanceBeelineMap = new TreeMap<>();

    private static Map<String, Double> otherInformationMap = new TreeMap<>();

    public static void analyze(List<Trip> trips, String outputDir) {
        analyze(trips, 0, outputDir);
    }

    public static void analyze(List<Trip> trips, int noPreviousEndOfActivityCounter, String outputDir) {
        /* Do calculations and write-out*/

        double averageTripDuration;
        int numberOfInIncompleteTrips = 0;
        double aggregatedWeightOfTripsWithNonNegativeDistanceBeeline;
        double aggregatedWeightOfTripsWithNonNegativeDistanceRouted;
        double aggregatedWeightOfTripsWithCalculableSpeedBeeline;
        double aggregatedWeightOfTripsWithCalculableSpeedRouted;

        Map<Integer, Double> tripDurationMap = TripCalculator.createTripDurationMap(trips, binWidthDuration_min);
        averageTripDuration = TripCalculator.calculateAverageTripDuration_min(trips);
        double aggregatedWeightOfConsideredTrips = getAgregatedWeight(trips);
        AnalysisFileWriter.writeToFileIntegerKey(tripDurationMap, outputDir + "/tripDuration.txt", binWidthDuration_min, aggregatedWeightOfConsideredTrips, averageTripDuration);
        AnalysisFileWriter.writeToFileIntegerKeyCumulative(tripDurationMap, outputDir + "/tripDurationCumulative.txt", binWidthDuration_min, aggregatedWeightOfConsideredTrips, averageTripDuration);

        Map <Integer, Double> departureTimeMap = TripCalculator.createDepartureTimeMap(trips, binWidthTime_h);
        AnalysisFileWriter.writeToFileIntegerKey(departureTimeMap, outputDir + "/departureTime.txt", binWidthTime_h, aggregatedWeightOfConsideredTrips, Double.NaN);

        Map<String, Double> activityTypeMap = TripCalculator.createActivityTypeMap(trips);
        AnalysisFileWriter.writeToFileStringKey(activityTypeMap, outputDir + "/activityTypes.txt", aggregatedWeightOfConsideredTrips);

        aggregatedWeightOfTripsWithNonNegativeDistanceBeeline = TripCalculator.countWeightOfTripsWithNonnegativeDistanceBeeline(trips);
        Map<Integer, Double> tripDistanceBeelineMap = TripCalculator.createTripDistanceBeelineMap(trips, binWidthDistance_km);
        double averageTripDistanceBeeline_km = TripCalculator.calculateAverageTripDistanceBeeline_km(trips);
        AnalysisFileWriter.writeToFileIntegerKey(tripDistanceBeelineMap, outputDir + "/tripDistanceBeeline.txt", binWidthDistance_km, aggregatedWeightOfTripsWithNonNegativeDistanceBeeline, averageTripDistanceBeeline_km);
        AnalysisFileWriter.writeToFileIntegerKeyCumulative(tripDistanceBeelineMap, outputDir + "/tripDistanceBeelineCumulative.txt", binWidthDistance_km, aggregatedWeightOfTripsWithNonNegativeDistanceBeeline, averageTripDistanceBeeline_km);

        aggregatedWeightOfTripsWithNonNegativeDistanceRouted = TripCalculator.countWeightOfTripsWithNonnegativeDistanceRouted(trips);
        Map<Integer, Double> tripDistanceRoutedMap = TripCalculator.createTripDistanceRoutedMap(trips, binWidthDistance_km);
        double averageTripDistanceRouted_km = TripCalculator.calculateAverageTripDistanceRouted_km(trips);
        AnalysisFileWriter.writeToFileIntegerKey(tripDistanceRoutedMap, outputDir + "/tripDistanceRouted.txt", binWidthDistance_km, aggregatedWeightOfTripsWithNonNegativeDistanceRouted, averageTripDistanceRouted_km);

        aggregatedWeightOfTripsWithCalculableSpeedBeeline = TripCalculator.countWeightOfTripsWithCalculableSpeedBeeline(trips);
        Map<Integer, Double> averageTripSpeedBeelineMap = TripCalculator.createAverageTripSpeedBeelineMap(trips, binWidthSpeed_km_h);
        double averageOfAverageTripSpeedsBeeline_km_h = TripCalculator.calculateAverageOfAverageTripSpeedsBeeline_km_h(trips);
        AnalysisFileWriter.writeToFileIntegerKey(averageTripSpeedBeelineMap, outputDir + "/averageTripSpeedBeeline.txt", binWidthSpeed_km_h, aggregatedWeightOfTripsWithCalculableSpeedBeeline, averageOfAverageTripSpeedsBeeline_km_h);
        AnalysisFileWriter.writeToFileIntegerKeyCumulative(averageTripSpeedBeelineMap, outputDir + "/averageTripSpeedBeelineCumulative.txt", binWidthSpeed_km_h, aggregatedWeightOfTripsWithCalculableSpeedBeeline, averageOfAverageTripSpeedsBeeline_km_h);

        aggregatedWeightOfTripsWithCalculableSpeedRouted = TripCalculator.countWeightOfTripsWithCalculableSpeedRouted(trips);
        Map<Integer, Double> averageTripSpeedRoutedMap = TripCalculator.createAverageTripSpeedRoutedMap(trips, binWidthSpeed_km_h);
        double averageOfAverageTripSpeedsRouted_km_h = TripCalculator.calculateAverageOfAverageTripSpeedsRouted_km_h(trips);
        AnalysisFileWriter.writeToFileIntegerKey(averageTripSpeedRoutedMap, outputDir + "/averageTripSpeedRouted.txt", binWidthSpeed_km_h, aggregatedWeightOfTripsWithCalculableSpeedRouted, averageOfAverageTripSpeedsRouted_km_h);


        /* Other information */
        otherInformationMap.put("Aggregated weight of trips that have no previous activity", (double) noPreviousEndOfActivityCounter);
        otherInformationMap.put("Aggregated weight of trips that have negative distance (beeline, from survey)", aggregatedWeightOfConsideredTrips - aggregatedWeightOfTripsWithNonNegativeDistanceBeeline);
        otherInformationMap.put("Aggregated weight of trips that have negative distance (routed, from survey)", aggregatedWeightOfConsideredTrips - aggregatedWeightOfTripsWithNonNegativeDistanceRouted);
        otherInformationMap.put("Aggregated weight of trips that have no calculable speed beeline", aggregatedWeightOfConsideredTrips - aggregatedWeightOfTripsWithCalculableSpeedBeeline);
        otherInformationMap.put("Aggregated weight of trips that have no calculable speed routed", aggregatedWeightOfConsideredTrips - aggregatedWeightOfTripsWithCalculableSpeedRouted);
        otherInformationMap.put("Aggregated weight of (complete) trips", aggregatedWeightOfConsideredTrips);
        AnalysisFileWriter.writeToFileOther(otherInformationMap, outputDir + "/otherInformation.txt");

        // write a routed distance vs. beeline distance comparison file
        doBeelineCaluclations(trips, binWidthDistance_km);
        AnalysisFileWriter.writeRoutedBeelineDistanceComparisonFile(distanceRoutedMap, distanceBeelineMap, outputDir + "/beeline.txt", aggregatedWeightOfConsideredTrips);

        log.info(numberOfInIncompleteTrips + " trips are incomplete.");
    }

    private static double getAgregatedWeight(List<Trip> trips) {
        double agregatedWeight = 0;
        for (Trip trip : trips)
            agregatedWeight += trip.getWeight();
        return agregatedWeight;
    }

    private static void doBeelineCaluclations(List<Trip> trips, int binWidthDistance_km) {
        Map<Integer, Double> tripDistanceBeelineMap = new TreeMap<>();
        boolean distanceBelowZeroWarningPrinted = false;
        for (Trip trip : trips) {
            double tripDistanceRouted_km = trip.getDistanceRouted_m() / 1000.;
            double tripDistanceBeeline_km = trip.getDistanceBeeline_m() / 1000.;
            if (tripDistanceBeeline_km < 0 || tripDistanceRouted_km < 0) {
                if (!distanceBelowZeroWarningPrinted) {
                    log.warn("Distance is below zero. This trip will not be considered.");
                    distanceBelowZeroWarningPrinted = true;
                }
            }
            else {
                double weight = trip.getWeight();
                TripUtils.addToMapIntegerKeyCeiling(tripDistanceBeelineMap, tripDistanceBeeline_km, binWidthDistance_km, weight);

                distanceBeelineMap.put(trip.getTripId(), tripDistanceBeeline_km); // TODO eventually remove this
                distanceRoutedMap.put(trip.getTripId(), tripDistanceRouted_km); // TODO eventually remove this
            }
        }
    }

    /* Create gnuplot graphics */
    public static void runGnuplot(String outputDir, String relativePathToGnuplotScript, String ralativePathToCampareFile) {
        GnuplotUtils.runGnuplotScript(outputDir, relativePathToGnuplotScript, ralativePathToCampareFile);
    }

    public static boolean doesExist(String analyzeDirectory) {
        File dir = new File(analyzeDirectory);
        return dir.exists() && dir.isDirectory() && hasAnalyzeContent(dir);
    }

    private static boolean hasAnalyzeContent(File analyzeDirectory) {
        List<String> childs = Arrays.asList(analyzeDirectory.list());
        return childs.contains("activityTypes.txt") &&
               childs.contains("averageTripSpeedBeeline.txt") &&
               childs.contains("averageTripSpeedBeelineCumulative.txt") &&
               childs.contains("averageTripSpeedRouted.txt") &&
               childs.contains("beeline.txt") &&
               childs.contains("departureTime.txt") &&
               childs.contains("otherInformation.txt") &&
               childs.contains("tripDistanceBeeline.txt") &&
               childs.contains("tripDistanceBeelineCumulative.txt") &&
               childs.contains("tripDistanceRouted.txt") &&
               childs.contains("tripDuration.txt") &&
               childs.contains("tripDurationCumulative.txt");
    }
}
