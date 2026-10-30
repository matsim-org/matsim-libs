package org.matsim.application.analysis.population;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** End-to-end check that the listener uses the controller's custom factory and exports before shutdown. */
class VttsModuleTest {

	@TempDir Path directory;

	@Test
	void usesTheRunsScoringFactoryAndCreatesDashboardData() throws IOException {
		var config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		Path output = directory.resolve("run");
		config.controller().setOutputDirectory(output.toString());
		config.controller().setLastIteration(0);
		// The integration must use in-memory experienced plans even if the usual XML output is disabled.
		config.scoring().setWriteExperiencedPlans(false);
		config.scoring().setDefaultMarginalUtilityOfMoney(2);
		config.scoring().getOrCreateDefaultModeParams("car").setMarginalUtilityOfTraveling(-6);
		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class)
			.setDefaultDashboards(SimWrapperConfigGroup.DefaultDashboardsMode.disabled);
		Controler controller = new Controler(config);
		controller.addOverridingModule(new SimWrapperModule());
		controller.addOverridingModule(new VttsModule());
		controller.addOverridingModule(new AbstractModule() {
			@Override public void install() {
				// This factory gives MUSE = 12, irrespective of the config's performing coefficient.
				// Reconstructing a standard factory instead of using the run's binding would fail the test.
				bindScoringFunctionFactory().toInstance(person -> {
					SumScoringFunction scoring = new SumScoringFunction();
					scoring.addScoringFunction(new SumScoringFunction.ActivityScoring() {
						private double score;
						@Override public void handleFirstActivity(Activity activity) { }
						@Override public void handleActivity(Activity activity) { score -= activity.getStartTime().seconds() * 12 / 3600; }
						@Override public void handleLastActivity(Activity activity) { handleActivity(activity); }
						@Override public void finish() { }
						@Override public double getScore() { return score; }
					});
					return scoring;
				});
			}
		});

		controller.run();

		var rows = VttsCsvWriterTest.read(output.resolve("analysis/vtts/vtts_persons.csv.gz"));
		var valid = rows.stream().filter(row -> row.get("status").equals("ok")).toList();
		assertFalse(valid.isEmpty());
		for (var row : valid) {
			assertEquals(12, Double.parseDouble(row.get("mean_muse_score_per_hour")), 1e-6);
			assertEquals(9, Double.parseDouble(row.get("mean_vtts_money_per_hour")), 1e-6);
		}
		assertTrue(Files.exists(output.resolve("analysis/vtts/vtts_experienced_plans.xml.gz")));
		assertTrue(Files.readString(output.resolve("dashboard-1.yaml")).contains("analysis/vtts/vtts_persons.csv.gz"));
	}
}
