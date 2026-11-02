package org.matsim.contrib.msp.capability.structure;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes the activity chain of a plan as one readable string, e.g. {@code home-work-leisure-home}.
 * <p>
 * Two spellings of the same chain are produced, because the reduction to the fundamental plan is
 * easier to check when both are side by side:
 * <ul>
 *     <li>{@link #of} names the activities, e.g. {@code home-work-leisure-home};</li>
 *     <li>{@link #classified} names their class instead, e.g. {@code home-mandatory-discretionary-home},
 *     which is exactly the information {@link FundamentalPlanExtractor} acts on.</li>
 * </ul>
 * Activity types are normalised first, so that chains of different agents can be compared and counted:
 * the typical duration MATSim appends is stripped off ({@code work_7200} becomes {@code work}), and the
 * sub-types of home collapse into plain {@code home} ({@code home_morning} and {@code home_28200} both
 * become {@code home}), because the morning and the evening stay at home are the same activity as far as
 * the measure is concerned. Other sub-types are kept as they are - {@code educ_primary} says something
 * {@code educ} does not.
 * <p>
 * {@link #mergeConsecutiveHome} is the last step for a <i>fundamental</i> chain: once the discretionary
 * activities are gone, two home stays that were separated by one of them have become a single stay, and
 * writing them as {@code home-home} would claim the person went out in between. It is the same merge
 * {@link CapabilityTimeline} does to the drawn day, so the chain and the drawing agree.
 *
 * @see FundamentalPlanExtractor
 */
public final class ActivityChain {

	/**
	 * What the activities are joined with. Activity types do not contain it in any scenario seen so
	 * far, so the chain stays splittable.
	 */
	public static final String SEPARATOR = "-";

	/**
	 * The class of a non-home activity the agent has no realistic freedom to drop.
	 */
	public static final String MANDATORY = "mandatory";

	/**
	 * The class of an activity whose performance is a matter of opportunity, i.e. the ones the
	 * fundamental plan drops.
	 */
	public static final String DISCRETIONARY = "discretionary";

	private ActivityChain() {
	}

	/**
	 * Collapses every run of consecutive home activities into its first one. Applied to a fundamental
	 * plan, so that the day of a person with no non-home mandatory activity reads {@code home} rather
	 * than {@code home-home} - the discretionary activity that sat between the two home stays is gone,
	 * so the person never left.
	 * <p>
	 * Not applied to an original chain: there, two home activities are separated by whatever the person
	 * actually did in between, so there is nothing to merge, and a plan that really does carry two
	 * adjacent home activities is better shown as it is.
	 *
	 * @return the activities with the runs of home collapsed, the given list unchanged
	 */
	public static List<Activity> mergeConsecutiveHome(List<Activity> activities, CapabilityConfigGroup config) {

		List<Activity> merged = new ArrayList<>(activities.size());

		for (Activity activity : activities) {
			boolean isHome = config.isHome(activity.getType());
			boolean previousWasHome = !merged.isEmpty()
				&& config.isHome(merged.get(merged.size() - 1).getType());

			if (!isHome || !previousWasHome) {
				merged.add(activity);
			}
		}

		return merged;
	}

	/**
	 * @return the normalised types of the given activities, in order, e.g. {@code home-work-home}, or
	 * an empty string if there are no activities
	 */
	public static String of(List<Activity> activities, CapabilityConfigGroup config) {
		return activities.stream()
			.map(activity -> normalize(activity.getType(), config))
			.collect(Collectors.joining(SEPARATOR));
	}

	/**
	 * @return the class of each of the given activities, in order, e.g.
	 * {@code home-mandatory-discretionary-home}
	 */
	public static String classified(List<Activity> activities, CapabilityConfigGroup config) {
		return activities.stream()
			.map(activity -> classify(activity.getType(), config))
			.collect(Collectors.joining(SEPARATOR));
	}

	/**
	 * @return the given activity type without the typical duration, and with every sub-type of home
	 * collapsed into the configured home activity
	 */
	public static String normalize(String activityType, CapabilityConfigGroup config) {
		return config.isHome(activityType)
			? config.getHomeActivity()
			: CapabilityConfigGroup.stripTypicalDuration(activityType);
	}

	/**
	 * @return {@link CapabilityConfigGroup#getHomeActivity()} for a home activity, {@link #MANDATORY}
	 * for any other mandatory one, {@link #DISCRETIONARY} otherwise. Home is named rather than called
	 * mandatory because it is the elastic one: it is what the chain is anchored at, not what pins it
	 * down.
	 */
	public static String classify(String activityType, CapabilityConfigGroup config) {
		if (config.isHome(activityType)) {
			return config.getHomeActivity();
		}
		return config.isMandatory(activityType) ? MANDATORY : DISCRETIONARY;
	}
}
