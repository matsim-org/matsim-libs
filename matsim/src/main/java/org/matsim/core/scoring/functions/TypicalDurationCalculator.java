package org.matsim.core.scoring.functions;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Resolves the Charypar-Nagel typical duration for an individual activity, analogous to
 * {@link OpeningIntervalCalculator} for opening times.  A defined result overrides the typical duration of the
 * activity's type from the scoring config, and the zero-utility duration is recomputed from it via
 * {@link ActivityUtilityParameters#computeZeroUtilityDuration_h(double)} so that the scoring curve stays
 * self-consistent.  An undefined result means the activity is scored against its type's config parameters, exactly
 * as without this hook.
 *
 * @see ActivityAttributeTypicalDurationCalculator
 */
public interface TypicalDurationCalculator {

	OptionalTime getTypicalDuration(final Activity act);

}
