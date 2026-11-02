package org.matsim.contrib.msp.capability.structure;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.core.population.PopulationUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests how the day of a shrunk plan is split into the stretches a discretionary activity could go into, and
 * in particular that home does not constrain them.
 */
public class FreePeriodTest {

	private final CapabilityConfigGroup config = new CapabilityConfigGroup();

	@Test
	void homeDoesNotConstrainTheDay() {

		// the plain commuter: home -> work -> home
		Plan plan = PopulationUtils.createPlan();
		activity(plan, "home", -1, 8 * 3600);
		activity(plan, "work", 8.5 * 3600, 17 * 3600);
		activity(plan, "home", 17.5 * 3600, -1);

		List<FreePeriod> periods = FreePeriod.of(plan, this.config);

		assertEquals(2, periods.size(), "before and after work");

		// the morning runs from the start of the day, not from when home happens to end at 08:00
		assertEquals(0, periods.get(0).availableFrom());
		assertEquals(8.5 * 3600, periods.get(0).deadline(), "work is the binding constraint");

		// the evening runs to the end of the day, not to when home happens to start at 17:30. This is
		// where a commuter's shopping flexibility actually is.
		assertEquals(17 * 3600, periods.get(1).availableFrom());
		assertEquals(FreePeriod.END_OF_DAY, periods.get(1).deadline());
	}

	@Test
	void someoneWhoStaysHomeHasTheWholeDay() {

		Plan plan = PopulationUtils.createPlan();
		activity(plan, "home", -1, -1);

		List<FreePeriod> periods = FreePeriod.of(plan, this.config);

		// a single activity means no gap between two activities, so without this case the person who has
		// the most freedom of all would drop out of the analysis entirely
		assertEquals(1, periods.size());
		assertEquals(0, periods.get(0).availableFrom());
		assertEquals(FreePeriod.END_OF_DAY, periods.get(0).deadline());
	}

	@Test
	void severalHomeActivitiesStillGiveOnlyOneFreePeriod() {

		// home -> home -> home, i.e. someone who only ever went out for discretionary activities
		Plan plan = PopulationUtils.createPlan();
		activity(plan, "home", -1, 10 * 3600);
		activity(plan, "home", 12 * 3600, 15 * 3600);
		activity(plan, "home", 17 * 3600, -1);

		List<FreePeriod> periods = FreePeriod.of(plan, this.config);

		// nothing pins this day down, so there is one free period. Counting the two gaps separately
		// would add up the same free time twice.
		assertEquals(1, periods.size());
		assertEquals(0, periods.get(0).availableFrom());
		assertEquals(FreePeriod.END_OF_DAY, periods.get(0).deadline());
	}

	@Test
	void everyFixedActivityDelimitsAPeriod() {

		// home -> work -> home -> work -> home, a split shift
		Plan plan = PopulationUtils.createPlan();
		activity(plan, "home", -1, 6 * 3600);
		activity(plan, "work", 6.5 * 3600, 10 * 3600);
		activity(plan, "home", 10.5 * 3600, 15 * 3600);
		activity(plan, "work", 15.5 * 3600, 20 * 3600);
		activity(plan, "home", 20.5 * 3600, -1);

		List<FreePeriod> periods = FreePeriod.of(plan, this.config);

		assertEquals(3, periods.size());
		assertEquals(0, periods.get(0).availableFrom());
		assertEquals(6.5 * 3600, periods.get(0).deadline());

		// between the two shifts, bounded by the work episodes rather than by the home stay in between
		assertEquals(10 * 3600, periods.get(1).availableFrom());
		assertEquals(15.5 * 3600, periods.get(1).deadline());

		assertEquals(20 * 3600, periods.get(2).availableFrom());
		assertEquals(FreePeriod.END_OF_DAY, periods.get(2).deadline());
	}

