package org.matsim.contrib.msp.capability.structure;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * The day of a shrunk plan as a sequence of blocks on a 00:00-24:00 axis, ready to be drawn.
 * <p>
 * Two things happen here that the fundamental plan itself does not express:
 * <ul>
 *     <li><b>Consecutive home activities are merged.</b> Home is elastic, so a plan whose mandatory
 *     activities are all home is one single stay at home as far as this analysis is concerned, not
 *     three separate ones. Without merging, someone who never performs a fixed activity would be drawn
 *     as several home bars with meaningless boundaries between them.</li>
 *     <li><b>Home blocks get soft boundaries.</b> The agent is certainly at home in the middle of the
 *     night and certainly at a fixed activity when one is scheduled, but when exactly it left or came
 *     home is not determined - that is the whole point of treating home as elastic. The block therefore
 *     carries a {@link Block#softStart()} and {@link Block#softEnd()}: it should be drawn <b>opaque
 *     outside</b> that range and <b>transparent inside</b> it, with a gradient in between.</li>
 * </ul>
 * A fixed (non-home) block has no soft range - its times are known - and is drawn solid.
 */
public final class CapabilityTimeline {

	/**
	 * When the agent stops being certainly at home in the morning, unless a fixed activity forces it
	 * earlier. Chosen so that the transparent part of the day roughly covers the hours in which shops
	 * are open.
	 */
	public static final double DAY_SOFT_START = 6 * 3600;

	/**
	 * When the agent becomes certainly at home again in the evening, unless a fixed activity pushes it
	 * later.
	 */
	public static final double DAY_SOFT_END = 22 * 3600;

	/**
	 * The shortest gradient a soft boundary is drawn with, so that a boundary never collapses into a
	 * hard edge just because a fixed activity happens early or late.
	 */
	public static final double MIN_RAMP = 3600;

	private CapabilityTimeline() {
	}

	/**
	 * One bar of the time axis.
	 *
	 * @param type the basic activity type, e.g. {@code home} or {@code work}
	 * @param home true if this is a (merged) stay at home
	 * @param from when the block starts [s]
	 * @param to when it ends [s]
	 * @param softStart start of the uncertain range; equal to {@code from} if the block is uncertain
	 * from its very beginning
	 * @param softEnd end of the uncertain range; equal to {@code to} if it stays uncertain to its end.
	 * For a fixed block {@code softStart == softEnd}, i.e. there is nothing uncertain to draw.
	 * @param coord where the activity is
	 */
	public record Block(String type, boolean home, double from, double to,
						double softStart, double softEnd, Coord coord) {

		/**
		 * @return true if there is an uncertain range to draw a gradient over
		 */
		public boolean isSoft() {
			return this.softEnd > this.softStart;
		}
	}

	/**
	 * @param plan a plan reduced to its mandatory activities, see {@link FundamentalPlanExtractor}
	 * @return the blocks of the day, in order. Empty if the plan has no activity.
	 */
	public static List<Block> of(Plan plan, CapabilityConfigGroup config) {

		List<Activity> activities = plan.getPlanElements().stream()
			.filter(Activity.class::isInstance)
			.map(Activity.class::cast)
			.toList();

		if (activities.isEmpty()) {
			return List.of();
		}

		// the fixed activities are what the day is pinned to; everything between them is one stay at home
		List<Block> blocks = new ArrayList<>();
		int i = 0;

		while (i < activities.size()) {
			Activity activity = activities.get(i);

			if (!config.isHome(activity.getType())) {
				double from = Math.min(activity.getStartTime().orElse(0), FreePeriod.END_OF_DAY);
				double to = Math.min(FundamentalPlanExtractor.departureTime(activity)
					.orElse(FreePeriod.END_OF_DAY), FreePeriod.END_OF_DAY);
				blocks.add(new Block(CapabilityConfigGroup.stripTypicalDuration(activity.getType()),
					false, from, Math.max(from, to), from, from, activity.getCoord()));
				i++;
				continue;
			}

			// collapse this run of home activities into one block
			int runStart = i;
			while (i < activities.size() && config.isHome(activities.get(i).getType())) {
				i++;
			}

			// a home block fills the whole gap between the fixed activities around it, because the agent
			// may come home late and leave early
			double from = runStart == 0 ? 0 : blocks.get(blocks.size() - 1).to();
			double to = i < activities.size()
				? Math.min(activities.get(i).getStartTime().orElse(FreePeriod.END_OF_DAY), FreePeriod.END_OF_DAY)
				: FreePeriod.END_OF_DAY;
			to = Math.max(from, to);

			blocks.add(new Block(CapabilityConfigGroup.stripTypicalDuration(activities.get(runStart).getType()),
				true, from, to, softStart(from, to, runStart == 0),
				softEnd(from, to, i >= activities.size()), activities.get(runStart).getCoord()));
		}

		return blocks;
	}

	/**
	 * @param atDayStart true if the block starts at 00:00, where the agent is certainly at home
	 * @return when the agent stops being certainly at home
	 */
	private static double softStart(double from, double to, boolean atDayStart) {
		if (!atDayStart) {
			// the block begins right after a fixed activity, so it is uncertain from the start: the agent
			// may not have gone home at all
			return from;
		}
		// certainly home until the small hours, but never so late that the gradient has no room left
		return clamp(Math.min(DAY_SOFT_START, to - MIN_RAMP), from, to);
	}

	/**
	 * @param atDayEnd true if the block ends at 24:00, where the agent is certainly at home again
	 */
	private static double softEnd(double from, double to, boolean atDayEnd) {
		if (!atDayEnd) {
			return to;
		}
		return clamp(Math.max(DAY_SOFT_END, from + MIN_RAMP), from, to);
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
