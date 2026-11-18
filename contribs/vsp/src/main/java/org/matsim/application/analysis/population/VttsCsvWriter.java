package org.matsim.application.analysis.population;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.matsim.application.analysis.population.VttsAttributes.*;

/** Exports an annotated population; performs no scoring or income normalization and uses no Tablesaw tables. */
public final class VttsCsvWriter {

	public void write(Path directory, Population population, Map<Id<Person>, Plan> plans) throws IOException {
		Files.createDirectories(directory);
		// Keep the two weightings separate. Person statistics contain one mean per person;
		// destination statistics contain one observation per valid trip destination.
		Map<String, DoubleArrayList> persons = metrics();
		Map<String, DoubleArrayList> destinations = metrics();
		Map<String, Integer> coverage = new TreeMap<>();
		try (CSVPrinter personCsv = csv(directory.resolve("vtts_persons.csv.gz"));
			 CSVPrinter tripCsv = csv(directory.resolve("vtts_trips.csv.gz"))) {
			personCsv.printRecord("person_id", "subpopulation", "income", "valid_destinations", "valid_vtts_destinations",
				"mean_muse_score_per_hour", "marginal_utility_of_money", "mean_vse_money_per_hour", "mean_vtts_money_per_hour", "status");
			tripCsv.printRecord("person_id", "trip_index", "destination_activity", "saving_mode", "muse_score_per_hour",
				"travel_utility_score_per_hour", "marginal_utility_of_money", "vse_money_per_hour", "vtts_money_per_hour", "status");
			// Write a row for every original person, including excluded or missing-plan cases.
			// Coverage therefore describes the entire population, even where values are unavailable.
			for (Person person : population.getPersons().values()) {
				personCsv.printRecord(person.getId(), PopulationUtils.getSubpopulation(person), PersonUtils.getIncome(person),
					attribute(person, DESTINATIONS), attribute(person, VTTS_DESTINATIONS), number(person, MUSE),
					number(person, MONEY_UTILITY), number(person, VSE), number(person, VTTS), attribute(person, STATUS));
				coverage.merge(String.valueOf(attribute(person, STATUS)), 1, Integer::sum);
				addMetrics(persons, person);
				Plan plan = plans.get(person.getId());
				if (plan == null) continue;
				int index = 0;
				// Incomplete/stuck plans remain in the export. Extract open chains safely, but
				// write destination rows only for trips with both an origin and an actual arrival.
				for (var trip : TripStructureUtils.getTrips2(plan)) {
					if (trip.getOriginActivity() == null || trip.getDestinationActivity() == null) continue;
					var activity = trip.getDestinationActivity();
					Object status = attribute(activity, STATUS);
					tripCsv.printRecord(person.getId(), index++, activity.getType(), attribute(activity, MODE), number(activity, MUSE),
						number(activity, TRAVEL_UTILITY), number(activity, MONEY_UTILITY), number(activity, VSE), number(activity, VTTS),
						status == null ? attribute(person, STATUS) : status);
					// Only analyzed destinations have their own status. For incomplete plans the CSV
					// reports the person's status, but their uncomputed activities add no observations.
					if (status != null) addMetrics(destinations, activity);
				}
			}
		}
		// Tile input has no header. Quote every field so delimiter detection cannot split labels
		// at spaces, and keep an empty third column where no icon is supplied. Counts refer to
		// the simulated sample; this writer never expands them by a scenario sample-size factor.
		try (CSVPrinter overview = new CSVPrinter(IOUtils.getBufferedWriter(directory.resolve("vtts_overview.csv").toString()),
			CSVFormat.DEFAULT.builder().setQuoteMode(QuoteMode.ALL).build())) {
			overview.printRecord("Agents in population", population.getPersons().size(), "user-group");
			overview.printRecord("Persons with MUSE", persons.get(MUSE).size(), "user-group");
			overview.printRecord("Persons with VTTS", persons.get(VTTS).size(), "user-group");
			overview.printRecord("Trips with MUSE", destinations.get(MUSE).size(), "");
			overview.printRecord("Trips with VTTS", destinations.get(VTTS).size(), "");
		}
		try (CSVPrinter csv = csv(directory.resolve("vtts_coverage.csv"))) {
			csv.printRecord("status", "persons", "meaning");
			for (var entry : coverage.entrySet()) csv.printRecord(entry.getKey(), entry.getValue(), coverageMeaning(entry.getKey()));
		}
		try (CSVPrinter summary = csv(directory.resolve("vtts_summary.csv"))) {
			summary.printRecord("metric", "unit", "weighting", "observations", "mean", "p25", "median", "p75", "p95");
			writeStatistics(summary, persons, "person");
			writeStatistics(summary, destinations, "trip");
		}
		writePopulation(directory.resolve("vtts_experienced_plans.xml.gz"), population, plans);
	}

