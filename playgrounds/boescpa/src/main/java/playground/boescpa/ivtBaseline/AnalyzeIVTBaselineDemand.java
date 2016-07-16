package playground.boescpa.ivtBaseline;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import playground.boescpa.analysis.ActivityAnalyzer;
import playground.boescpa.analysis.scenarioAnalyzer.ScenarioAnalyzer;
import playground.boescpa.analysis.scenarioAnalyzer.eventHandlers.AgentCounter;
import playground.boescpa.analysis.scenarioAnalyzer.eventHandlers.ScenarioAnalyzerEventHandler;
import playground.boescpa.analysis.scenarioAnalyzer.eventHandlers.TripActivityCrosscorrelator;
import playground.boescpa.analysis.scenarioAnalyzer.eventHandlers.TripAnalyzer;
import playground.boescpa.analysis.spatialCutters.NoCutter;
import playground.boescpa.analysis.trips.*;
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

	private final String pathToEventsFile;
	private final Network network;
	private final Population population;
	private final int scaleFactor;
	private final String pathToOutputFolder;

	private AnalyzeIVTBaselineDemand(String pathToEventsFile, Network network, Population population, int scaleFactor, String pathToOutputFolder) {
		this.pathToEventsFile = pathToEventsFile;
		this.network = network;
		this.population = population;
		this.scaleFactor = scaleFactor;
		this.pathToOutputFolder = pathToOutputFolder;
	}

    public static void main(final String[] args) {
        final String pathToEventsFile = args[0];
        final Network network = NetworkUtils.readNetwork(args[1]);
        final Population population = PopulationUtils.readPopulation(args[2]);
		new ObjectAttributesXmlReader(population.getPersonAttributes()).parse(args[3]);
        final int scaleFactor = Integer.parseInt(args[4]);
        final String pathToOutputFolder = args.length > 5 ? args[5] : "";

		AnalyzeIVTBaselineDemand analyzeIVTBaselineDemand = new AnalyzeIVTBaselineDemand(
				pathToEventsFile, network, population, scaleFactor, pathToOutputFolder);

        // analyze events
        analyzeIVTBaselineDemand.analyzeEvents();

        // analyze trips
        analyzeIVTBaselineDemand.analyzeTrips();

        // analyze population
		analyzeIVTBaselineDemand.analyzePopulation();
    }

	private void analyzePopulation() {
		ActivityAnalyzer activityAnalyzer = new ActivityAnalyzer();
		activityAnalyzer.analyzePopulation(population);
		activityAnalyzer.printActChainAnalysis(pathToOutputFolder + "actChainAnalysis.csv");
	}

	private void analyzeTrips() {
		List<Trip> origTrips = getNoStuckTrips();

        List<Trip> homeTrips = TripFilter.purposeTripFilter(origTrips, "home");
        TripWriter.writeTrips(homeTrips, pathToOutputFolder + "trips_home.csv");

        List<Trip> workTrips = TripFilter.purposeTripFilter(origTrips, "work");
        List<Trip> childWorkTrips = filterTripsForAge(population, workTrips, 0, 15);
        TripWriter.writeTrips(childWorkTrips, pathToOutputFolder + "trips_childWork.csv");
        List<Trip> teenWorkTrips = filterTripsForAge(population, workTrips, 16, 20);
        TripWriter.writeTrips(teenWorkTrips, pathToOutputFolder + "trips_teenWork.csv");
        List<Trip> adultWorkTrips = filterTripsForAge(population, workTrips, 21, 65);
        TripWriter.writeTrips(adultWorkTrips, pathToOutputFolder + "trips_adultWork.csv");
        List<Trip> retireeWorkTrips = filterTripsForAge(population, workTrips, 66, 150);
        TripWriter.writeTrips(retireeWorkTrips, pathToOutputFolder + "trips_retireeWork.csv");

        List<Trip> educationTrips = TripFilter.purposeTripFilter(origTrips, "education");
        List<Trip> kindergartenTrips = filterTripsForAge(population, educationTrips, 0, 6);
        TripWriter.writeTrips(kindergartenTrips, pathToOutputFolder + "trips_kindergarten.csv");
        List<Trip> schoolTrips = filterTripsForAge(population, educationTrips, 7, 16);
        TripWriter.writeTrips(schoolTrips, pathToOutputFolder + "trips_school.csv");
        List<Trip> highschoolTrips = filterTripsForAge(population, educationTrips, 17, 20);
        TripWriter.writeTrips(highschoolTrips, pathToOutputFolder + "trips_highschool.csv");
        List<Trip> higherEducationTrips = filterTripsForAge(population, educationTrips, 21, 150);
        TripWriter.writeTrips(higherEducationTrips, pathToOutputFolder + "trips_higherEducation.csv");
    }

	private List<Trip> getNoStuckTrips() {
		TripEventHandler.setAnonymizeTrips(false);
		EventsToTrips eventsToTrips = new EventsToTrips(new TripEventHandler(network) {
			@Override
			protected boolean agentIsToConsider(Id<Person> personId) {
				return super.agentIsToConsider(personId) && isToConsider(personId);
			}
		});
		List<Trip> origTrips = eventsToTrips.createTripsFromEvents(pathToEventsFile);
		TripWriter.writeTrips(origTrips, pathToOutputFolder + "all_trips.csv");
		return TripFilter.removeUnfinishedTrips(origTrips, null);
	}

    private List<Trip> filterTripsForAge(Population population, List<Trip> origTrips, int minAge, int maxAge) {
        List<Trip> filteredTrips = new LinkedList<>();
        for (Trip tempTrip : origTrips) {
            if ((int)population.getPersons().get(tempTrip.agentId).getCustomAttributes().get("age") >= minAge
                    && (int)population.getPersons().get(tempTrip.agentId).getCustomAttributes().get("age") <= maxAge) {
                filteredTrips.add(tempTrip.clone());
            }
        }
        return Collections.unmodifiableList(filteredTrips);
    }

    private void analyzeEvents() {
        try {
            // Analyze the events:
            ScenarioAnalyzerEventHandler[] handlers = {
                    new AgentCounter(network) {
						@Override
						protected boolean isPersonToConsider(Id<Person> personId) {
							return super.isPersonToConsider(personId) && isToConsider(personId);
						}
					},
                    new TripAnalyzer(network){
						@Override
						protected boolean isPersonToConsider(Id<Person> personId) {
							return super.isPersonToConsider(personId) && isToConsider(personId);
						}
					},
                    new TripActivityCrosscorrelator(network){
						@Override
						protected boolean isPersonToConsider(Id<Person> personId) {
							return super.isPersonToConsider(personId) && isToConsider(personId);
						}
					},
            };
            ScenarioAnalyzer scenarioAnalyzer = new ScenarioAnalyzer(pathToEventsFile, scaleFactor, handlers);
            scenarioAnalyzer.analyzeScenario();

            // Return the results:
            scenarioAnalyzer.createResults(pathToEventsFile + "_analysisResults.csv", new NoCutter());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

	private boolean isToConsider(Id<Person> personId) {
		return population.getPersonAttributes().getAttribute(personId.toString(), "subpopulation") == null;
	}

}
