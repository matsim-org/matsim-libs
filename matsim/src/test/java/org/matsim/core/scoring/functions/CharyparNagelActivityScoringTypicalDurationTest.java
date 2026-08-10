package org.matsim.core.scoring.functions;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.population.PopulationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Tests for the per-activity typical duration ({@link TypicalDurationCalculator}) in
 * {@link CharyparNagelActivityScoring}.  During a simulation, the scoring is handed attribute-less activity
 * reconstructions (see {@link org.matsim.core.scoring.EventsToActivities}); these tests feed exactly such bare
 * activities and verify that the alignment with the selected plan supplies the attributes.
 */
public class CharyparNagelActivityScoringTypicalDurationTest {

	private static Config config() {
		Config config = ConfigUtils.createConfig();
		ScoringConfigGroup scoring = config.scoring();
		scoring.setPerforming_utils_hr(6.);
		scoring.setLateArrival_utils_hr(-18.);
		scoring.setEarlyDeparture_utils_hr(-6.);
		for (String type : new String[]{"home", "work", "home_evening"}) {
			scoring.addActivityParams(new ScoringConfigGroup.ActivityParams(type).setTypicalDuration(2. * 3600.));
		}
		ScoringConfigGroup.ActivityParams interaction = new ScoringConfigGroup.ActivityParams("pt interaction");
		interaction.setScoringThisActivityAtAll(false);
		scoring.addActivityParams(interaction);
		return config;
	}

