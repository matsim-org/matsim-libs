package org.matsim.simwrapper.dashboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.matsim.simwrapper.SimWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VttsDashboardTest {

	@TempDir Path directory;

	@Test
	void referencesExistingDataWithoutRegisteringAScoringCommand() throws IOException {
		SimWrapper wrapper = SimWrapper.create();
		wrapper.addDashboard(new VttsDashboard("analysis/custom_vtts"));
		wrapper.generate(directory);
		// SimWrapper numbers generated dashboards from 1.
		String yaml = Files.readString(directory.resolve("dashboard-1.yaml"));
		assertTrue(yaml.contains("analysis/custom_vtts/vtts_persons.csv.gz"));
		assertTrue(yaml.contains("analysis/custom_vtts/vtts_summary.csv"));
		assertTrue(yaml.contains("analysis/custom_vtts/vtts_coverage.csv"));
		assertTrue(yaml.contains("mean_vtts_money_per_hour"));
		// No run config, events or plans are needed to generate and run this file-only dashboard.
		wrapper.run(directory);
	}
}
