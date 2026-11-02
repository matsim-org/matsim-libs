package org.matsim.contrib.msp.capability.structure;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A stretch of the day in which a discretionary activity could be inserted: the agent leaves
 * {@link #origin()} no earlier than {@link #availableFrom()} and has to be at {@link #destination()}
 * by {@link #deadline()}.
 * <p>
 * <b>What delimits a free period.</b> Only the mandatory activities that are <i>not</i> home, because
 * home is elastic - the agent may come home later, leave earlier, or skip a home stay. Those
 * non-home activities are the fixed anchors of the day, and the free periods are the stretches between
 * them, plus the one before the first anchor (which starts at 00:00 at home) and the one after the last
 * (which ends at 24:00 at home). An agent with no fixed anchor at all - someone who stays home, or
 * whose mandatory activities are all home - has one free period covering the whole day.
 * <p>
 * Treating home as elastic is what makes the measure meaningful. With home pinned down, a plan like
 * <pre>
 *   home 00:00-08:00 -&gt; work 08:30-17:00 -&gt; home 17:20-...
 * </pre>
 * would leave gaps of 30 and 20 minutes, into which nothing fits once travel and a minimum duration are
 * accounted for, and every commuter would come out with no capability at all. With home elastic the
 * evening opens up to the end of the day, which is where a commuter's shopping flexibility actually is.
 * <p>
 * <b>Insertion is en route</b>: {@code origin -> facility -> destination}. The alternative of going home
 * first and making a round trip from there is never better: by the triangle inequality its detour is at
 * least as long, it cannot start earlier, and it ends at the same deadline, so it is dominated on both
 * utility and cost. That holds as long as one mode is used for the whole detour, which is the case here;
 * see the known limitations in the README for what that leaves out.
 * <p>
 * Intermediate home activities inside a period are ignored for routing, so for a period between two work
 * episodes the baseline is the direct trip rather than the trip via home. The common plan shapes - home
 * to work, work to home, and the whole day for someone who stays home - have no intermediate activity,
 * so this is exact for them and an approximation only for multi-episode plans.
 */
public record FreePeriod(Facility origin, double availableFrom, Facility destination, double deadline,
						 String originType, String destinationType) {

	/**
	 * Without the activity types, which only the drawing needs.
	 */
	public FreePeriod(Facility origin, double availableFrom, Facility destination, double deadline) {
		this(origin, availableFrom, destination, deadline, "", "");
	}

	/**
	 * End of the analysed day [s]. Everything is confined to 00:00-24:00, so there is no wrap around.
	 */
	public static final double END_OF_DAY = 24 * 3600;

	public FreePeriod {
		if (deadline < availableFrom) {
			throw new IllegalArgumentException("A free period cannot end before it starts: "
				+ Time.writeTime(availableFrom) + " to " + Time.writeTime(deadline));
		}
	}

	/**
	 * Splits the day of a shrunk plan into its free periods.
	 *
	 * @param plan a plan reduced to its mandatory activities, see {@link FundamentalPlanExtractor}
	 * @param config used to tell the elastic home activity from the fixed ones
	 * @return the free periods, in the order of the day. Empty if the plan has no activity at all.
	 */
	public static List<FreePeriod> of(Plan plan, CapabilityConfigGroup config) {

		List<Activity> activities = plan.getPlanElements().stream()
			.filter(Activity.class::isInstance)
			.map(Activity.class::cast)
			.toList();

		if (activities.isEmpty()) {
			return List.of();
		}

		Activity first = activities.get(0);
		Activity last = activities.get(activities.size() - 1);

		// the fixed anchors of the day: every mandatory activity that is not home
		List<Activity> anchors = anchorsOf(plan, config);

		List<FreePeriod> periods = new ArrayList<>();

		if (anchors.isEmpty()) {
			// nothing pins the day down, so the whole day is one free period. This is the case for
			// someone who stays home, and it is the only way such a person enters the analysis at all,
			// since they have no gap between two activities.
			add(periods, first, 0, last, END_OF_DAY);
			return periods;
		}

		// before the first anchor: the agent is at its first activity from the beginning of the day
		add(periods, first, 0, anchors.get(0), startOf(anchors.get(0)));

		// between two consecutive anchors
		for (int i = 0; i < anchors.size() - 1; i++) {
			Activity from = anchors.get(i);
			Activity to = anchors.get(i + 1);
			add(periods, from, endOf(from), to, startOf(to));
		}

		// after the last anchor: the agent has until the end of the day to get to its last activity
		Activity lastAnchor = anchors.get(anchors.size() - 1);
		add(periods, lastAnchor, endOf(lastAnchor), last, END_OF_DAY);

		return periods;
	}

	/**
	 * Adds a period, unless it has no time in it at all. Back to back fixed activities leave no room for
	 * anything, which is a perfectly ordinary plan rather than an error, so such a period is simply
	 * dropped instead of failing the analysis.
	 */
	private static void add(List<FreePeriod> periods, Activity origin, double availableFrom,
						   Activity destination, double deadline) {
		if (deadline > availableFrom) {
			periods.add(new FreePeriod(facility(origin), availableFrom, facility(destination), deadline,
				CapabilityConfigGroup.stripTypicalDuration(origin.getType()),
				CapabilityConfigGroup.stripTypicalDuration(destination.getType())));
		}
	}

	/**
	 * Checks that the fixed activities say when they happen, since it is their times that delimit the free
	 * periods. A plan that does not say is not analysed at all: inventing a time would silently produce a
	 * wrong capability instead of an obvious gap in the output.
	 * <p>
	 * Plans files do not normally store activity start times, so they are reconstructed - see
	 * {@link FundamentalPlanExtractor#startTimes(Plan)}. This returns false when even that fails, which
	 * happens for a plans file whose legs carry neither travel nor departure times.
	 *
	 * @return true if every fixed activity has both a start and a departure time
	 */
	public static boolean hasUsableTimes(Plan plan, CapabilityConfigGroup config) {
		return anchorsOf(plan, config).stream().allMatch(anchor ->
			anchor.getStartTime().isDefined() && FundamentalPlanExtractor.departureTime(anchor).isDefined());
	}

	/**
	 * @return the mandatory activities that pin the day down, i.e. all of them except home
	 */
	private static List<Activity> anchorsOf(Plan plan, CapabilityConfigGroup config) {
		return plan.getPlanElements().stream()
			.filter(Activity.class::isInstance)
			.map(Activity.class::cast)
			.filter(activity -> !config.isHome(activity.getType()))
			.toList();
	}

	/**
	 * @return true if every fixed activity of the plan happens inside the analysed day, so the plan can
	 * be analysed without having to deal with times past midnight
	 */
	public static boolean isWithinDay(Plan plan, CapabilityConfigGroup config) {
		return anchorsOf(plan, config).stream()
			.allMatch(activity -> activity.getStartTime().orElse(0) <= END_OF_DAY
				&& FundamentalPlanExtractor.departureTime(activity).orElse(0) <= END_OF_DAY);
	}

	private static double startOf(Activity anchor) {
		// guarded by hasUsableTimes: a fixed activity whose start time is unknown would otherwise collapse
		// the period before it to nothing, which looks like "no capability" rather than "cannot tell"
		return Math.min(anchor.getStartTime().orElse(0), END_OF_DAY);
	}

	private static double endOf(Activity anchor) {
		// a scenario may give an activity a maximum duration rather than an end time, so ask for the
		// departure time rather than reading the end time directly. A fixed activity that says neither
		// occupies the rest of the day.
		return Math.min(FundamentalPlanExtractor.departureTime(anchor).orElse(END_OF_DAY), END_OF_DAY);
	}

	private static Facility facility(Activity activity) {
		return new ActivityFacilityView(activity);
	}

	/**
	 * An activity seen as a place, so that the travel times can treat a person's own activities and the
	 * discretionary activity facilities alike. MATSim's own {@code ActivityWrapperFacility} is not public.
	 */
	private record ActivityFacilityView(Activity activity) implements Facility {

		@Override
		public Coord getCoord() {
			return this.activity.getCoord();
		}

		@Override
		public Id<Link> getLinkId() {
			return this.activity.getLinkId();
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			return Map.of();
		}
	}
}
