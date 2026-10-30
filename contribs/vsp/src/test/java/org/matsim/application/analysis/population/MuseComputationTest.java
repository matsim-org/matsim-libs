package org.matsim.application.analysis.population;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.ActivityAttributeTypicalDurationCalculator;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.matsim.application.analysis.population.kn.AddVttsEtcToActivities.getMUSE_h;
import static org.matsim.application.analysis.population.kn.KNTestFixture.*;
import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.ExcludeStageActivities;

/**
 * KN MUSE characterization tests retained as the refactoring baseline, plus scorer-lifecycle tests.
 *
 * <p>MUSE is the activity score gained by starting one second earlier, scaled to score/hour.
 * These tests compare the existing production calculation with independent expected values;
 * they do not compute full monetary VTTS or exercise the policy-comparison workflow.
 * Current edge-case behavior is recorded explicitly rather than changed by the tests.
 */
class MuseComputationTest {

	// Subtracting nearby scores and multiplying by 3600 can amplify floating-point error.
	private static final double TOLERANCE = 1e-6;

	/** Checks the work marginal, overnight home marginal, and mean over the two destinations. */
	@Test
	void computesNormalAndOvernightActivityMarginalsWithoutChangingTimes() {
		var scenario = scenario();
		var person = person(scenario, "person", 2000, false);
		var plan = person.getSelectedPlan();
		var activities = TripStructureUtils.getActivities(plan, ExcludeStageActivities);
		// An arbitrary existing attribute must survive the temporary activity perturbations.
		activities.get(1).getAttributes().putAttribute("customActivityAttribute", 42.);

		muse(scenario).computeMuseForAllActs(plan);

		// Work lasts its typical 8 hours, so its marginal is approximately 6 score/hour.
		double work = marginal(8, 8);
		// The matching home types are scored together: 8 morning hours + 6 evening hours.
		// With a typical duration of 16 hours, the marginal is approximately 6.86 score/hour.
		double home = marginal(16, 14);
		// The morning activity is an origin, not an incoming-trip destination. The plan mean
		// therefore includes work and evening home, without an extra morning observation.
		assertNull(getMUSE_h(activities.getFirst()));
		assertEquals(work, getMUSE_h(activities.get(1)), TOLERANCE);
		assertEquals(home, getMUSE_h(activities.getLast()), TOLERANCE);
		assertEquals((work + home) / 2, getMUSE_h(plan), TOLERANCE);
		// Starting an activity earlier is a scoring experiment only. The original experienced
		// schedule, custom attribute, plan selection, and stored score must remain intact.
		assertTrue(activities.getFirst().getStartTime().isUndefined());
		assertEquals(8 * HOUR, activities.getFirst().getEndTime().seconds());
		assertEquals(9 * HOUR, activities.get(1).getStartTime().seconds());
		assertEquals(17 * HOUR, activities.get(1).getEndTime().seconds());
		assertEquals(18 * HOUR, activities.getLast().getStartTime().seconds());
		assertTrue(activities.getLast().getEndTime().isUndefined());
		assertEquals(42., activities.get(1).getAttributes().getAttribute("customActivityAttribute"));
		assertSame(plan, person.getSelectedPlan());
		assertEquals(0., plan.getScore(), TOLERANCE);
	}

	/** A different evening type must not borrow duration from the morning activity. */
	@Test
	void scoresDifferentFirstAndLastActivityTypesSeparately() {
		var scenario = scenario();
		var plan = person(scenario, "person", 2000, false).getSelectedPlan();
		var activities = TripStructureUtils.getActivities(plan, ExcludeStageActivities);
		// Only the type changes; the typical duration and experienced times remain the same.
		activities.getLast().setType("other_home");

		muse(scenario).computeMuseForAllActs(plan);

		// Standard scoring now treats morning and evening separately. The evening lasts
		// from 18:00 to midnight (6 hours); the unchanged morning score cancels in the difference.
		double evening = marginal(16, 6);
		assertEquals(evening, getMUSE_h(activities.getLast()), TOLERANCE);
		assertEquals((marginal(8, 8) + evening) / 2, getMUSE_h(plan), TOLERANCE);
	}

