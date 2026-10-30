package org.matsim.simwrapper.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.matsim.simwrapper.SimWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VttsDashboardTest {

	@TempDir Path directory;

	@Test
	void referencesExistingDataWithoutRegisteringAScoringCommand() throws IOException {
		SimWrapper wrapper = SimWrapper.create();
		wrapper.addDashboard(new VttsDashboard("analysis/custom_vtts", "Included subpopulation: berlin_person."));
		wrapper.generate(directory);
		// SimWrapper numbers generated dashboards from 1.
		String yaml = Files.readString(directory.resolve("dashboard-1.yaml"));
		// Browser discovery checks only current-folder filenames. A trigger pointing at the
		// nested person CSV would silently hide the dashboard even when those data exist.
		assertFalse(yaml.contains("triggerPattern:"));
		assertTrue(yaml.contains("Included subpopulation: berlin_person."));
		assertTrue(yaml.contains("analysis/custom_vtts/vtts_persons.csv.gz"));
		assertTrue(yaml.contains("analysis/custom_vtts/vtts_summary.csv"));
		assertTrue(yaml.contains("analysis/custom_vtts/vtts_coverage.csv"));
		assertTrue(yaml.contains("mean_vtts_money_per_hour"));
		// The two tabs must use different observation levels, not duplicate person means.
		JsonNode dashboard = new ObjectMapper(new YAMLFactory()).readTree(yaml);
		assertEquals("Per person", dashboard.path("subtabs").get(0).path("title").asText());
		assertEquals("Per trip", dashboard.path("subtabs").get(1).path("title").asText());
		for (String metric : List.of("vtts", "muse", "vse")) {
			String column = metric.equals("muse") ? "muse_score_per_hour" : metric + "_money_per_hour";
			JsonNode person = dashboard.path("layout").path(metric).get(0);
			JsonNode trip = dashboard.path("layout").path("trip_" + metric).get(0);
			assertEquals("analysis/custom_vtts/vtts_persons.csv.gz", person.path("datasets").path("dataset").path("file").asText());
			assertEquals("analysis/custom_vtts/vtts_trips.csv.gz", trip.path("datasets").path("dataset").path("file").asText());
			assertEquals("$dataset.mean_" + column, person.path("traces").get(0).path("x").asText());
			assertEquals("$dataset." + column, trip.path("traces").get(0).path("x").asText());
			assertEquals("Trips", trip.path("layout").path("yaxis").path("title").asText());
			assertTrue(trip.path("datasets").path("dataset").path("filter").path(column).path("invert").asBoolean());
			assertTrue(dashboard.path("subtabs").get(1).path("rows").toString().contains("trip_" + metric));
		}
		// No run config, events or plans are needed to generate and run this file-only dashboard.
		wrapper.run(directory);
	}
}
