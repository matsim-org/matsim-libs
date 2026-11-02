package org.matsim.contrib.msp.capability.measure;

import org.matsim.contrib.msp.capability.CapabilityConfigGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the facilities one person can reach into single numbers: how much choice the person actually
 * has.
 * <p>
 * A logsum is the expected maximum utility of a set of alternatives and is what a discrete choice model
 * gives for "how good is this choice set". It rises both when the alternatives get better and when there
 * are more of them, which is why it, and not a plain count, is the natural answer to "how capable is
 * this person". See Ben-Akiva and Lerman (1985) for the derivation, Small and Rosen (1981) for its
 * welfare reading, and {@code contribs/accessibility} for the same shape used as an accessibility
 * measure.
 * <p>
 * <b>What beta does.</b> Every logsum here has the form {@code (1/beta) * ln sum_j exp(beta * V_j)},
 * which interpolates between two extremes:
 * <ul>
 *     <li>{@code beta -> infinity}: the logsum becomes {@code max_j V_j}. A further alternative adds
 *     nothing - the alternatives are perfect substitutes.</li>
 *     <li>{@code beta -> 0}: it grows without bound, like {@code mean(V) + ln(N)/beta}. Note that this
 *     is <i>not</i> a sum of utilities; it is the count of alternatives taking over.</li>
 * </ul>
 * The useful way to read it: <b>{@code N} identical alternatives of value {@code V} are worth
 * {@code V + ln(N)/beta}</b>. So beta is set by deciding what a duplicate alternative should be worth:
 * a second identical one adds {@code ln(2)/beta}, i.e. 0.69 utils at {@code beta = 1}, 0.17 at
 * {@code beta = 4} and 0.09 at {@code beta = 8}. A nest with a single member returns that member's
 * utility exactly, so nothing is inflated by being alone.
 */
public final class CapabilityLogsums {

	private CapabilityLogsums() {
	}

	/**
	 * One facility as the logsum sees it: what it is worth to the person, and which kind of place it is.
	 *
	 * @param subType the sub-type of the facility, e.g. the brand of the supermarket. Empty or null if
	 * the facilities do not say, in which case the facility goes into the shared "unknown" nest - see
	 * {@link #nestedUtilityLogsum}.
	 * @param utility {@code u_potential,max} of that facility, i.e. what its best realizable insertion
	 * is worth
	 */
	public record Alternative(String subType, double utility) {

		private static final String UNKNOWN = "";

		private String nest() {
			return this.subType == null || this.subType.isBlank() ? UNKNOWN : this.subType;
		}
	}

	/**
	 * The comprehensive measure {@code u*}: a two level nested logsum over what the person can do, not
	 * just over how far they have to go.
	 * <pre>
	 *   u'_k = (1 / b_1) * ln sum_{f in k} exp(b_1 * u_f)      within one sub-type, e.g. one brand
	 *   u*   = (1 / b_2) * ln sum_k       exp(b_2 * u'_k)      across the sub-types
	 * </pre>
	 * The nesting is what makes this more than a flat logsum: two branches of the same chain are close
	 * substitutes and should not count as two independent options, while two <i>different</i> brands
	 * genuinely widen the choice. {@code b_1 > b_2} is both what expresses that and exactly the
	 * condition for the nested logit to be consistent with random utility maximisation (McFadden 1978,
	 * Daly and Zachary 1978): the dissimilarity parameter is {@code b_2 / b_1}, which has to lie in
	 * {@code (0, 1]}.
	 * <p>
	 * Facilities whose sub-type is unknown share <b>one</b> nest. Giving each of them a nest of its own
	 * would make missing data look like variety, which is the one reading this measure must not have.
	 *
	 * @return {@code u*}, or {@link Double#NaN} if nothing is reachable
	 * @see CapabilityConfigGroup#getBetaWithinSubType()
	 * @see CapabilityConfigGroup#getBetaAcrossSubType()
	 */
	public static double nestedUtilityLogsum(List<Alternative> alternatives, CapabilityConfigGroup config) {
		return nestedUtilityLogsum(alternatives, config.getBetaWithinSubType(), config.getBetaAcrossSubType());
	}

	/**
	 * @param betaWithin {@code b_1}, the scale within one sub-type. Has to be larger than
	 * {@code betaAcross}.
	 * @param betaAcross {@code b_2}, the scale across the sub-types
	 * @see #nestedUtilityLogsum(List, CapabilityConfigGroup)
	 */
	public static double nestedUtilityLogsum(List<Alternative> alternatives, double betaWithin, double betaAcross) {

		if (alternatives.isEmpty()) {
			return Double.NaN;
		}

		Map<String, List<Double>> nests = new LinkedHashMap<>();
		for (Alternative alternative : alternatives) {
			nests.computeIfAbsent(alternative.nest(), key -> new ArrayList<>()).add(alternative.utility());
		}

		double[] nestUtilities = new double[nests.size()];
		int i = 0;
		for (List<Double> utilities : nests.values()) {
			nestUtilities[i++] = logsum(utilities.stream().mapToDouble(Double::doubleValue).toArray(), betaWithin);
		}

		return logsum(nestUtilities, betaAcross);
	}

	/**
	 * The accessibility measure of {@code contribs/accessibility}: a logsum over the <i>cost</i> of
	 * reaching the facilities, ignoring what is on offer there.
	 * <pre>
	 *   (1 / theta) * ln sum_f exp(-theta * c_f)
	 * </pre>
	 * Larger is better: a further reachable facility always adds, and a cheaper one adds more. Kept
	 * next to {@code u*} because it is the number the accessibility literature reports, which makes it
	 * the natural point of comparison - but it says nothing about <i>what</i> the person can do there,
	 * and nothing about flexibility.
	 *
	 * @param costs {@code c_insertion} of the best insertion per facility [utils]
	 * @return the cost logsum, or {@link Double#NaN} if nothing is reachable
	 */
	public static double costLogsum(double[] costs, CapabilityConfigGroup config) {
		if (costs.length == 0) {
			return Double.NaN;
		}
		double theta = config.getCostLogsumScale();
		double[] utilities = new double[costs.length];
		for (int i = 0; i < costs.length; i++) {
			utilities[i] = -theta * costs[i];
		}
		return logsum(utilities, theta);
	}

	/**
	 * {@code (1/beta) * ln sum_j exp(beta * V_j)}, shifted by the maximum so that large utilities do not
	 * overflow.
	 *
	 * @throws IllegalArgumentException if beta is not positive
	 */
	public static double logsum(double[] values, double beta) {

		if (beta <= 0) {
			throw new IllegalArgumentException("A logsum scale has to be positive, got " + beta
				+ ". beta -> 0 is the degenerate case in which the number of alternatives takes over "
				+ "completely and the logsum diverges.");
		}
		if (values.length == 0) {
			return Double.NaN;
		}

		double max = Double.NEGATIVE_INFINITY;
		for (double value : values) {
			max = Math.max(max, beta * value);
		}
		if (Double.isInfinite(max)) {
			return max / beta;
		}

		double sum = 0;
		for (double value : values) {
			sum += Math.exp(beta * value - max);
		}
		return (max + Math.log(sum)) / beta;
	}
}
