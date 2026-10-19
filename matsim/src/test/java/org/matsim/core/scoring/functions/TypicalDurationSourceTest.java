package org.matsim.core.scoring.functions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup.TypicalDurationSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests that {@link org.matsim.core.config.groups.ScoringConfigGroup#getTypicalDurationSource()} selects the
 * {@link TypicalDurationCalculator} of the default scoring, and that code can bind its own.
 */
public class TypicalDurationSourceTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private static Config equil() {
		return ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
	}

	private static com.google.inject.Injector injector(Scenario scenario, AbstractModule... overrides) {
		AbstractModule module = new CharyparNagelScoringFunctionModule();
		for (AbstractModule override : overrides) {
			module = AbstractModule.override(List.of(module), override);
		}
		return Injector.createInjector(scenario.getConfig(), new ScenarioByInstanceModule(scenario), module);
	}

	@Test
	void configSelectsTheCalculator() {
		Map<TypicalDurationSource, Class<? extends TypicalDurationCalculator>> expected = Map.of(
			TypicalDurationSource.activityParams, ActivityTypeTypicalDurationCalculator.class,
			TypicalDurationSource.activityAttribute, ActivityAttributeTypicalDurationCalculator.class,
			TypicalDurationSource.personAttribute, PersonAttributeTypicalDurationCalculator.class);
		assertThat(ConfigUtils.createConfig().scoring().getTypicalDurationSource()).isEqualTo(TypicalDurationSource.activityParams);
		expected.forEach((source, calculatorClass) -> {
			Config config = ConfigUtils.createConfig();
			config.scoring().setTypicalDurationSource(source);
			Scenario scenario = ScenarioUtils.createScenario(config);
			assertThat(injector(scenario).getInstance(TypicalDurationCalculator.class)).isInstanceOf(calculatorClass);
			assertThat(TypicalDurationCalculator.of(source)).isInstanceOf(calculatorClass);
		});
	}

	/**
	 * Both the injected and the directly constructed default scoring function factory use the calculator; a binding
	 * replaces the one from the config.
	 */
	@Test
	void factoriesUseTheCalculator() {
		Config config = equil();
		config.scoring().setTypicalDurationSource(TypicalDurationSource.personAttribute);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Person person = scenario.getPopulation().getPersons().values().iterator().next();

		AtomicInteger calls = new AtomicInteger();
		TypicalDurationCalculator counting = (p, act, mainActivityIndex) -> {
			calls.incrementAndGet();
			return OptionalTime.undefined();
		};
		ScoringFunctionFactory injected = injector(scenario, new AbstractModule() {
			@Override
			public void install() {
				bind(TypicalDurationCalculator.class).toInstance(counting);
			}
		}).getInstance(ScoringFunctionFactory.class);
		scoreSelectedPlan(injected, person);
		// all main activities, but the first and the last one (both home) are scored as one
		int mainActivities = TripStructureUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities).size();
		assertThat(calls.get()).isEqualTo(mainActivities - 1);

		// the directly constructed factory takes the calculator from the config, which here makes a wrong attribute abort
		PersonUtils.setTypicalDurations(person, List.of(new PersonUtils.TypicalDuration("w", 3600.)));
		assertThatThrownBy(() -> scoreSelectedPlan(new CharyparNagelScoringFunctionFactory(scenario), person))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("does not fit main activity 1 (type w)");
	}

	/** Scores the person's selected plan as if executed as planned, with the plan's activities handed over bare. */
	private static void scoreSelectedPlan(ScoringFunctionFactory factory, Person person) {
		ScoringFunction scoringFunction = factory.createNewScoringFunction(person);
		double time = 0;
		for (Activity planned : TripStructureUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities)) {
			Activity executed = PopulationUtils.createActivityFromLinkId(planned.getType(), planned.getLinkId());
			if (time > 0) {
				executed.setStartTime(time);
			}
			time = planned.getEndTime().orElse(time + planned.getMaximumDuration().orElse(3600.));
			if (planned.getEndTime().isDefined() || planned.getMaximumDuration().isDefined()) {
				executed.setEndTime(time);
			}
			scoringFunction.handleActivity(executed);
		}
		scoringFunction.finish();
	}

	/**
	 * In a run, the person attribute gives the same executed scores as activity attributes with the same values, and
	 * different ones than the activity types' typical durations.
	 */
	@Test
	void personAttributeScoresLikeActivityAttributesInARun() {
		Map<Id<Person>, Double> activityParams = run(TypicalDurationSource.activityParams, false);
		Map<Id<Person>, Double> activityAttribute = run(TypicalDurationSource.activityAttribute, false);
		Map<Id<Person>, Double> personAttribute = run(TypicalDurationSource.personAttribute, false);

		assertThat(personAttribute).hasSameSizeAs(activityAttribute);
		personAttribute.forEach((personId, score) -> assertThat(score).isCloseTo(activityAttribute.get(personId), within(1e-9)));
		personAttribute.forEach((personId, score) -> assertThat(score).isNotCloseTo(activityParams.get(personId), within(1e-3)));
	}

	@Test
	void personAttributeThatDoesNotFitAbortsAtStartup() {
		assertThatThrownBy(() -> run(TypicalDurationSource.personAttribute, true))
			.hasStackTraceContaining("does not fit main activity 1 (type w)");
	}

	/**
	 * Runs equil for 0 iterations, with the persons' typical durations shorter than the config ones (h 10h instead of
	 * 12h, w 6h instead of 8h), and returns the executed scores.
	 */
	private Map<Id<Person>, Double> run(TypicalDurationSource source, boolean garbled) {
		Config config = equil();
		config.controller().setOutputDirectory(utils.getOutputDirectory() + source + "/");
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setDumpDataAtEnd(false);
		config.scoring().setTypicalDurationSource(source);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Map<String, Double> typicalDurations = Map.of("h", 10 * 3600., "w", 6 * 3600.);
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (Activity act : TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities)) {
					act.getAttributes().putAttribute(ActivityAttributeTypicalDurationCalculator.TYPICAL_DURATION_ATTRIBUTE, typicalDurations.get(act.getType()));
				}
			}
			List<PersonUtils.TypicalDuration> entries = new ArrayList<>();
			for (Activity act : TripStructureUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities)) {
				entries.add(new PersonUtils.TypicalDuration(garbled ? "h" : act.getType(), typicalDurations.get(act.getType())));
			}
			PersonUtils.setTypicalDurations(person, entries);
		}

		new Controler(scenario).run();

		Map<Id<Person>, Double> scores = new HashMap<>();
		scenario.getPopulation().getPersons().values().forEach(person -> scores.put(person.getId(), person.getSelectedPlan().getScore()));
		return scores;
	}
}