	private static String coverageMeaning(String status) {
		return switch (status) {
			case "ok" -> "VTTS calculated for all trips.";
			case "excluded" -> "Outside the included population stated above; excluded by subpopulation or the scenario-specific person filter.";
			case "not_mobile" -> "No trips: the person stayed at one activity all day. VTTS does not apply.";
			case "activity_mismatch" -> "Selected and experienced plans have different main-activity counts or types; scoring attributes cannot be matched safely.";
			case "stuck" -> "A stuck event was recorded; the person is excluded from VTTS statistics.";
			case "missing_plan" -> "No experienced plan is available.";
			case "incomplete_plan" -> "The experienced plan has incomplete trip or activity boundaries/times.";
			case "invalid_money_utility" -> "MUSE is available but marginal utility of money is invalid; monetary conversion is unavailable.";
			case "partial_vtts" -> "VTTS is available for only some trips; the person mean uses those valid trips.";
			case "non_finite_muse" -> "No finite trip MUSE values are available.";
			default -> status;
		};
	}

	/** Stream copies one person at a time, preserving the original experienced plans and their owners. */
	private static void writePopulation(Path path, Population population, Map<Id<Person>, Plan> plans) {
		StreamingPopulationWriter writer = new StreamingPopulationWriter();
		writer.startStreaming(path.toString());
		try {
			for (Person original : population.getPersons().values()) {
				Plan experienced = plans.get(original.getId());
				if (experienced == null) continue;
				// Adding an existing experienced plan to a new person would change its owner.
				// Copy both the person attributes and plan, then discard the copy after streaming it.
				Person copy = population.getFactory().createPerson(original.getId());
				AttributesUtils.copyAttributesFromTo(original, copy);
				Plan plan = population.getFactory().createPlan();
				PopulationUtils.copyFromTo(experienced, plan, true);
				copy.addPlan(plan);
				writer.run(copy);
			}
		} finally {
			writer.closeStreaming();
		}
	}

	private static CSVPrinter csv(Path path) throws IOException {
		// MATSim's writer enables compression from the .gz suffix on the detailed exports.
		return new CSVPrinter(IOUtils.getBufferedWriter(path.toString()), CSVFormat.DEFAULT);
	}

	private static Map<String, DoubleArrayList> metrics() {
		Map<String, DoubleArrayList> result = new LinkedHashMap<>();
		for (String key : new String[]{MUSE, VSE, VTTS}) result.put(key, new DoubleArrayList());
		return result;
	}

	private static void addMetrics(Map<String, DoubleArrayList> metrics, Attributable object) {
		// Check each metric independently: a missing VTTS must not remove a valid MUSE or VSE.
		metrics.forEach((key, values) -> {
			Double value = getDouble(object, key);
			if (value != null && Double.isFinite(value)) values.add(value.doubleValue());
		});
	}

	private static Object number(Attributable object, String key) {
		// Empty CSV cells represent unavailable results; keep finite negative and zero values.
		Double value = getDouble(object, key);
		return value != null && Double.isFinite(value) ? value : "";
	}

	private static Object attribute(Attributable object, String key) {
		return object.getAttributes().getAttribute(key);
	}

	private static void writeStatistics(CSVPrinter csv, Map<String, DoubleArrayList> metrics, String weighting) throws IOException {
		for (var entry : metrics.entrySet()) {
			double[] values = entry.getValue().toDoubleArray();
			Arrays.sort(values);
			String name = entry.getKey().equals(MUSE) ? "MUSE" : entry.getKey().equals(VSE) ? "VSE" : "VTTS";
			String unit = entry.getKey().equals(MUSE) ? "score/hour" : "money/hour";
			csv.printRecord(name, unit, weighting, values.length, values.length == 0 ? "" : Arrays.stream(values).average().orElseThrow(),
				quantile(values, 0.25), quantile(values, 0.5), quantile(values, 0.75), quantile(values, 0.95));
		}
	}

	/** Linear interpolation between sorted observations, with missing results written as empty cells. */
	private static Object quantile(double[] values, double fraction) {
		if (values.length == 0) return "";
		double position = fraction * (values.length - 1);
		int lower = (int) position;
		int upper = Math.min(lower + 1, values.length - 1);
		return values[lower] + (position - lower) * (values[upper] - values[lower]);
	}
}
