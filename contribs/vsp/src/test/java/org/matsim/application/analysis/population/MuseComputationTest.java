package org.matsim.application.analysis.population;

import com.google.inject.Injector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.TypicalDurationSource;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.ExcludeStageActivities;

/**
 * Verifies that {@link MuseComputation}, and the activity score of {@link AgentWiseComparisonKN}, score the activities
 * against the typical durations the run's scoring uses, also when these come from the person
 * ({@link TypicalDurationSource#personAttribute}, as in matsim-berlin v7.2).  The scoring then pairs the handed
 * activities with the main activities of the selected plan by position, so they have to be handed in plan order.  The
 * equivalence tested is the one of {@link VTTSHandlerPerActivityTypicalDurationTest}: an activity with its own typical
 * duration T must yield exactly what a run whose config gives its activity type typical duration T yields.
 */
public class MuseComputationTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	private static final Id<Link> LINK = Id.createLinkId("l");

	/** The person's typical durations, in plan order.  The morning one is not used: the evening activity of the same type is scored together with it. */
	private static final List<PersonUtils.TypicalDuration> INDIVIDUAL = List.of(
		new PersonUtils.TypicalDuration("home", 6. * 3600.),
		new PersonUtils.TypicalDuration("work", 8. * 3600.),
		new PersonUtils.TypicalDuration("leisure", 1.5 * 3600.),
		new PersonUtils.TypicalDuration("home", 10. * 3600.));

	/** The config typical durations that give the activities their individual ones. */
	private static final Map<String, Double> AS_CONFIG = Map.of("home", 10. * 3600., "work", 8. * 3600., "leisure", 1.5 * 3600.);

	/** Config typical durations different from the individual ones. */
	private static final Map<String, Double> OTHER_CONFIG = Map.of("home", 8. * 3600., "work", 2. * 3600., "leisure", 3. * 3600.);

	/**
	 * @param museH      the MUSE of the activities after the first, in utils/h
	 * @param planMuseH  their mean, as put into the plan
	 * @param actsScore  the activity score as {@link AgentWiseComparisonKN} computes it
	 */
	private record Result(List<Double> museH, double planMuseH, double actsScore) {
	}

	private Result compute(Map<String, Double> typicalDurations, TypicalDurationSource source) {
		Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.scoring().setDefaultPerforming_utils_hr(6.);
		typicalDurations.forEach((type, typicalDuration) -> config.scoring().addDefaultActivityParams(new ScoringConfigGroup.ActivityParams(type).setTypicalDuration(typicalDuration)));
		config.scoring().setTypicalDurationSource(source);

		Scenario scenario = ScenarioUtils.createScenario(config);
		Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("p"));
		PersonUtils.setIncome(person, 2000.);
		PersonUtils.setTypicalDurations(person, INDIVIDUAL);

		// as in the experienced plans: the first activity without start time, the last one without end time
		Plan plan = PopulationUtils.createPlan();
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", LINK).setEndTime(7. * 3600.);
		PopulationUtils.createAndAddLeg(plan, "car");
		Activity work = PopulationUtils.createAndAddActivityFromLinkId(plan, "work", LINK);
		work.setStartTime(7.5 * 3600.);
		work.setEndTime(15.5 * 3600.);
		PopulationUtils.createAndAddLeg(plan, "car");
		Activity leisure = PopulationUtils.createAndAddActivityFromLinkId(plan, "leisure", LINK);
		leisure.setStartTime(16. * 3600.);
		leisure.setEndTime(17.5 * 3600.);
		PopulationUtils.createAndAddLeg(plan, "car");
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", LINK).setStartTime(18. * 3600.);
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		scenario.getPopulation().addPerson(person);

		Injector injector = AgentWiseComparisonKNUtils.createInjector(scenario);
		injector.getInstance(MuseComputation.class).computeMuseForAllActs(plan);

		ScoringFunction sf = injector.getInstance(ScoringFunctionFactory.class).createNewScoringFunction(person);
		TripStructureUtils.getActivities(plan, ExcludeStageActivities).forEach(sf::handleActivity);
		sf.finish();

		List<Double> museH = TripStructureUtils.getActivities(plan, ExcludeStageActivities).stream().skip(1).map(AddVttsEtcToActivities::getMUSE_h).toList();
		return new Result(museH, AddVttsEtcToActivities.getMUSE_h(plan), sf.getScore());
	}

	@Test
	void personTypicalDurationsReproduceConfigTypicalDurations() {
		Result fromConfig = compute(AS_CONFIG, TypicalDurationSource.activityParams);
		Result stock = compute(OTHER_CONFIG, TypicalDurationSource.activityParams);
		Result individual = compute(OTHER_CONFIG, TypicalDurationSource.personAttribute);

		for (int act = 0; act < 3; act++) {
			assertEquals(fromConfig.museH().get(act), individual.museH().get(act), 1e-9, "activity " + (act + 1));
			assertNotEquals(stock.museH().get(act), individual.museH().get(act), 1e-3, "activity " + (act + 1));
		}
		assertEquals(fromConfig.planMuseH(), individual.planMuseH(), 1e-9);
		assertEquals(fromConfig.actsScore(), individual.actsScore(), 1e-9);

		// work: performing times typical over actual duration
		assertEquals(6. * 8. / 8., fromConfig.museH().get(0), 1e-3);
	}

}
