package org.matsim.application.prepare.population;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.Time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Data-driven test for {@link SplitActivityTypesDuration}.
 * <p>
 * Each case below is a literal input plan plus the expected effect of the transformation, i.e. which duration tag
 * gets appended to each activity type, and whether the activity ends up encoded as a typical {@code maximumDuration}
 * (with its end time removed) or keeps its end time.
 * <p>
 * All cases run with the production default parameters:
 * <ul>
 *     <li>bin size = 600s (10 min): durations are rounded to the nearest multiple of 600 and used as the type suffix,</li>
 *     <li>max typical duration = 86400s (24h): the largest possible bin, and the fallback "end time" for the last
 *         activity of a plan when it has no end time,</li>
 *     <li>end-time-to-duration = 1800s (30 min): activities not longer than this get their end time replaced by a
 *         fixed {@code maximumDuration}.</li>
 * </ul>
 */
public class SplitActivityTypesDurationTest {

	private static final int BIN_SIZE = 600;
	private static final int MAX_TYPICAL_DURATION = 86400;
	private static final int END_TIME_TO_DURATION = 1800;

	private static Stream<Case> cases() {
		return Stream.of(

			// Duration is derived from start/end times. The first activity has no start time (assumed to start at
			// midnight, i.e. 0); the last activity has no end time (assumed to end at max typical duration, 24h).
			// As in every case here, the last activity of the plan carries only a start time - the realistic shape
			// for the final (overnight) activity of a day.
			new Case("duration-from-start-and-end-times")
				.in(act("home").end("07:00:00"))          // 0 .. 25200  -> 25200 (=42*600)
				.in(act("work").start("08:00:00").end("17:00:00")) // 28800 .. 61200 -> 32400 (=54*600)
				.in(act("leisure").start("18:00:00"))     // last, start only: 64800 .. 86400 -> 21600 (=36*600)
				.expect(out("home_25200").end("07:00:00"))
				.expect(out("work_32400").end("17:00:00"))
				.expect(out("leisure_21600")),

			// Rounding: the suffix is the duration rounded to the nearest 600s bin. The first three activities use
			// maximumDuration as the input, which isolates the rounding from the end-time-to-duration rewrite below
			// (and is left untouched - only the type suffix is rounded). The last activity carries only a start time,
			// so its duration is 86400 - start.
			new Case("rounding-to-nearest-bin")
				.in(act("a").maxDuration(100))            // 100/600 = 0.17 -> floored up to the smallest bin, 600
				.in(act("b").maxDuration(899))            // 899/600 = 1.50- -> rounds down to 600
				.in(act("c").maxDuration(100000))         // 166.7 -> capped at the largest bin, 86400 (=144*600)
				.in(act("d").start("23:45:00"))           // last, start only: 86400-85500 = 900 -> rounds up to 1200
				.expect(out("a_600").maxDuration(100))
				.expect(out("b_600").maxDuration(899))
				.expect(out("c_86400").maxDuration(100000))
				.expect(out("d_1200")),

			// Short activities (duration <= 1800s) that have an end time: the end time is removed and the *exact*
			// (un-rounded) duration is stored as maximumDuration, while the type suffix still uses the rounded value.
			new Case("short-activity-encoded-as-duration")
				.in(act("home").end("08:00:00"))          // 28800, long -> keeps its end time
				.in(act("shopping").start("08:10:00").end("08:26:40")) // 1000s, short -> rounded type 1200, maxDur 1000
				.in(act("work").start("08:40:00"))        // last, start only -> 86400-31200 = 55200
				.expect(out("home_28800").end("08:00:00"))
				.expect(out("shopping_1200").maxDuration(1000))
				.expect(out("work_55200")),

			// maximumDuration takes precedence over start/end times when computing the duration, and a long
			// activity keeps its end time even if a maximumDuration was given. (Shown on the middle "work" activity;
			// the last activity carries only a start time.)
			new Case("maximum-duration-takes-precedence")
				.in(act("home").end("08:00:00"))                                     // 28800
				.in(act("work").start("08:00:00").end("18:00:00").maxDuration(3600)) // uses 3600, not 36000
				.in(act("leisure").start("20:00:00"))                                // last, start only -> 86400-72000 = 14400
				.expect(out("home_28800").end("08:00:00"))
				.expect(out("work_3600").end("18:00:00").maxDuration(3600))
				.expect(out("leisure_14400")),

			// Overnight merge. The plan is interpreted as periodically repeated (period = 24h), so the morning
			// "home" (00:00 .. 06:00) and the evening "home" (18:30 .. midnight) are really *one* activity that
			// starts in the evening and ends the next morning. The merge models that: the evening portion
			// (86400 - 18:30 = 19800) and the morning portion (06:00 = 21600) are summed into the single
			// overnight duration 41400 (= 11h30m), which is written to *both* home activities.
			new Case("overnight-activities-are-merged")
				.in(act("home").end("06:00:00"))          // morning portion: 21600
				.in(act("work").start("06:30:00").end("18:00:00")) // 41400
				.in(act("home").start("18:30:00"))        // evening portion: 86400-66600 = 19800
				.expect(out("home_41400").end("06:00:00")) // 19800 + 21600 = 41400, the one overnight activity
				.expect(out("work_41400").end("18:00:00"))
				.expect(out("home_41400")),

			// --- Activities around the day boundary (midnight = 86400s) ---------------------------------------
			// The last activity of a plan only ever has a start time. Under the periodic interpretation it is the
			// evening half of an overnight activity that continues into the next day's first activity (see the
			// merge case above). The following cases probe a last activity that starts before or after midnight,
			// with and without a merge partner of the same base type "home". (A non-last activity *can* legitimately
			// run past midnight; that is shown in "long-night-out-home-at-30h" below.)

			// Last activity starts before midnight (no end time): with no merge partner it is just measured to the
			// 24h boundary (86400-66600 = 19800).
			// Compare with "overnight-activities-are-merged" above, which has the same times but a "home" last
			// activity, so the evening and morning portions merge into one 41400 overnight activity.
			new Case("midnight-starts-before-no-merge")
				.in(act("home").end("06:00:00"))                   // 21600
				.in(act("work").start("06:30:00").end("18:00:00")) // 41400
				.in(act("leisure").start("18:30:00"))              // last, start only -> 86400-66600 = 19800
				.expect(out("home_21600").end("06:00:00"))
				.expect(out("work_41400").end("18:00:00"))
				.expect(out("leisure_19800")),

			// Last activity *starts after* midnight (25:00 = 90000), no merge partner. The start lies beyond the 24h
			// period boundary, so measuring to that boundary gives a negative raw duration (86400-90000 = -3600),
			// which roundDuration() clamps to the smallest bin, 600. With no same-type partner to wrap into, this is
			// genuinely degenerate input (a daily activity that starts after the day is over); 600 is just the
			// fallback. (The short-activity rewrite does not fire: there is no end time.)
			new Case("midnight-starts-after-midnight-no-merge")
				.in(act("home").end("06:00:00"))                   // 21600
				.in(act("work").start("06:30:00").end("18:00:00")) // 41400
				.in(act("leisure").start("25:00:00"))              // 86400-90000 = -3600 -> clamped to 600
				.expect(out("home_21600").end("06:00:00"))
				.expect(out("work_41400").end("18:00:00"))
				.expect(out("leisure_600")),

			// Same as above, but with a testee configured for a longer day: maxTypicalDuration = 27h (97200s).
			// Now the last activity's start (25:00 = 90000) is *inside* the period, so measuring to the boundary
			// gives a positive duration (97200-90000 = 7200) instead of a negative one that needs clamping.
			new Case("midnight-starts-after-midnight-no-merge-mtd-27h")
				.maxTypicalDuration(Time.parseTime("27:00:00"))
				.in(act("home").end("06:00:00"))                   // 21600
				.in(act("work").start("06:30:00").end("18:00:00")) // 41400
				.in(act("leisure").start("25:00:00"))              // 97200-90000 = 7200
				.expect(out("home_21600").end("06:00:00"))
				.expect(out("work_41400").end("18:00:00"))
				.expect(out("leisure_7200")),

			// Same, but the last activity is "home" and so merges with the morning home. This exposes a bug.
			// Periodic-correct: the overnight home runs from 25:00 to the next day's 06:00, i.e.
			// 86400 + 21600 - 90000 = 18000 (5h). But run() first rounds/clamps the (negative) evening portion to
			// 600, and mergeOvernightActivities() then sums the already-clamped suffixes: 21600 + 600 = 22200. So
			// the result (22200, ~6h10m) overstates the true overnight duration by exactly the clamp swing (4200s).
			// The fix would be to merge the *raw* (signed) portions before rounding.
			new Case("midnight-starts-after-midnight-with-merge")
				.in(act("home").end("06:00:00"))                   // morning portion: 21600
				.in(act("home").start("25:00:00"))                 // evening portion: 86400-90000 = -3600 -> clamped to 600
				.expect(out("home_22200").end("06:00:00"))         // BUG: 21600 + 600 = 22200; periodic-correct would be 18000
				.expect(out("home_22200")),

			// A full chain that keeps going well past midnight and only gets back home at 30:00:05 the next morning (the final home activity
			// *starts* at 30:00:05 and, like every last activity, has no end time). The middle activities keep their
			// post-midnight end times and are split by their durations - times past 24h are not folded
			// back. The final home starts past the 24h boundary, so it hits the same merge bug as the case above:
			// periodic-correct overnight = 86400 + 28800 - 108005 = 7195 (~2h, home from 06:00 to 08:00 next day),
			// but the clamp-then-sum yields 28800 + 600 = 29400. Documented as current behaviour.
			new Case("long-night-out-home-at-30h")
				.in(act("home").end("08:00:00"))                          // morning portion: 28800
				.in(act("work").start("09:00:00").end("17:30:00"))        // 30600
				.in(act("leisure").start("19:00:00").end("23:00:00"))     // 14400
				.in(act("restaurant").start("24:00:00").end("26:00:00"))  // 7200, just after midnight
				.in(act("shopping").start("27:00:00").end("28:00:00"))    // 3600, deep into the night
				.in(act("home").start("30:00:05"))                        // evening portion: 86400-108005 < 0 -> clamped to 600
				.expect(out("home_29400").end("08:00:00"))                // BUG: 28800 + 600 = 29400; periodic-correct would be 7200
				.expect(out("work_30600").end("17:30:00"))
				.expect(out("leisure_14400").end("23:00:00"))
				.expect(out("restaurant_7200").end("26:00:00"))
				.expect(out("shopping_3600").end("28:00:00"))
				.expect(out("home_29400")),
			// No merge for a single-activity plan (nothing to merge against). The single activity is also the last
			// one, so it carries only a start time: 86400-28800 = 57600.
			new Case("single-activity-plan-is-not-merged")
				.in(act("home").start("08:00:00"))
				.expect(out("home_57600")),

			// Excluded types are neither split nor merged. Here "home" is excluded, so it keeps its raw type; because
			// the first/last activities are no longer "<base>_<duration>", the overnight merge is skipped too.
			new Case("excluded-types-are-left-untouched")
				.exclude("home")
				.in(act("home").end("08:00:00"))
				.in(act("work").start("08:30:00").end("17:00:00")) // 30600
				.in(act("home").start("17:30:00"))
				.expect(out("home").end("08:00:00"))
				.expect(out("work_30600").end("17:00:00"))
				.expect(out("home")),

			// Just below the overlong threshold (maxTypicalDuration * overlongPlansFactor = 86400 * 1.2 = 103680):
			// the summed rounded durations are 86400 + 16800 = 103200, which is accepted. (Rounded durations are
			// always multiples of 600, so 103200 is the largest possible sum that does not exceed the threshold.)
			// Compare with overlongPlanIsRejected(), which is one bin (600s) higher and fails.
			new Case("just-below-overlong-threshold")
				.in(act("a").maxDuration(86400))  // capped at the largest bin, 86400
				.in(act("b").start("19:20:00"))   // last, start only: 86400-69600 = 16800
				.expect(out("a_86400").maxDuration(86400))
				.expect(out("b_16800"))
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	void transform(Case c) {

		Person person = c.toPerson();

		SplitActivityTypesDuration algorithm = new SplitActivityTypesDuration(BIN_SIZE, c.maxTypicalDuration, END_TIME_TO_DURATION);
		algorithm.setExclude(new HashSet<>(c.exclude));

		algorithm.run(person);

		List<Activity> activities = TripStructureUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

		assertThat(activities).hasSameSizeAs(c.expected);

		for (int i = 0; i < activities.size(); i++) {
			Activity actual = activities.get(i);
			ActOut expected = c.expected.get(i);

			assertThat(actual.getType())
				.as("type of activity %d in case '%s'", i, c.name)
				.isEqualTo(expected.type);

			if (expected.endTime == null)
				assertThat(actual.getEndTime().isUndefined())
					.as("end time of activity %d ('%s') should be undefined", i, expected.type)
					.isTrue();
			else
				assertThat(actual.getEndTime().seconds())
					.as("end time of activity %d ('%s')", i, expected.type)
					.isEqualTo(expected.endTime);

			if (expected.maxDuration == null)
				assertThat(actual.getMaximumDuration().isUndefined())
					.as("maximum duration of activity %d ('%s') should be undefined", i, expected.type)
					.isTrue();
			else
				assertThat(actual.getMaximumDuration().seconds())
					.as("maximum duration of activity %d ('%s')", i, expected.type)
					.isEqualTo(expected.maxDuration);
		}
	}

	/**
	 * Plans whose summed (rounded) durations exceed {@code overlongPlansFactor * maxTypicalDuration} (default
	 * 1.2 * 24h = 103680s) are rejected, because they indicate broken input data. This is the just-over boundary:
	 * 86400 + 17400 = 103800, one 600s bin above the threshold (compare with the accepted "just-below" case).
	 */
	@Test
	void overlongPlanIsRejected() {

		Case c = new Case("overlong")
			.in(act("a").maxDuration(86400)) // capped at the largest bin, 86400
			.in(act("b").start("19:10:00")); // last, start only: 86400-69000 = 17400; sum 103800 > 103680

		Person person = c.toPerson();

		SplitActivityTypesDuration algorithm = new SplitActivityTypesDuration(BIN_SIZE, MAX_TYPICAL_DURATION, END_TIME_TO_DURATION);

		assertThatThrownBy(() -> algorithm.run(person))
			.isInstanceOf(RuntimeException.class);
	}

	// --- tiny tabular DSL for building input plans and expected results -------------------------------------------

	private static final Coord ORIGIN = new Coord(0, 0);

	private static ActIn act(String type) {
		return new ActIn(type);
	}

	private static ActOut out(String type) {
		return new ActOut(type);
	}

	/** A literal input activity. Times are in seconds; unset values mean "no start / end / maximum duration". */
	private static final class ActIn {
		private final String type;
		private Double startTime;
		private Double endTime;
		private Double maxDuration;

		private ActIn(String type) {
			this.type = type;
		}

		ActIn start(String time) {
			this.startTime = Time.parseTime(time);
			return this;
		}

		ActIn end(String time) {
			this.endTime = Time.parseTime(time);
			return this;
		}

		ActIn maxDuration(double seconds) {
			this.maxDuration = seconds;
			return this;
		}
	}

	/** The expected state of an activity after the transformation. By default end time and maximum duration are undefined. */
	private static final class ActOut {
		private final String type;
		private Double endTime;
		private Double maxDuration;

		private ActOut(String type) {
			this.type = type;
		}

		ActOut end(String time) {
			this.endTime = Time.parseTime(time);
			return this;
		}

		ActOut maxDuration(double seconds) {
			this.maxDuration = seconds;
			return this;
		}
	}

	private static final class Case {
		private final String name;
		private final Set<String> exclude = new HashSet<>();
		private final List<ActIn> in = new ArrayList<>();
		private final List<ActOut> expected = new ArrayList<>();
		private int maxTypicalDuration = MAX_TYPICAL_DURATION;

		private Case(String name) {
			this.name = name;
		}

		Case maxTypicalDuration(double seconds) {
			this.maxTypicalDuration = (int) seconds;
			return this;
		}

		Case exclude(String... types) {
			this.exclude.addAll(Arrays.asList(types));
			return this;
		}

		Case in(ActIn act) {
			this.in.add(act);
			return this;
		}

		Case expect(ActOut act) {
			this.expected.add(act);
			return this;
		}

		Person toPerson() {
			Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId(name));
			Plan plan = PopulationUtils.createPlan();
			person.addPlan(plan);

			for (int i = 0; i < in.size(); i++) {
				ActIn a = in.get(i);
				if (i > 0)
					PopulationUtils.createAndAddLeg(plan, "car");

				Activity activity = PopulationUtils.createAndAddActivityFromCoord(plan, a.type, ORIGIN);
				if (a.startTime != null)
					activity.setStartTime(a.startTime);
				if (a.endTime != null)
					activity.setEndTime(a.endTime);
				if (a.maxDuration != null)
					activity.setMaximumDuration(a.maxDuration);
			}

			return person;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
