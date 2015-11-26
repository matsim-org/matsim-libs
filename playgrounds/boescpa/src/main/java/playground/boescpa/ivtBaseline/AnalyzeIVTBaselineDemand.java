package playground.boescpa.ivtBaseline;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import playground.boescpa.analysis.ActivityAnalyzer;
import playground.boescpa.analysis.scenarioAnalyzer.ScenarioAnalyzer;
import playground.boescpa.analysis.scenarioAnalyzer.eventHandlers.*;
import playground.boescpa.analysis.spatialCutters.NoCutter;
import playground.boescpa.analysis.trips.*;
import playground.boescpa.analysis.trips.tripAnalysis.TravelTimesAndDistances;
import playground.boescpa.lib.tools.NetworkUtils;
import playground.boescpa.lib.tools.PopulationUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides the basic stats of the demand.
 *
 * @author boescpa
 */
public class AnalyzeIVTBaselineDemand {

    public static void main(final String[] args) {
        final String pathToEventsFile = args[0];
        final Network network = NetworkUtils.readNetwork(args[1]);
        final Population population = PopulationUtils.readPopulation(args[2]);
        final int scaleFactor = Integer.parseInt(args[3]);
        final String pathToOutputFolder = args.length > 4 ? args[4] : "";

        // analyze events
        runScenarioAnalyzer(network, pathToEventsFile, scaleFactor);

        // analyze trips
        TripEventHandler.setAnonymizeTrips(false);
        List<Trip> origTrips = EventsToTrips.createTripsFromEvents(pathToEventsFile, network);
        TripWriter.writeTrips(origTrips, "all_trips.csv");
        List<Trip> noStuckTrips = TripFilter.removeUnfinishedTrips(origTrips, null);
        analyzeTrips(noStuckTrips, population, pathToOutputFolder);

        // analyze population
        ActivityAnalyzer activityAnalyzer = new ActivityAnalyzer();
        activityAnalyzer.analyzePopulation(population);
        activityAnalyzer.printActChainAnalysis(pathToOutputFolder + "actChainAnalysis.csv");
    }

    private static void analyzeTrips(List<Trip> origTrips, Population population, String pathToOutputFiles) {
        List<Trip> homeTrips = TripFilter.purposeTripFilter(origTrips, "home");
        TripWriter.writeTrips(homeTrips, pathToOutputFiles + "trips_home.csv");

        List<Trip> workTrips = TripFilter.purposeTripFilter(origTrips, "work");
        List<Trip> childWorkTrips = filterTripsForAge(population, workTrips, 0, 16);
        TripWriter.writeTrips(childWorkTrips, pathToOutputFiles + "trips_childWork.csv");
        List<Trip> teenWorkTrips = filterTripsForAge(population, workTrips, 17, 20);
        TripWriter.writeTrips(teenWorkTrips, pathToOutputFiles + "trips_teenWork.csv");
        List<Trip> adultWorkTrips = filterTripsForAge(population, workTrips, 21, 65);
        TripWriter.writeTrips(adultWorkTrips, pathToOutputFiles + "trips_adultWork.csv");
        List<Trip> retireeWorkTrips = filterTripsForAge(population, workTrips, 66, 150);
        TripWriter.writeTrips(retireeWorkTrips, pathToOutputFiles + "trips_retireeWork.csv");

        List<Trip> educationTrips = TripFilter.purposeTripFilter(origTrips, "education");
        List<Trip> kindergartenTrips = filterTripsForAge(population, educationTrips, 0, 6);
        TripWriter.writeTrips(kindergartenTrips, pathToOutputFiles + "trips_kindergarten.csv");
        List<Trip> schoolTrips = filterTripsForAge(population, educationTrips, 7, 16);
        TripWriter.writeTrips(schoolTrips, pathToOutputFiles + "trips_school.csv");
        List<Trip> highschoolTrips = filterTripsForAge(population, educationTrips, 17, 20);
        TripWriter.writeTrips(highschoolTrips, pathToOutputFiles + "trips_highschool.csv");
        List<Trip> higherEducationTrips = filterTripsForAge(population, educationTrips, 21, 150);
        TripWriter.writeTrips(higherEducationTrips, pathToOutputFiles + "trips_higherEducation.csv");
    }

    private static List<Trip> filterTripsForAge(Population population, List<Trip> origTrips, int minAge, int maxAge) {
        List<Trip> filteredTrips = new LinkedList<>();
        for (Trip tempTrip : origTrips) {
            if ((int)population.getPersons().get(tempTrip.agentId).getCustomAttributes().get("age") >= minAge
                    && (int)population.getPersons().get(tempTrip.agentId).getCustomAttributes().get("age") <= maxAge) {
                filteredTrips.add(tempTrip.clone());
            }
        }
        return Collections.unmodifiableList(filteredTrips);
    }

    private static void runScenarioAnalyzer (Network network, String pathToEventsFile, int scaleFactor) {
        try {
            // Analyze the events:
            ScenarioAnalyzerEventHandler[] handlers = {
                    new AgentCounter(network),
                    new TripAnalyzer(network),
                    new TripActivityCrosscorrelator(network),
            };
            ScenarioAnalyzer scenarioAnalyzer = new ScenarioAnalyzer(pathToEventsFile, scaleFactor, handlers);
            scenarioAnalyzer.analyzeScenario();

            // Return the results:
            scenarioAnalyzer.createResults(pathToEventsFile + "_analysisResults.csv", new NoCutter());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}