	/** A stage interaction must not become an extra destination in the activity average. */
	@Test
	void excludesStageActivitiesFromTheCalculationAndPlanMean() {
		var scenario = scenario();
		var plan = person(scenario, "person", 2000, false).getSelectedPlan();
		// Split the outbound hour into two half-hour legs around a zero-duration interaction.
		// Home still ends at 08:00 and work still starts at 09:00, so main-activity MUSE is unchanged.
		Activity stage = PopulationUtils.createActivityFromLinkId(TripStructureUtils.createStageActivityType("car"), LINK);
		stage.setStartTime(8.5 * HOUR);
		stage.setEndTime(8.5 * HOUR);
		Leg firstLeg = (Leg) plan.getPlanElements().get(1);
		firstLeg.setTravelTime(0.5 * HOUR);
		firstLeg.getRoute().setTravelTime(0.5 * HOUR);
		Leg continuation = PopulationUtils.createLeg(firstLeg);
		continuation.setDepartureTime(8.5 * HOUR);
		plan.getPlanElements().add(2, stage);
		plan.getPlanElements().add(3, continuation);

		muse(scenario).computeMuseForAllActs(plan);

		assertNull(getMUSE_h(stage));
		assertEquals((marginal(8, 8) + marginal(16, 14)) / 2, getMUSE_h(plan), TOLERANCE);
	}

	/** Verifies that computation uses the injected factory and passes the relevant person to it. */
	@Test
	void usesTheInjectedPersonSpecificScoringFactory() {
		var scenario = scenario();
		var first = person(scenario, "first", 1000, false);
		var second = person(scenario, "second", 3000, false);
		// This deliberately simple scorer has utility -rate * start_time_hours. A one-second
		// earlier start must therefore produce MUSE = rate. Rates are selected by person ID,
		// not by income: this test isolates factory injection from monetary normalization.
		ScoringFunctionFactory factory = person -> {
			double rate = person.getId().equals(first.getId()) ? 12 : 24;
			SumScoringFunction scoring = new SumScoringFunction();
			scoring.addScoringFunction(new SumScoringFunction.ActivityScoring() {
				private double score;

				// The first activity has no defined start, so it contributes nothing to this test scorer.
				@Override public void handleFirstActivity(Activity activity) { }
				@Override public void handleActivity(Activity activity) { score -= activity.getStartTime().seconds() * rate / HOUR; }
				@Override public void handleLastActivity(Activity activity) { handleActivity(activity); }
				// Scores are available as soon as activities are handled. No finalization is needed
				// for this test scorer's linear utility.
				@Override public void finish() { }
				@Override public double getScore() { return score; }
			});
			return scoring;
		};
		MuseComputation computation = muse(scenario, factory);

		computation.computeMuseForAllActs(first.getSelectedPlan());
		computation.computeMuseForAllActs(second.getSelectedPlan());

		assertEquals(12, getMUSE_h(first.getSelectedPlan()), TOLERANCE);
		assertEquals(24, getMUSE_h(second.getSelectedPlan()), TOLERANCE);
	}

	/** Records the existing no-trip result so any later replacement of NaN is deliberate. */
	@Test
	void currentlyStoresNaNForAPlanWithoutDestinations() {
		var scenario = scenario();
		var plan = PopulationUtils.createPlan();
		var person = scenario.getPopulation().getFactory().createPerson(org.matsim.api.core.v01.Id.createPersonId("at_home"));
		person.addPlan(plan);
		activity(plan, "home", Double.NaN, Double.NaN);

		muse(scenario).computeMuseForAllActs(plan);

		// No destination is evaluated, so the current mean calculation divides zero by zero.
		// This captures observed behavior; it does not endorse NaN as the future reporting policy.
		assertTrue(Double.isNaN(getMUSE_h(plan)));
	}

	/** Keeps KN's two cumulative scorers, with chronological callbacks and one final finish. */
	@Test
	void usesTwoScorersInPlanOrderAndFinishesAtTheEnd() {
		var scenario = scenario();
		var person = person(scenario, "person", 2000, true);
		List<List<String>> sequences = new ArrayList<>();
		List<List<Double>> destinationStarts = new ArrayList<>();
		ScoringFunctionFactory factory = ignored -> {
			List<String> sequence = new ArrayList<>();
			List<Double> starts = new ArrayList<>();
			sequences.add(sequence);
			destinationStarts.add(starts);
			SumScoringFunction scoring = new SumScoringFunction();
			scoring.addScoringFunction(new SumScoringFunction.ActivityScoring() {
				private double score;

				@Override public void handleFirstActivity(Activity activity) {
					sequence.add("first:" + activity.getType());
				}
				@Override public void handleActivity(Activity activity) {
					sequence.add(activity.getType());
					starts.add(activity.getStartTime().seconds());
					score -= activity.getStartTime().seconds() * 12 / HOUR;
				}
				@Override public void handleLastActivity(Activity activity) { handleActivity(activity); }
				@Override public void finish() { sequence.add("finish"); }
				@Override public double getScore() { return score; }
			});
			return scoring;
		};

		muse(scenario, factory).computeMuseForAllActs(person.getSelectedPlan());

		// Deliver morning home before work so scorers can align callbacks with the selected plan.
		// Finish only after evening home: finishing earlier would finalize an incomplete day.
		assertEquals(2, sequences.size());
		sequences.forEach(sequence -> assertEquals(
			List.of("first:home", "work", "shop", "home", "finish"), sequence));
		assertEquals(List.of(9 * HOUR, 18 * HOUR, 20 * HOUR), destinationStarts.get(0));
		assertEquals(List.of(9 * HOUR - 1, 18 * HOUR - 1, 20 * HOUR - 1), destinationStarts.get(1));
		assertEquals(12, getMUSE_h(person.getSelectedPlan()), TOLERANCE);
	}

