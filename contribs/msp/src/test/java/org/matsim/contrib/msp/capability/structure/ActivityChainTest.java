package org.matsim.contrib.msp.capability.structure;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.core.population.PopulationUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests how an activity chain is written out: the normalisation of the types, and the two spellings
 * the fundamental-plan table puts side by side.
 */
public class ActivityChainTest {

	@Test
	void writesTheChainOfNames() {
		Plan plan = plan("home_morning", "work_7200", "leisure", "home_28200");
		assertEquals("home-work-leisure-home", ActivityChain.of(activities(plan), config()));
	}

	@Test
	void writesTheChainOfClasses() {
		Plan plan = plan("home_morning", "work_7200", "leisure", "home_28200");
		assertEquals("home-mandatory-discretionary-home", ActivityChain.classified(activities(plan), config()));
	}

	@Test
	void stripsTheTypicalDuration() {
		assertEquals("work", ActivityChain.normalize("work_7200", config()));
		assertEquals("shop", ActivityChain.normalize("shop", config()));
		// only a trailing _<digits> is a typical duration, so a sub-type survives
		assertEquals("educ_primary", ActivityChain.normalize("educ_primary", config()));
	}

	@Test
	void collapsesEverySubTypeOfHome() {
		CapabilityConfigGroup config = config();
		assertEquals("home", ActivityChain.normalize("home", config));
		assertEquals("home", ActivityChain.normalize("home_morning", config));
		assertEquals("home", ActivityChain.normalize("home_evening", config));
		assertEquals("home", ActivityChain.normalize("home_28200", config));
		assertEquals("home", ActivityChain.normalize("home_morning_3600", config));
	}

	@Test
	void followsTheConfiguredHomeActivity() {
		// equil calls home 'h'
		CapabilityConfigGroup config = new CapabilityConfigGroup();
		config.setMandatoryActivities(Set.of("h", "w"));
		config.setHomeActivity("h");

		Plan plan = plan("h", "w", "shop", "h");
		assertEquals("h-w-shop-h", ActivityChain.of(activities(plan), config));
		assertEquals("h-mandatory-discretionary-h", ActivityChain.classified(activities(plan), config));
	}

	@Test
	void anEmptyChainIsAnEmptyString() {
		assertEquals("", ActivityChain.of(List.of(), config()));
		assertEquals("", ActivityChain.classified(List.of(), config()));
	}

	@Test
	void everyNonHomeMandatoryTypeIsCalledMandatory() {
		CapabilityConfigGroup config = config();
		// the default mandatory types, and their sub-types
		assertEquals("mandatory", ActivityChain.classify("work", config));
		assertEquals("mandatory", ActivityChain.classify("educ_primary", config));
		assertEquals("mandatory", ActivityChain.classify("errands", config));
		assertEquals("mandatory", ActivityChain.classify("accomp_3600", config));
		// home keeps its own name, because it is the elastic one
		assertEquals("home", ActivityChain.classify("home_morning", config));
		// everything else
		assertEquals("discretionary", ActivityChain.classify("shop_other", config));
		assertEquals("discretionary", ActivityChain.classify("leisure", config));
	}

	@Test
	void mergesConsecutiveHomeStays() {

		// what a fundamental plan looks like once the shop between the two home stays is dropped
		Plan plan = plan("home_morning", "home_evening");
		List<Activity> merged = ActivityChain.mergeConsecutiveHome(activities(plan), config());

		assertEquals(1, merged.size(), "the person never left, so it is one home stay");
		assertEquals("home", ActivityChain.of(merged, config()));
		assertEquals("home", ActivityChain.classified(merged, config()));
	}

	@Test
	void mergesEveryRunOfHomeButKeepsWhatSeparatesThem() {

		// home - home - work - home - home  ->  home - work - home
		Plan plan = plan("home_morning", "home", "work_7200", "home", "home_evening");
		List<Activity> merged = ActivityChain.mergeConsecutiveHome(activities(plan), config());

		assertEquals("home-work-home", ActivityChain.of(merged, config()));
		assertEquals("home-mandatory-home", ActivityChain.classified(merged, config()));
	}

	@Test
	void leavesAChainWithoutAdjacentHomeAlone() {

		Plan plan = plan("home", "work", "shop", "home");
		List<Activity> merged = ActivityChain.mergeConsecutiveHome(activities(plan), config());

		assertEquals(4, merged.size(), "the home stays are not adjacent, so there is nothing to merge");
		assertEquals("home-work-shop-home", ActivityChain.of(merged, config()));
	}

	@Test
	void mergingDoesNotTouchTheGivenList() {

		Plan plan = plan("home", "home");
		List<Activity> activities = activities(plan);

		ActivityChain.mergeConsecutiveHome(activities, config());

		assertEquals(2, activities.size(), "the caller's list is left as it is");
	}

	@Test
	void mergingAnEmptyOrSingleChainIsHarmless() {
		assertEquals(List.of(), ActivityChain.mergeConsecutiveHome(List.of(), config()));
		assertEquals("home", ActivityChain.of(
			ActivityChain.mergeConsecutiveHome(activities(plan("home_28200")), config()), config()));
	}

	private static CapabilityConfigGroup config() {
		return new CapabilityConfigGroup();
	}

	private static Plan plan(String... types) {
		Plan plan = PopulationUtils.createPlan();
		for (String type : types) {
			plan.addActivity(PopulationUtils.createActivityFromCoord(type, new Coord(0, 0)));
		}
		return plan;
	}

	private static List<Activity> activities(Plan plan) {
		return plan.getPlanElements().stream()
			.filter(Activity.class::isInstance)
			.map(Activity.class::cast)
			.toList();
	}
}
