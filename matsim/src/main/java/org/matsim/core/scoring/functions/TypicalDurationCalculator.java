package org.matsim.core.scoring.functions;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Resolves the Charypar-Nagel typical duration for an individual activity, analogous to
 * {@link OpeningIntervalCalculator} for opening times.  A defined result overrides the typical duration of the
 * activity's type from the scoring config, and the zero-utility duration is recomputed from it via
 * {@link ActivityUtilityParameters#computeZeroUtilityDuration_h(double)} so that the scoring curve stays
 * self-consistent.  An undefined result means the activity is scored against its type's config parameters, exactly
 * as without this hook.
 * <p>
 * The default scoring uses the implementation selected by {@link ScoringConfigGroup#getTypicalDurationSource()}
 * (see {@link #of}); code can bind its own instead, e.g.
 * {@code bind(TypicalDurationCalculator.class).to(MyTypicalDurationCalculator.class)} in an overriding module.  A
 * bound instance is shared by the scoring functions of all persons, which may be called concurrently.
 *
 * @see ActivityTypeTypicalDurationCalculator
 * @see ActivityAttributeTypicalDurationCalculator
 * @see PersonAttributeTypicalDurationCalculator
 */
public interface TypicalDurationCalculator {

	/**
	 * Main-activity index of an activity that is not paired with a main activity of the person's selected plan.
	 */
	int UNALIGNED = -1;

	/**
	 * @param person            the person whose activity is scored, or null if the scoring does not know it
	 * @param act               the main activity of the person's selected plan at {@code mainActivityIndex}, which
	 *                          carries the attributes of the plan; if {@link #UNALIGNED}, the activity as handed to
	 *                          the scoring
	 * @param mainActivityIndex the position of the activity among the main (non-stage) activities of the selected
	 *                          plan, or {@link #UNALIGNED} for stage activities, without a person, or if the executed
	 *                          activities could not be paired with the plan
	 * @return the typical duration in seconds, or undefined for the typical duration of the activity's type
	 */
	OptionalTime getTypicalDuration(Person person, Activity act, int mainActivityIndex);

	/**
	 * The calculator for the given source of typical durations.
	 */
	static TypicalDurationCalculator of(ScoringConfigGroup.TypicalDurationSource source) {
		return switch (source) {
			case activityParams -> new ActivityTypeTypicalDurationCalculator();
			case activityAttribute -> new ActivityAttributeTypicalDurationCalculator();
			case personAttribute -> new PersonAttributeTypicalDurationCalculator();
		};
	}

}