	private static Person person(Plan plan) {
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("p"));
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		return person;
	}

	private static ScoringParameters params(Config config) {
		return new ScoringParameters.Builder(config.scoring(), config.scoring().getScoringParameters(null), config.scenario()).build();
	}

	private static CharyparNagelActivityScoring attributeScoring(Config config, Person person) {
		return new CharyparNagelActivityScoring(params(config), new ActivityAttributeTypicalDurationCalculator(), person);
	}

	/** The plan with attributes: home(t*=8h) - work(t*=8h) - home_evening (no attribute). */
	private static Plan plan() {
		return planWithWorkTypical(28800.);
	}

	private static Plan planWithWorkTypical(double workTypicalDuration) {
		Plan plan = PopulationUtils.createPlan();
		Activity home = PopulationUtils.createAndAddActivityFromCoord(plan, "home", new Coord(0., 0.));
		home.setEndTime(28800.);
		home.getAttributes().putAttribute(ActivityAttributeTypicalDurationCalculator.TYPICAL_DURATION_ATTRIBUTE, 28800.);
		PopulationUtils.createAndAddLeg(plan, "car");
		Activity work = PopulationUtils.createAndAddActivityFromCoord(plan, "work", new Coord(1., 0.));
		work.setEndTime(59400.);
		work.getAttributes().putAttribute(ActivityAttributeTypicalDurationCalculator.TYPICAL_DURATION_ATTRIBUTE, workTypicalDuration);
		PopulationUtils.createAndAddLeg(plan, "car");
		PopulationUtils.createAndAddActivityFromCoord(plan, "home_evening", new Coord(0., 0.));
		return plan;
	}

	/** Bare activity as EventsToActivities produces it: type and times only, no attributes. */
	private static Activity bare(String type, Double start, Double end) {
		Activity act = PopulationUtils.createActivityFromCoord(type, new Coord(0., 0.));
		if (start != null) act.setStartTime(start);
		if (end != null) act.setEndTime(end);
		return act;
	}

	private static double score(CharyparNagelActivityScoring scoring, double workStart, double workEnd) {
		scoring.handleFirstActivity(bare("home", null, 28800.));
		scoring.handleActivity(bare("work", workStart, workEnd));
		scoring.handleLastActivity(bare("home_evening", workEnd + 1800., null));
		scoring.finish();
		return scoring.getScore();
	}

	@Test
	void planAlignmentSuppliesTheAttributes() {
		Config config = config();
		// with the plan alignment, home and work are scored against their 8h attribute typical; without a person, the
		// bare activities fall back to the 2h config typical -- the scores must differ.
		double scoreWithPlan = score(attributeScoring(config, person(plan())), 30600., 59400.);
		double scoreWithoutPlan = score(attributeScoring(config, null), 30600., 59400.);
		assertNotEquals(scoreWithPlan, scoreWithoutPlan, 1.);

		// gold standard: handing in the attribute-carrying plan activities directly must give exactly the same result
		// as the alignment.
		CharyparNagelActivityScoring direct = attributeScoring(config, null);
		Plan p = plan();
		direct.handleFirstActivity((Activity) p.getPlanElements().get(0));
		Activity work = (Activity) p.getPlanElements().get(2);
		work.setStartTime(30600.);
		direct.handleActivity(work);
		Activity last = (Activity) p.getPlanElements().get(4);
		last.setStartTime(61200.);
		direct.handleLastActivity(last);
		direct.finish();
		assertEquals(direct.getScore(), scoreWithPlan, 1e-9);
	}

	/** Without attributes, the calculator variant must reproduce the stock scoring exactly. */
	@Test
	void withoutAttributesEqualsStockScoring() {
		Config config = config();
		Plan barePlan = plan();
		barePlan.getPlanElements().forEach(pe -> {
			if (pe instanceof Activity act) act.getAttributes().removeAttribute(ActivityAttributeTypicalDurationCalculator.TYPICAL_DURATION_ATTRIBUTE);
		});
		double calculatorVariant = score(attributeScoring(config, person(barePlan)), 30600., 59400.);
		double stock = score(new CharyparNagelActivityScoring(params(config)), 30600., 59400.);
		assertEquals(stock, calculatorVariant, 1e-9);
	}

	/**
	 * The essential consistency property: an activity carrying typical duration T must be scored exactly as if its
	 * activity type had typical duration T in the config, for both zero-utility-duration computation modes.
	 */
	@Test
	void attributeReproducesConfigTypicalDuration() {
		for (ScoringConfigGroup.TypicalDurationScoreComputation computation : ScoringConfigGroup.TypicalDurationScoreComputation.values()) {
			// A: work typical 2h in the config, but 8h as activity attribute (home/home_evening attribute-less in this plan)
			Config configA = config();
			configA.scoring().getActivityParams("work").setTypicalDurationScoreComputation(computation);
			Plan planA = planWithWorkTypical(28800.);
			((Activity) planA.getPlanElements().get(0)).getAttributes().removeAttribute(ActivityAttributeTypicalDurationCalculator.TYPICAL_DURATION_ATTRIBUTE);
			double withAttribute = score(attributeScoring(configA, person(planA)), 30600., 59400.);

			// B: work typical 8h in the config, stock scoring on the bare activities
			Config configB = config();
			configB.scoring().getActivityParams("work").setTypicalDurationScoreComputation(computation);
			configB.scoring().getActivityParams("work").setTypicalDuration(28800.);
			double fromConfig = score(new CharyparNagelActivityScoring(params(configB)), 30600., 59400.);

			assertEquals(fromConfig, withAttribute, 1e-9, "computation mode " + computation);
		}
	}

	/**
	 * Stage activities must not consume the plan alignment: the realized stream contains "pt interaction" activities
	 * whose count varies with routing, while the alignment covers main activities only.  Scores must equal the
	 * interaction-free day exactly (interactions have scoringThisActivityAtAll=false).
	 */
	@Test
	void stageActivitiesBypassThePlanAlignment() {
		Config config = config();
		CharyparNagelActivityScoring withStages = attributeScoring(config, person(plan()));
		withStages.handleFirstActivity(bare("home", null, 28800.));
		withStages.handleActivity(bare("pt interaction", 29000., 29000.));
		withStages.handleActivity(bare("pt interaction", 29800., 29800.));
		withStages.handleActivity(bare("work", 30600., 59400.));
		withStages.handleActivity(bare("pt interaction", 60000., 60000.));
		withStages.handleLastActivity(bare("home_evening", 61200., null));
		withStages.finish();

		double plain = score(attributeScoring(config, person(plan())), 30600., 59400.);
		assertEquals(plain, withStages.getScore(), 1e-9);
	}

	/**
	 * Scoring functions may be created at iteration start, BEFORE replanning: the plan selected at construction time
	 * is not necessarily the executed one.  The alignment must resolve the plan lazily -- here the person's selected
	 * plan is replaced after construction (with a different work typical duration standing in for a structurally
	 * different plan), and the scoring must use the replacement.
	 */
	@Test
	void planIsResolvedAtFirstCallbackNotAtConstruction() {
		Config config = config();
		Person person = person(plan());
		CharyparNagelActivityScoring scoring = attributeScoring(config, person);

		// "replanning": a new selected plan with a different work typical duration (4h instead of 8h)
		person.addPlan(planWithWorkTypical(14400.));
		person.setSelectedPlan(person.getPlans().get(1));

		double lazily = score(scoring, 30600., 59400.);
		double onReplanned = score(attributeScoring(config, person(planWithWorkTypical(14400.))), 30600., 59400.);
		double onStale = score(attributeScoring(config, person(plan())), 30600., 59400.);
		assertEquals(onReplanned, lazily, 1e-9);
		assertNotEquals(onStale, lazily, 1.);
	}

	/**
	 * When the handed activity sequence diverges from the selected plan (e.g. with within-day replanning), the
	 * alignment is abandoned with a warning and scoring falls back to the activity types' config parameters, i.e.
	 * behaves like the stock scoring.
	 */
	@Test
	void alignmentMismatchFallsBackToConfigParameters() {
		Config config = config();
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("unexpected").setTypicalDuration(2. * 3600.));

		CharyparNagelActivityScoring diverged = attributeScoring(config, person(plan()));
		diverged.handleFirstActivity(bare("unexpected", null, 28800.));
		diverged.handleActivity(bare("work", 30600., 59400.));
		diverged.handleLastActivity(bare("home_evening", 61200., null));
		diverged.finish();

		CharyparNagelActivityScoring stock = new CharyparNagelActivityScoring(params(config));
		stock.handleFirstActivity(bare("unexpected", null, 28800.));
		stock.handleActivity(bare("work", 30600., 59400.));
		stock.handleLastActivity(bare("home_evening", 61200., null));
		stock.finish();

		assertEquals(stock.getScore(), diverged.getScore(), 1e-9);
	}
}