	@Test
	void recognisesActivitiesPastTheEndOfTheDay() {

		Plan ok = PopulationUtils.createPlan();
		activity(ok, "home", -1, 8 * 3600);
		activity(ok, "work", 8.5 * 3600, 17 * 3600);
		activity(ok, "home", 17.5 * 3600, -1);
		assertTrue(FreePeriod.isWithinDay(ok, this.config));

		// a night shift that ends after midnight cannot be analysed without wrap around
		Plan nightShift = PopulationUtils.createPlan();
		activity(nightShift, "home", -1, 20 * 3600);
		activity(nightShift, "work", 21 * 3600, 29 * 3600);
		activity(nightShift, "home", 29.5 * 3600, -1);
		assertTrue(!FreePeriod.isWithinDay(nightShift, this.config));

		// a home activity reaching past midnight is fine, home is elastic anyway
		Plan lateHome = PopulationUtils.createPlan();
		activity(lateHome, "home", -1, 8 * 3600);
		activity(lateHome, "work", 8.5 * 3600, 17 * 3600);
		activity(lateHome, "home", 17.5 * 3600, 30 * 3600);
		assertTrue(FreePeriod.isWithinDay(lateHome, this.config));
	}

	@Test
	void subTypesOfHomeCountAsHome() {
		Plan plan = PopulationUtils.createPlan();
		activity(plan, "home_28200", -1, 8 * 3600);
		activity(plan, "work_28800", 8.5 * 3600, 17 * 3600);
		activity(plan, "home_49200", 17.5 * 3600, -1);

		assertEquals(2, FreePeriod.of(plan, this.config).size(), "home_28200 has to be recognised as home");
	}

	@Test
	void refusesToGuessWhenAFixedActivityHasNoTimes() {

		// a fixed activity whose start time could not be reconstructed would silently collapse the period
		// before it to nothing, which reads as "no capability" rather than "cannot tell"
		Plan unusable = PopulationUtils.createPlan();
		activity(unusable, "home", -1, 8 * 3600);
		activity(unusable, "work", -1, -1);
		activity(unusable, "home", -1, -1);

		assertTrue(!FreePeriod.hasUsableTimes(unusable, this.config),
			"such a plan has to be left out of the analysis rather than scored");

		Plan usable = PopulationUtils.createPlan();
		activity(usable, "home", -1, 8 * 3600);
		activity(usable, "work", 8.5 * 3600, 17 * 3600);
		activity(usable, "home", 17.5 * 3600, -1);

		assertTrue(FreePeriod.hasUsableTimes(usable, this.config));
	}

	@Test
	void aFixedActivityWithAMaximumDurationIsUsable() {

		// the equil example gives its work activities a maximum duration rather than an end time
		Plan plan = PopulationUtils.createPlan();
		activity(plan, "home", -1, 8 * 3600);
		Activity work = PopulationUtils.createActivityFromCoord("work", new Coord(0, 0));
		work.setStartTime(8.5 * 3600);
		work.setMaximumDuration(8 * 3600);
		plan.addActivity(work);
		activity(plan, "home", -1, -1);

		assertTrue(FreePeriod.hasUsableTimes(plan, this.config));

		List<FreePeriod> periods = FreePeriod.of(plan, this.config);
		assertEquals(2, periods.size());
		assertEquals(8.5 * 3600 + 8 * 3600, periods.get(1).availableFrom(),
			"the agent leaves work when its maximum duration is up");
	}

	@Test
	void backToBackFixedActivitiesLeaveNoRoom() {

		// work ends exactly when the next one starts: a perfectly ordinary plan, and simply no period
		Plan plan = PopulationUtils.createPlan();
		activity(plan, "home", -1, 8 * 3600);
		activity(plan, "work", 8 * 3600, 12 * 3600);
		activity(plan, "educ", 12 * 3600, 17 * 3600);
		activity(plan, "home", 17.5 * 3600, -1);

		List<FreePeriod> periods = FreePeriod.of(plan, this.config);

		// [0, 08:00] and [17:00, 24:00] remain; the zero-width one between work and educ is dropped
		assertEquals(2, periods.size());
		assertEquals(0, periods.get(0).availableFrom());
		assertEquals(17 * 3600, periods.get(1).availableFrom());
	}

	private static void activity(Plan plan, String type, double startTime, double endTime) {
		Activity activity = PopulationUtils.createActivityFromCoord(type, new Coord(0, 0));
		if (startTime >= 0) {
			activity.setStartTime(startTime);
		}
		if (endTime >= 0) {
			activity.setEndTime(endTime);
		}
		plan.addActivity(activity);
	}
}
