package org.matsim.contrib.msp.capability.measure;

import org.matsim.contrib.msp.capability.structure.FreePeriod;

/**
 * How the flexibility of <b>one insertion</b> is turned into a utility bonus.
 * <p>
 * The flexibility is the {@code slack} of that insertion: the width of the window the activity could be
 * started in, i.e. the time available on site less the minimum duration (see {@link FreePeriod} for
 * where the window comes from).
 * <pre>
 *   bonus = f(slack)
 * </pre>
 * The function is applied <b>per insertion</b>, and the insertions of one facility are then combined by
 * taking the <b>maximum</b> of their utilities - never by summing their slack first. Both halves of that
 * matter, and for the same reason the function is concave:
 * <ul>
 *     <li>Summing the slack of several insertions and applying the function once would describe a trip
 *     nobody can make - no single opportunity offers the freedom of all of them.</li>
 *     <li>Applying the function per insertion and <i>summing</i> the bonuses would reward a fragmented
 *     day: a person with four one-hour chances would beat a person who is free all day, which is the
 *     opposite of what the measure is supposed to say.</li>
 * </ul>
 * The maximum avoids both: a person who is free all day holds one very flexible opportunity, which is
 * worth more than any of the short ones a fragmented day offers.
 */
public enum FlexibilityFunction {

	/**
	 * {@code k * ln(1 + slack / referenceDuration)}, the default.
	 * <p>
	 * Concave, so the tenth free hour is worth less than the first - which is how a time buffer
	 * actually behaves. It needs no cap: for a whole free day it stays just below the base utility,
	 * so the "can this activity be done at all" signal keeps the upper hand over "how freely".
	 * <p>
	 * The {@code 1 +} keeps it well defined and non-negative at {@code slack = 0}: an activity that fits
	 * with no slack to spare scores exactly the base utility, rather than {@code ln(0)}. Dividing by
	 * the reference duration makes the argument of the logarithm dimensionless and sets the scale at
	 * which the diminishing returns set in.
	 */
	LOG,

	/**
	 * {@code k * slack}, in units of the reference duration. Not implemented yet.
	 * <p>
	 * Grows without bound, so for a person who is free all day the flexibility term outgrows the base
	 * utility several times over. It would need a cap to be usable.
	 */
	LINEAR,

	/**
	 * The number of distinct parts of the day (morning, afternoon, evening) in which the activity can
	 * be started. Not implemented yet.
	 * <p>
	 * The most literal reading of "being able to go in the morning or in the afternoon or in the
	 * evening is worth more than only in the evening". Bounded by construction and insensitive to very
	 * long windows, but it is not a transform of the slack of a single insertion, so unlike {@link #LOG}
	 * and {@link #LINEAR} it cannot be computed one insertion at a time.
	 */
	DAY_PARTS;

	/**
	 * @param slack the width of the start time window of one insertion [s]
	 * @param marginalUtility {@code k}, the utility of one reference duration of slack
	 * @param referenceDuration {@code t_typ}, the unit the slack is measured in [s]
	 * @return the flexibility part of the utility, always {@code >= 0}
	 * @throws UnsupportedOperationException for the functions that are not implemented yet
	 */
	public double apply(double slack, double marginalUtility, double referenceDuration) {

		if (slack < 0) {
			throw new IllegalArgumentException("The slack of an insertion cannot be negative: " + slack);
		}

		return switch (this) {
			case LOG -> marginalUtility * Math.log(1 + slack / referenceDuration);
			case LINEAR, DAY_PARTS -> throw new UnsupportedOperationException(
				"The flexibility function " + this + " is not implemented yet, use " + LOG + ".");
		};
	}
}
