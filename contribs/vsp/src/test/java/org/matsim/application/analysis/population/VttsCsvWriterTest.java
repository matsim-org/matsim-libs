package org.matsim.application.analysis.population;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.matsim.application.analysis.population.VttsAttributes.*;
import static org.matsim.application.analysis.population.kn.KNTestFixture.*;

class VttsCsvWriterTest {

	@TempDir Path directory;

	@Test
	void exportsDifferentWeightingsAndRoundTripsAttributesWithoutChangingPlanOwners() throws IOException {
		var scenario = scenario();
		var two = person(scenario, "two", 2000, false);
		var three = person(scenario, "three", 2000, true);
		var analysis = new VttsAnalysis(muse(scenario), VttsAnalysisTest.parameters(2, -6), new DefaultAnalysisMainModeIdentifier());
		Map<Id<Person>, Plan> plans = new LinkedHashMap<>();
		for (var person : scenario.getPopulation().getPersons().values()) {
			plans.put(person.getId(), person.getSelectedPlan());
			analysis.analyze(person, person.getSelectedPlan());
		}

		new VttsCsvWriter().write(directory, scenario.getPopulation(), plans);

		assertEquals(2, read(directory.resolve("vtts_persons.csv.gz")).size());
		assertEquals(5, read(directory.resolve("vtts_trips.csv.gz")).size());
		var summary = read(directory.resolve("vtts_summary.csv"));
		double twoSum = marginal(8, 8) + marginal(16, 14);
		double threeSum = marginal(8, 8) + marginal(1, 1) + marginal(16, 12);
		assertEquals((twoSum / 2 + threeSum / 3) / 2, mean(summary, "person"), 1e-6);
		assertEquals((twoSum + threeSum) / 5, mean(summary, "trip"), 1e-6);
		// The browser guesses delimiters for this headerless file. Quoted labels keep their
		// spaces intact; all rows must have the same three columns, including an optional icon.
		Path overview = directory.resolve("vtts_overview.csv");
		assertTrue(Files.readString(overview).startsWith("\"Agents in population\",\"2\",\"user-group\""));
		try (var csv = CSVFormat.DEFAULT.parse(IOUtils.getBufferedReader(overview.toString()))) {
			var tiles = csv.getRecords();
			assertEquals(5, tiles.size());
			tiles.forEach(row -> assertEquals(3, row.size()));
			assertEquals(List.of("2", "2", "2", "5", "5"), tiles.stream().map(row -> row.get(1)).toList());
			assertEquals("Trips with VTTS", tiles.getLast().get(0));
			assertEquals("", tiles.getLast().get(2));
		}
		assertEquals("ok", read(directory.resolve("vtts_coverage.csv")).getFirst().get("status"));
		var reloaded = PopulationUtils.readPopulation(directory.resolve("vtts_experienced_plans.xml.gz").toString());
		assertEquals(getDouble(two, VTTS), getDouble(reloaded.getPersons().get(two.getId()), VTTS));
		assertSame(two, plans.get(two.getId()).getPerson());
		assertSame(three, plans.get(three.getId()).getPerson());
	}

	@Test
	void exportsIncompletePlansWithoutInventingDestinationRows() throws IOException {
		var scenario = scenario();
		var outbound = person(scenario, "outbound", 2000, false);
		var returning = person(scenario, "returning", 2000, false);
		var initialLeg = person(scenario, "initial_leg", 2000, false);
		var outboundElements = outbound.getSelectedPlan().getPlanElements();
		outboundElements.subList(2, outboundElements.size()).clear(); // home -> leg, no arrival
		returning.getSelectedPlan().getPlanElements().removeLast(); // home -> work -> leg
		initialLeg.getSelectedPlan().getPlanElements().removeFirst(); // leg -> work -> home
		var analysis = new VttsAnalysis(muse(scenario), VttsAnalysisTest.parameters(2, -6), new DefaultAnalysisMainModeIdentifier());
		Map<Id<Person>, Plan> plans = new LinkedHashMap<>();
		for (var person : scenario.getPopulation().getPersons().values()) {
			plans.put(person.getId(), person.getSelectedPlan());
			analysis.analyze(person, person.getSelectedPlan());
		}

		new VttsCsvWriter().write(directory, scenario.getPopulation(), plans);

		var persons = read(directory.resolve("vtts_persons.csv.gz"));
		assertEquals(3, persons.size());
		persons.forEach(row -> {
			assertEquals("incomplete_plan", row.get("status"));
			assertEquals("", row.get("mean_vtts_money_per_hour"));
		});
		var trips = read(directory.resolve("vtts_trips.csv.gz"));
		assertEquals(2, trips.size()); // Only the closed home -> work and work -> home trips.
		assertTrue(trips.stream().noneMatch(row -> row.get("person_id").equals("outbound")));
		trips.forEach(row -> {
			assertEquals("incomplete_plan", row.get("status"));
			assertEquals("", row.get("muse_score_per_hour"));
		});
		var coverage = read(directory.resolve("vtts_coverage.csv")).getFirst();
		assertEquals("incomplete_plan", coverage.get("status"));
		assertEquals("3", coverage.get("persons"));
	}

	static List<CSVRecord> read(Path path) throws IOException {
		try (var reader = IOUtils.getBufferedReader(path.toString());
			 var csv = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
			return csv.getRecords();
		}
	}

	private static double mean(List<CSVRecord> rows, String weighting) {
		return rows.stream().filter(row -> row.get("metric").equals("MUSE") && row.get("weighting").equals(weighting))
			.mapToDouble(row -> Double.parseDouble(row.get("mean"))).findFirst().orElseThrow();
	}
}
