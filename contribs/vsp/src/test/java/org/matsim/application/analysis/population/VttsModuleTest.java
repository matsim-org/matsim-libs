package org.matsim.application.analysis.population;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
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
		Scenario scenario = ScenarioUtils.loadScenario(config);
		var sample = scenario.getPopulation().getPersons().values().iterator().next().getSelectedPlan();
		var activities = TripStructureUtils.getActivities(sample, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
		// MATSim records no experienced activities for these all-day plans. They must be
		// not_mobile, rather than mismatches between one planned and zero experienced activities.
		addAllDayPerson(scenario, "stays_home", activities.getFirst(), null);
		addAllDayPerson(scenario, "stays_work", activities.get(1), null);
		addAllDayPerson(scenario, "excluded_freight", activities.getFirst(), "freight");
		Controler controller = new Controler(scenario);
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
		for (String id : new String[]{"stays_home", "stays_work"}) {
			var row = rows.stream().filter(r -> r.get("person_id").equals(id)).findFirst().orElseThrow();
			assertEquals("not_mobile", row.get("status"));
			assertEquals("0", row.get("valid_destinations"));
			assertEquals("", row.get("mean_vtts_money_per_hour"));
		}
		assertEquals("excluded", rows.stream().filter(r -> r.get("person_id").equals("excluded_freight"))
			.findFirst().orElseThrow().get("status"));
		var coverage = VttsCsvWriterTest.read(output.resolve("analysis/vtts/vtts_coverage.csv"));
		assertTrue(coverage.stream().filter(row -> row.get("status").equals("not_mobile"))
			.findFirst().orElseThrow().get("meaning").contains("No trips"));
		assertTrue(Files.exists(output.resolve("analysis/vtts/vtts_experienced_plans.xml.gz")));
		String dashboard = Files.readString(output.resolve("dashboard-1.yaml"));
		assertTrue(dashboard.contains("analysis/vtts/vtts_persons.csv.gz"));
		assertTrue(dashboard.contains("Included subpopulation: person"));
	}

	private static void addAllDayPerson(Scenario scenario, String id, Activity template, String subpopulation) {
		var population = scenario.getPopulation();
		var person = population.getFactory().createPerson(Id.createPersonId(id));
		if (subpopulation != null) PopulationUtils.putSubpopulation(person, subpopulation);
		var plan = population.getFactory().createPlan();
		plan.addActivity(PopulationUtils.createActivityFromLinkId(template.getType(), template.getLinkId()));
		person.addPlan(plan);
		population.addPerson(person);
	}
}
