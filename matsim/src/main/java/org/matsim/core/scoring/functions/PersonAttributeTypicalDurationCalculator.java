package org.matsim.core.scoring.functions;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.utils.misc.OptionalTime;

import java.util.List;

/**
 * Takes the typical duration from the person's {@value PersonUtils#TYPICAL_DURATIONS} attribute, which lists the
 * typical durations of the main activities of the person's plan in their order.  {@link CharyparNagelActivityScoring}
 * pairs the executed activities with the main activities of the selected plan and hands over the position, which
 * selects the entry; the entry's type must be the activity's type.  Selected by
 * {@link ScoringConfigGroup.TypicalDurationSource#personAttribute}.
 * <p>
 * Persons without the attribute, and scoring without a person, fall back to the typical duration of the activity's
 * type from the scoring config, so that e.g. a subpopulation without individual typical durations is scored as usual.
 * A person with the attribute, however, must have a fitting, positive entry for every scored main activity.  If not,
 * the attribute was written for a different plan, and the run aborts instead of scoring against the config values.
 */
public final class PersonAttributeTypicalDurationCalculator implements TypicalDurationCalculator {

	@Override
	public OptionalTime getTypicalDuration(Person person, Activity act, int mainActivityIndex) {
		if (person == null || StageActivityTypeIdentifier.isStageActivity(act.getType())) {
			return OptionalTime.undefined();
		}
		List<PersonUtils.TypicalDuration> typicalDurations = PersonUtils.getTypicalDurations(person);
		if (typicalDurations == null) {
			return OptionalTime.undefined();
		}
		if (mainActivityIndex == UNALIGNED) {
			throw inconsistent(person, act, mainActivityIndex, "the executed activities could not be paired with the main activities of the selected plan");
		}
		if (mainActivityIndex >= typicalDurations.size()) {
			throw inconsistent(person, act, mainActivityIndex, "the attribute has only " + typicalDurations.size() + " entries");
		}
		PersonUtils.TypicalDuration typicalDuration = typicalDurations.get(mainActivityIndex);
		if (!typicalDuration.type().equals(act.getType())) {
			throw inconsistent(person, act, mainActivityIndex, "the entry is for type " + typicalDuration.type());
		}
		if (!(typicalDuration.seconds() > 0)) {
			throw inconsistent(person, act, mainActivityIndex, "the entry " + typicalDuration.seconds() + " is not positive");
		}
		return OptionalTime.defined(typicalDuration.seconds());
	}

	private static IllegalStateException inconsistent(Person person, Activity act, int mainActivityIndex, String reason) {
		return new IllegalStateException("The " + PersonUtils.TYPICAL_DURATIONS + " attribute of person " + person.getId()
			+ " does not fit main activity " + mainActivityIndex + " (type " + act.getType() + ") of the plan: " + reason
			+ ". It must list the main activities of the plan in their order as type=seconds;type=seconds;..., with positive durations.");
	}

}
