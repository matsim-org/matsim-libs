package org.matsim.application.analysis.population.kn;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.TypicalDurationSource;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ActivityAttributeTypicalDurationCalculator;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Verifies that {@link VTTSHandler} scores the activities it reconstructs from the events against the typical
 * durations the run's scoring uses: it aligns them with the selected plan and resolves their typical durations from
 * the configured source.  The equivalence tested here is the defining property: an activity with its own typical
 * duration T must yield exactly the marginals of a run whose config gives its activity type typical duration T.
 */
public class VTTSHandlerPerActivityTypicalDurationTest {

	private static final Id<Person> PID = Id.createPersonId("p");
	private static final Id<Link> LINK = Id.createLinkId("l");

	/** The individual typical durations; all different from the config ones, the morning and evening ones from each other. */
	private static final Map<String, Double> INDIVIDUAL = Map.of("home", 6. * 3600., "work", 8. * 3600., "home_evening", 10. * 3600.);

	private static Config config(Map<String, Double> typicalDurations, TypicalDurationSource source) {
		Config config = ConfigUtils.createConfig();
		ScoringConfigGroup scoring = config.scoring();
		scoring.setDefaultPerforming_utils_hr(6.);
		typicalDurations.forEach((type, typicalDuration) -> scoring.addDefaultActivityParams(new ScoringConfigGroup.ActivityParams(type).setTypicalDuration(typicalDuration)));
		scoring.setTypicalDurationSource(source);
		return config;
	}

	/** Config typical durations different from the individual ones. */
	private static Config otherConfig(TypicalDurationSource source) {
		return config(Map.of("home", 8. * 3600., "work", 2. * 3600., "home_evening", 8. * 3600.), source);
	}

	/** home - car - work - car - home_evening, with the individual typical durations as activity and person attributes. */
	private static Scenario scenario(Config config) {
		Scenario scenario = ScenarioUtils.createScenario(config);
		Person person = scenario.getPopulation().getFactory().createPerson(PID);
		Plan plan = PopulationUtils.createPlan();
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", LINK).setEndTime(28800.);
		PopulationUtils.createAndAddLeg(plan, "car");
		PopulationUtils.createAndAddActivityFromLinkId(plan, "work", LINK).setEndTime(59400.);
		PopulationUtils.createAndAddLeg(plan, "car");
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home_evening", LINK);
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		List<Activity> activities = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
		activities.forEach(act -> act.getAttributes().putAttribute(ActivityAttributeTypicalDurationCalculator.TYPICAL_DURATION_ATTRIBUTE, INDIVIDUAL.get(act.getType())));
		PersonUtils.setTypicalDurations(person, activities.stream().map(act -> new PersonUtils.TypicalDuration(act.getType(), INDIVIDUAL.get(act.getType()))).toList());
		scenario.getPopulation().addPerson(person);
		return scenario;
	}

	/** The events of the day; the handler reconstructs bare activities from them. Trip 0 goes to work, trip 1 home. */
	private static List<VTTSHandler.TripData> trips(Config config) {
		Scenario scenario = scenario(config);
		VTTSHandler handler = new VTTSHandler(scenario, new SubpopulationScoringParameters(scenario));
		handler.reset(0);
		handler.handleEvent(new ActivityEndEvent(28800., PID, LINK, null, "home"));
		handler.handleEvent(new PersonDepartureEvent(28800., PID, LINK, "car", "car"));
		handler.handleEvent(new ActivityStartEvent(30600., PID, LINK, null, "work"));
		handler.handleEvent(new ActivityEndEvent(59400., PID, LINK, null, "work"));
		handler.handleEvent(new PersonDepartureEvent(59400., PID, LINK, "car", "car"));
		handler.handleEvent(new ActivityStartEvent(61200., PID, LINK, null, "home_evening"));
		handler.computeFinalVTTS();
		return handler.getTripDataMap().get(PID);
	}

	@Test
	void individualTypicalDurationsReproduceConfigTypicalDurations() {
		List<VTTSHandler.TripData> fromConfig = trips(config(INDIVIDUAL, TypicalDurationSource.activityParams));
		List<VTTSHandler.TripData> stock = trips(otherConfig(TypicalDurationSource.activityParams));

		for (TypicalDurationSource source : List.of(TypicalDurationSource.activityAttribute, TypicalDurationSource.personAttribute)) {
			List<VTTSHandler.TripData> individual = trips(otherConfig(source));
			for (int trip = 0; trip < 2; trip++) {
				// trip 1 ends the day: the overnight pair home / home_evening, each with its own typical duration
				assertEquals(fromConfig.get(trip).musl_h, individual.get(trip).musl_h, 1e-9, source + ", trip " + trip);
				// the morning activity cancels out of the marginal, but not out of the activity score
				assertEquals(fromConfig.get(trip).actScore, individual.get(trip).actScore, 1e-9, source + ", trip " + trip);
				assertEquals(fromConfig.get(trip).actTypDur_h, individual.get(trip).actTypDur_h, 1e-9, source + ", trip " + trip);
				assertNotEquals(stock.get(trip).musl_h, individual.get(trip).musl_h, 1e-3, source + ", trip " + trip);
			}
		}
		assertEquals(8., fromConfig.get(0).actTypDur_h, 1e-9);
		assertEquals(10., fromConfig.get(1).actTypDur_h, 1e-9);
	}
}
