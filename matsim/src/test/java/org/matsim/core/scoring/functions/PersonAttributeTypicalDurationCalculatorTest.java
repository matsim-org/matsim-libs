package org.matsim.core.scoring.functions;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.SumScoringFunction;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PersonAttributeTypicalDurationCalculator} in {@link CharyparNagelActivityScoring}, on a day as the
 * events hand it to the scoring: no attributes, and stage activities in between.
 */
class PersonAttributeTypicalDurationCalculatorTest {

	private static final String CAR_INTERACTION = "car interaction";

	/** Main activities of the day and their typical durations; "work" twice, with different typical durations. */
	private static final List<PersonUtils.TypicalDuration> DAY = List.of(
		new PersonUtils.TypicalDuration("home_morning", 25200),
		new PersonUtils.TypicalDuration("work", 15000),
		new PersonUtils.TypicalDuration("shop_daily", 1800),
		new PersonUtils.TypicalDuration("work", 14000),
		new PersonUtils.TypicalDuration("home_evening", 37000));

	private final ScoringParameters params = params();

	/**
	 * The person attribute gives the same score as the activity attributes with the same values, with the same pairing
	 * of executed and planned activities; stage activities do not take entries.
	 */
	@Test
	void scoresLikeTheActivityAttribute() {
		Person withActivityAttributes = person(plan(true));
		double activityAttribute = score(new CharyparNagelActivityScoring(params, new ActivityAttributeTypicalDurationCalculator(), withActivityAttributes), executedDay());

		Person person = person(plan(false));
		PersonUtils.setTypicalDurations(person, DAY);
		double personAttribute = score(scoring(person), executedDay());

		assertThat(personAttribute).isEqualTo(activityAttribute);
		// and the individual values make a difference
		assertThat(personAttribute).isNotEqualTo(score(new CharyparNagelActivityScoring(params), executedDay()));
	}

	/**
	 * Persons without the attribute, and scoring without a person, use the config values, as without the calculator.
	 */
	@Test
	void withoutAttributeFallsBackToConfig() {
		double stock = score(new CharyparNagelActivityScoring(params), executedDay());
		assertThat(score(scoring(person(plan(false))), executedDay())).isEqualTo(stock);
		assertThat(score(new CharyparNagelActivityScoring(params, new PersonAttributeTypicalDurationCalculator(), null), executedDay())).isEqualTo(stock);
	}

	@Test
	void wrongTypeAborts() {
		Person person = person(plan(false));
		PersonUtils.setTypicalDurations(person, List.of(DAY.get(0), DAY.get(1), new PersonUtils.TypicalDuration("leisure", 1800), DAY.get(3), DAY.get(4)));
		assertThatThrownBy(() -> score(scoring(person), executedDay()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("is for type leisure");
	}

	@Test
	void tooFewEntriesAbort() {
		Person person = person(plan(false));
		PersonUtils.setTypicalDurations(person, DAY.subList(0, 4));
		assertThatThrownBy(() -> score(scoring(person), executedDay()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("has only 4 entries");
	}

	@Test
	void nonPositiveEntryAborts() {
		Person person = person(plan(false));
		PersonUtils.setTypicalDurations(person, List.of(DAY.get(0), DAY.get(1), new PersonUtils.TypicalDuration("shop_daily", 0), DAY.get(3), DAY.get(4)));
		assertThatThrownBy(() -> score(scoring(person), executedDay()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("is not positive");
	}

	/**
	 * When the executed activities cannot be paired with the plan, the attribute cannot be applied either.
	 */
	@Test
	void brokenPairingAborts() {
		Person person = person(plan(false));
		PersonUtils.setTypicalDurations(person, DAY);
		List<Activity> executed = executedDay();
		// the plan's second work activity executed as a shop activity
		executed.set(5, executed("shop_daily", 12 * 3600 + 3000, 17 * 3600));
		assertThatThrownBy(() -> score(scoring(person), executed))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("could not be paired");
	}

	private CharyparNagelActivityScoring scoring(Person person) {
		return new CharyparNagelActivityScoring(params, new PersonAttributeTypicalDurationCalculator(), person);
	}

	private static ScoringParameters params() {
		Config config = ConfigUtils.createConfig();
		config.scenario().setSimulationPeriodInDays(1.125);
		ScoringConfigGroup scoring = config.scoring();
		scoring.setDefaultPerforming_utils_hr(6.0);
		for (String type : List.of("home_morning", "work", "shop_daily", "home_evening")) {
			scoring.addDefaultActivityParams(new ScoringConfigGroup.ActivityParams(type).setTypicalDuration(8 * 3600));
		}
		scoring.addDefaultActivityParams(new ScoringConfigGroup.ActivityParams(CAR_INTERACTION).setScoringThisActivityAtAll(false));
		return new ScoringParameters.Builder(scoring, scoring.getScoringParametersOrDefault(null), config.scenario()).build();
	}

	private static Person person(Plan plan) {
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("p"));
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		return person;
	}

	/** The planned day; with the typical durations as activity attributes or without. */
	private static Plan plan(boolean activityAttributes) {
		Plan plan = PopulationUtils.createPlan();
		int main = 0;
		for (String type : List.of("home_morning", CAR_INTERACTION, CAR_INTERACTION, "work", "shop_daily", "work", "home_evening")) {
			if (!plan.getPlanElements().isEmpty()) {
				plan.addLeg(PopulationUtils.createLeg(TransportMode.car));
			}
			Activity act = PopulationUtils.createActivityFromLinkId(type, Id.createLinkId("l"));
			if (!type.equals(CAR_INTERACTION)) {
				if (activityAttributes) {
					act.getAttributes().putAttribute(ActivityAttributeTypicalDurationCalculator.TYPICAL_DURATION_ATTRIBUTE, DAY.get(main).seconds());
				}
				main++;
			}
			plan.addActivity(act);
		}
		return plan;
	}

	/** The day as the events hand it to the scoring: times, no attributes, stage activities included. */
	private static List<Activity> executedDay() {
		return new ArrayList<>(List.of(
			executed("home_morning", Double.NaN, 7 * 3600),
			executed(CAR_INTERACTION, 7 * 3600 + 300, 7 * 3600 + 300),
			executed(CAR_INTERACTION, 7 * 3600 + 2400, 7 * 3600 + 2400),
			executed("work", 7 * 3600 + 2700, 12 * 3600),
			executed("shop_daily", 12 * 3600 + 600, 12 * 3600 + 2400),
			executed("work", 12 * 3600 + 3000, 17 * 3600),
			executed("home_evening", 17 * 3600 + 1800, Double.NaN)));
	}

	private static Activity executed(String type, double start, double end) {
		Activity act = PopulationUtils.createActivityFromLinkId(type, Id.createLinkId("l"));
		if (!Double.isNaN(start)) {
			act.setStartTime(start);
		}
		if (!Double.isNaN(end)) {
			act.setEndTime(end);
		}
		return act;
	}

	private static double score(CharyparNagelActivityScoring scoring, List<Activity> executed) {
		SumScoringFunction sf = new SumScoringFunction();
		sf.addScoringFunction(scoring);
		executed.forEach(sf::handleActivity);
		sf.finish();
		return sf.getScore();
	}
}