	@Test
	void acceptsAnExperiencedPlanWithoutChangingItsPersonBackpointer() {
		var scenario = scenario();
		var person = person(scenario, "person", 2000, false);
		var selected = person.getSelectedPlan();
		var experienced = PopulationUtils.createPlan();
		PopulationUtils.copyFromTo(selected, experienced, true);

		muse(scenario).computeMuseForAllActs(person, experienced);

		assertNull(experienced.getPerson());
		assertSame(selected, person.getSelectedPlan());
		assertEquals((marginal(8, 8) + marginal(16, 14)) / 2, getMUSE_h(experienced), TOLERANCE);
	}

	/** Reproduces Berlin's person-based duration scoring on a detached experienced plan. */
	@ParameterizedTest
	@CsvSource({"home, 16, 14", "work, 2, 6"})
	void usesPersonTypicalDurationsInSelectedPlanOrder(String lastType, double lastTypicalHours, double lastDurationHours) {
		var scenario = scenario();
		scenario.getConfig().scoring().setTypicalDurationSource(ScoringConfigGroup.TypicalDurationSource.personAttribute);
		var person = person(scenario, "person", 2000, false);
		var selected = person.getSelectedPlan();
		TripStructureUtils.getActivities(selected, ExcludeStageActivities).getLast().setType(lastType);
		// The scorer looks these up by main-activity index, including morning home. Config still
		// says 8 hours for work; the person's 4-hour value must be used for both score variants.
		// When the final activity is also work, its separate 2-hour typical duration checks that
		// values are resolved by position, not just by activity type. There is no evening home.
		PersonUtils.setTypicalDurations(person, List.of(
			new PersonUtils.TypicalDuration("home", 16 * HOUR),
			new PersonUtils.TypicalDuration("work", 4 * HOUR),
			new PersonUtils.TypicalDuration(lastType, lastTypicalHours * HOUR)));
		var experienced = PopulationUtils.createPlan();
		PopulationUtils.copyFromTo(selected, experienced, true);
		var activities = TripStructureUtils.getActivities(experienced, ExcludeStageActivities);

		// Use the real scoring factory, as the controller does. Sending work before morning home
		// used to throw because work could not be matched to entry zero of typicalDurations.
		muse(scenario).computeMuseForAllActs(person, experienced);

		double lastMuse = marginal(lastTypicalHours, lastDurationHours);
		assertEquals(marginal(4, 8), getMUSE_h(activities.get(1)), TOLERANCE);
		assertEquals(lastMuse, getMUSE_h(activities.getLast()), TOLERANCE);
		assertEquals((marginal(4, 8) + lastMuse) / 2, getMUSE_h(experienced), TOLERANCE);
		assertNull(getMUSE_h(activities.getFirst()));
		assertNull(getMUSE_h(selected));
		assertSame(selected, person.getSelectedPlan());
		assertNull(experienced.getPerson());
	}

	/** Activity copies must retain attributes used by the scenario's activity scorer. */
	@Test
	void preservesAttributesUsedByCustomActivityScoring() {
		var scenario = scenario();
		var person = person(scenario, "person", 2000, false);
		var activities = TripStructureUtils.getActivities(person.getSelectedPlan(), ExcludeStageActivities);
		activities.get(1).getAttributes().putAttribute(ActivityAttributeTypicalDurationCalculator.TYPICAL_DURATION_ATTRIBUTE, 4 * HOUR);
		var parameters = new SubpopulationScoringParameters(scenario);
		ScoringFunctionFactory factory = agent -> {
			SumScoringFunction scoring = new SumScoringFunction();
			// Read the handed activities directly rather than their selected-plan counterparts.
			// This ensures the earlier-start copy itself retains the typical-duration attribute.
			scoring.addScoringFunction(new CharyparNagelActivityScoring(parameters.getScoringParameters(agent),
				new ActivityAttributeTypicalDurationCalculator(), null));
			return scoring;
		};

		muse(scenario, factory).computeMuseForAllActs(person.getSelectedPlan());

		// Config says 8 hours, but this scorer uses the activity's 4-hour typical duration.
		assertEquals(marginal(4, 8), getMUSE_h(activities.get(1)), TOLERANCE);
		assertEquals(marginal(16, 14), getMUSE_h(activities.getLast()), TOLERANCE);
	}
}
