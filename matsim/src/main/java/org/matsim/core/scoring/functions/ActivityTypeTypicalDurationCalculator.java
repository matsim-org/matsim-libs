package org.matsim.core.scoring.functions;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Leaves the typical duration of every activity to the config parameters of its type; the default.
 */
public final class ActivityTypeTypicalDurationCalculator implements TypicalDurationCalculator {

	@Override
	public OptionalTime getTypicalDuration(Person person, Activity act, int mainActivityIndex) {
		return OptionalTime.undefined();
	}

}
