package org.matsim.contrib.msp.capability.structure;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.core.population.PopulationUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the blocks of the time axis: how home activities are merged and where their soft boundaries end
 * up.
 */
public class CapabilityTimelineTest {

	private final CapabilityConfigGroup config = new CapabilityConfigGroup();

	@Test
	void mergesConsecutiveHomeActivitiesIntoOneBlock() {

		// someone who only ever went out for discretionary activities: three home activities in the plan, but
		// one single stay at home as far as this analysis is concerned
		Plan plan = PopulationUtils.createPlan();
		activity(plan, "home_morning", -1, 10 * 3600);
		activity(plan, "home", 12 * 3600, 15 * 3600);
		activity(plan, "home_evening", 17 * 3600, -1);

		List<CapabilityTimeline.Block> blocks = CapabilityTimeline.of(plan, this.config);

		assertEquals(1, blocks.size(), "three home activities, one bar");
		CapabilityTimeline.Block home = blocks.get(0);
		assertTrue(home.home());
		assertEquals(0, home.from());
		assertEquals(FreePeriod.END_OF_DAY, home.to());

		// certainly home before 06:00 and after 22:00, uncertain in between
		assertEquals(6 * 3600, home.softStart());
		assertEquals(22 * 3600, home.softEnd());
		assertTrue(home.isSoft());
	}

	@Test
	void homeIsSoftTowardsTheFixedActivityAroundIt() {

		Plan plan = PopulationUtils.createPlan();
		activity(plan, "home_morning", -1, 8 * 3600);
		activity(plan, "work", 8.5 * 3600, 17 * 3600);
		activity(plan, "home_evening", 17.5 * 3600, -1);

		List<CapabilityTimeline.Block> blocks = CapabilityTimeline.of(plan, this.config);
		assertEquals(3, blocks.size());

		// the morning block runs to when work starts, not to when home happens to end
		CapabilityTimeline.Block morning = blocks.get(0);
		assertEquals(0, morning.from());
		assertEquals(8.5 * 3600, morning.to());
		assertEquals(6 * 3600, morning.softStart(), "certainly home until 06:00");
		assertEquals(8.5 * 3600, morning.softEnd(), "uncertain right up to the start of work");

		// the fixed activity has nothing uncertain about it
		CapabilityTimeline.Block work = blocks.get(1);
		assertFalse(work.home());
		assertFalse(work.isSoft());
		assertEquals(8.5 * 3600, work.from());
		assertEquals(17 * 3600, work.to());

		// the evening block starts when work ends and is uncertain from that moment on
		CapabilityTimeline.Block evening = blocks.get(2);
		assertEquals(17 * 3600, evening.from());
		assertEquals(FreePeriod.END_OF_DAY, evening.to());
		assertEquals(17 * 3600, evening.softStart(), "the agent may not have gone straight home");
		assertEquals(22 * 3600, evening.softEnd(), "certainly home again from 22:00");
	}

	@Test
	void theSoftBoundaryShiftsWhenAFixedActivityIsEarly() {

		// an early shift: work starts at 05:00, before the 06:00 the boundary would normally sit at
		Plan plan = PopulationUtils.createPlan();
		activity(plan, "home_morning", -1, 4.5 * 3600);
		activity(plan, "work", 5 * 3600, 13 * 3600);
		activity(plan, "home_evening", 13.5 * 3600, -1);

		CapabilityTimeline.Block morning = CapabilityTimeline.of(plan, this.config).get(0);

		assertEquals(5 * 3600, morning.to());
		// pushed back far enough to leave room for a gradient, rather than collapsing into a hard edge
		assertEquals(5 * 3600 - CapabilityTimeline.MIN_RAMP, morning.softStart());
		assertTrue(morning.isSoft());
	}

	@Test
	void theSoftBoundaryShiftsWhenAFixedActivityIsLate() {

		Plan plan = PopulationUtils.createPlan();
		activity(plan, "home_morning", -1, 14 * 3600);
		activity(plan, "work", 15 * 3600, 23 * 3600);
		activity(plan, "home_evening", 23.5 * 3600, -1);

		List<CapabilityTimeline.Block> blocks = CapabilityTimeline.of(plan, this.config);
		CapabilityTimeline.Block evening = blocks.get(blocks.size() - 1);

		assertEquals(23 * 3600, evening.from());
		// 22:00 has passed already, so the gradient runs over the last hour of the day instead
		assertEquals(FreePeriod.END_OF_DAY, evening.softEnd());
		assertTrue(evening.isSoft());
	}

	@Test
	void aHomeStayBetweenTwoFixedActivitiesIsUncertainThroughout() {

		// a split shift: whether the agent went home in between is not determined at all
		Plan plan = PopulationUtils.createPlan();
		activity(plan, "home_morning", -1, 6 * 3600);
		activity(plan, "work", 6.5 * 3600, 10 * 3600);
		activity(plan, "home", 10.5 * 3600, 15 * 3600);
		activity(plan, "work", 15.5 * 3600, 20 * 3600);
		activity(plan, "home_evening", 20.5 * 3600, -1);

		List<CapabilityTimeline.Block> blocks = CapabilityTimeline.of(plan, this.config);
		assertEquals(5, blocks.size());

		CapabilityTimeline.Block middle = blocks.get(2);
		assertTrue(middle.home());
		assertEquals(10 * 3600, middle.from(), "from the end of the first shift");
		assertEquals(15.5 * 3600, middle.to(), "to the start of the second");
		assertEquals(middle.from(), middle.softStart());
		assertEquals(middle.to(), middle.softEnd());
		assertTrue(middle.isSoft(), "drawn entirely transparent: the agent may not have been home at all");
	}

	@Test
	void blocksCoverTheWholeDayWithoutGaps() {

		Plan plan = PopulationUtils.createPlan();
		activity(plan, "home_morning", -1, 8 * 3600);
		activity(plan, "work", 8.5 * 3600, 17 * 3600);
		activity(plan, "home_evening", 17.5 * 3600, -1);

		List<CapabilityTimeline.Block> blocks = CapabilityTimeline.of(plan, this.config);

		assertEquals(0, blocks.get(0).from());
		assertEquals(FreePeriod.END_OF_DAY, blocks.get(blocks.size() - 1).to());
		for (int i = 1; i < blocks.size(); i++) {
			assertEquals(blocks.get(i - 1).to(), blocks.get(i).from(),
				"the bars have to be drawn end to end");
		}
	}

	@Test
	void anEmptyPlanHasNoBlocks() {
		assertTrue(CapabilityTimeline.of(PopulationUtils.createPlan(), this.config).isEmpty());
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
