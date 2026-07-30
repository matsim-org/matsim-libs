package org.matsim.core.scoring.functions;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Takes the typical duration from the {@value #TYPICAL_DURATION_ATTRIBUTE} attribute (in seconds) of the activity
 * itself, so that every activity can carry its own typical duration instead of encoding it in the activity type
 * (e.g. one config activity type per duration bin, {@code home_30}, {@code home_60}, ...).  Activities without the
 * attribute (or with a non-positive value) fall back to the typical duration of their activity type from the
 * scoring config.
 * <p>
 * Note that the activities handed to the scoring during a simulation are reconstructed from events and carry no
 * attributes; pass the {@link org.matsim.api.core.v01.population.Person} to
 * {@link CharyparNagelActivityScoring} so that the attributes are read from the selected plan.
 */
public final class ActivityAttributeTypicalDurationCalculator implements TypicalDurationCalculator {

	/**
	 * Attribute key under which an activity carries its typical duration (in seconds); deliberately the same string
	 * as the config parameter {@link ScoringConfigGroup.ActivityParams#TYPICAL_DURATION}.
	 */
	public static final String TYPICAL_DURATION_ATTRIBUTE = ScoringConfigGroup.ActivityParams.TYPICAL_DURATION;

	@Override
	public OptionalTime getTypicalDuration(Activity act) {
		Object value = act.getAttributes().getAttribute(TYPICAL_DURATION_ATTRIBUTE);
		if (value instanceof Number number && number.doubleValue() > 0) {
			return OptionalTime.defined(number.doubleValue());
		}
		return OptionalTime.undefined();
	}

}
