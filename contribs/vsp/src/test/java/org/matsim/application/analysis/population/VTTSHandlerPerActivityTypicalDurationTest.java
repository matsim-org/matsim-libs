package org.matsim.application.analysis.population;

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
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ActivityAttributeTypicalDurationCalculator;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Verifies that {@link VTTSHandler} can be instantiated with a per-activity-parameterized activity scoring: the
 * activities it reconstructs from the events are aligned with the selected plan and carry the plan activities'
 * attributes, so an {@link ActivityAttributeTypicalDurationCalculator}-based scoring sees the per-activity typical
 * durations.  The equivalence tested here is the defining property: an activity carrying typical duration T must
 * yield exactly the marginals of a run whose config gives its activity type typical duration T.
 */
public class VTTSHandlerPerActivityTypicalDurationTest {

	private static final Id<Person> PID = Id.createPersonId("p");
	private static final Id<Link> LINK = Id.createLinkId("l");

	private static Config config(double workTypicalDuration) {
		Config config = ConfigUtils.createConfig();
		ScoringConfigGroup scoring = config.scoring();
		scoring.setDefaultPerforming_utils_hr(6.);
		scoring.addDefaultActivityParams(new ScoringConfigGroup.ActivityParams("home").setTypicalDuration(8. * 3600.));
		scoring.addDefaultActivityParams(new ScoringConfigGroup.ActivityParams("work").setTypicalDuration(workTypicalDuration));
		scoring.addDefaultActivityParams(new ScoringConfigGroup.ActivityParams("home_evening").setTypicalDuration(8. * 3600.));
		return config;
	}

	private static Scenario scenario(Config config, Double workTypicalDurationAttribute) {
		Scenario scenario = ScenarioUtils.createScenario(config);
		Person person = scenario.getPopulation().getFactory().createPerson(PID);
		Plan plan = PopulationUtils.createPlan();
		Activity home = PopulationUtils.createAndAddActivityFromLinkId(plan, "home", LINK);
		home.setEndTime(28800.);
		PopulationUtils.createAndAddLeg(plan, "car");
		Activity work = PopulationUtils.createAndAddActivityFromLinkId(plan, "work", LINK);
		work.setEndTime(59400.);
		if (workTypicalDurationAttribute != null) {
			work.getAttributes().putAttribute(ActivityAttributeTypicalDurationCalculator.TYPICAL_DURATION_ATTRIBUTE, workTypicalDurationAttribute);
		}
		PopulationUtils.createAndAddLeg(plan, "car");
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home_evening", LINK);
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		scenario.getPopulation().addPerson(person);
		return scenario;
	}

	/** The events of the day home - car - work - car - home_evening; the handler reconstructs bare activities from them. */
	private static double workTripMarginalUtilityOfActivityTime(VTTSHandler handler) {
		handler.reset(0);
		handler.handleEvent(new ActivityEndEvent(28800., PID, LINK, null, "home"));
		handler.handleEvent(new PersonDepartureEvent(28800., PID, LINK, "car", "car"));
		handler.handleEvent(new ActivityStartEvent(30600., PID, LINK, null, "work"));
		handler.handleEvent(new ActivityEndEvent(59400., PID, LINK, null, "work"));
		handler.handleEvent(new PersonDepartureEvent(59400., PID, LINK, "car", "car"));
		handler.handleEvent(new ActivityStartEvent(61200., PID, LINK, null, "home_evening"));
		handler.computeFinalVTTS();
		List<VTTSHandler.TripData> trips = handler.getTripDataMap().get(PID);
		// trip 0 is the trip to work; its marginal utility of schedule delay refers to the work activity
		return trips.getFirst().musl_h;
	}

	@Test
	void attributeReproducesConfigTypicalDuration() {
		// A: work typical 2h in the config, but 8h as attribute of the plan's work activity, scored per activity
		Scenario scenarioA = scenario(config(2. * 3600.), 8. * 3600.);
		VTTSHandler withAttribute = new VTTSHandler(scenarioA, new SubpopulationScoringParameters(scenarioA),
				params -> new CharyparNagelActivityScoring(params, new ActivityAttributeTypicalDurationCalculator(), null));

		// B: work typical 8h in the config, stock scoring
		Scenario scenarioB = scenario(config(8. * 3600.), null);
		VTTSHandler fromConfig = new VTTSHandler(scenarioB, new SubpopulationScoringParameters(scenarioB), null);

		// C: same as A but with stock scoring, which ignores the attribute
		Scenario scenarioC = scenario(config(2. * 3600.), 8. * 3600.);
		VTTSHandler stock = new VTTSHandler(scenarioC, new SubpopulationScoringParameters(scenarioC), null);

		double marginalWithAttribute = workTripMarginalUtilityOfActivityTime(withAttribute);
		double marginalFromConfig = workTripMarginalUtilityOfActivityTime(fromConfig);
		double marginalStock = workTripMarginalUtilityOfActivityTime(stock);

		assertEquals(marginalFromConfig, marginalWithAttribute, 1e-9);
		assertNotEquals(marginalStock, marginalWithAttribute, 1e-3);
	}
}
