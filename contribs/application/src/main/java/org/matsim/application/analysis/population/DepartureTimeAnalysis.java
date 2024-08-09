package org.matsim.application.analysis.population;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@CommandLine.Command(
		name = "analyze-departure-time",
		description = "Analyze the departure time of the trips"
)
public class DepartureTimeAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(DepartureTimeAnalysis.class);

	@CommandLine.Option(names = "--plans", description = "Path to input population (plans) file", required = true)
	private Path inputPlans;

	@CommandLine.Option(names = "--output-folder", description = "Path to analysis output folder", required = true)
	private Path outputFolder;

	private static String[] ageGroups = new String[]{"0-17", "18-29", "30-49", "50-69", "70 or more"};


	public static void main(String[] args) {
		new DepartureTimeAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Map<String, Map<Integer, MutableInt>> recordMap = initializeRecordMap();
		Population population = PopulationUtils.readPopulation(inputPlans.toString());

		int processedPerson = 0;
		int tripsDepartAfter24h = 0;

		for (Person person : population.getPersons().values()) {
			//TODO potentially filter persons, e.g. by home location or by subpopulation

			processedPerson++;
			int age = PersonUtils.getAge(person);
			String ageGroup = determineAgeGroup(age);
			Plan plan = person.getSelectedPlan();
			List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
			for (TripStructureUtils.Trip trip : trips) {
				double departureTime = trip.getOriginActivity().getEndTime().seconds();
				// maybe the Math.floor operation is not needed?
				int timeBin = (int) Math.floor(departureTime / 3600);
				if (timeBin >= 24) {
					tripsDepartAfter24h++;
					continue;
				}
				// 1 time bin = 1 hour
				recordMap.get(ageGroup).get(timeBin).increment();
			}
		}

		// write results
		if (!Files.exists(outputFolder)) {
			Files.createDirectory(outputFolder);
		}
		CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(outputFolder + "/departure_time_analysis.csv"), CSVFormat.TDF);
		List<String> titleRow = new ArrayList<>();
		titleRow.add("age_group");
		for (int i = 0; i < 24; i++) {
			titleRow.add(Integer.toString(i));
		}
		tsvWriter.printRecord(titleRow);
		for (String ageGroup : ageGroups) {
			List<String> outputRow = new ArrayList<>();
			outputRow.add(ageGroup);
			for (int i = 0; i < 24; i++) {
				outputRow.add(Integer.toString(recordMap.get(ageGroup).get(i).intValue()));
			}
			tsvWriter.printRecord(outputRow);
		}
		tsvWriter.close();

		log.info("Person processed (living within the area): {}", processedPerson);
		log.info("Number of trips that depart after 24 hours is {}", tripsDepartAfter24h);

		return 0;
	}

	private Map<String, Map<Integer, MutableInt>> initializeRecordMap() {
		Map<String, Map<Integer, MutableInt>> recordMap = new HashMap<>();
		for (String ageGroup : ageGroups) {
			recordMap.put(ageGroup, new HashMap<>());
			for (int i = 0; i < 24; i++) {
				recordMap.get(ageGroup).put(i, new MutableInt(0));
			}
		}
		return recordMap;
	}

	private String determineAgeGroup(int age) {
		if (age <= 17) {
			return ageGroups[0];
		}

		if (age <= 29) {
			return ageGroups[1];
		}

		if (age <= 49) {
			return ageGroups[2];
		}

		if (age <= 69) {
			return ageGroups[3];
		}

		return ageGroups[4];
	}
}
