package org.matsim.modechoice.replanning;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.modechoice.PlanCandidate;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The MultinomialLogitSelector collects a set of candidates with given
 * utilities and then selects on according to the multinomial logit model. For
 * each candidate (i) a utility is calculated as:
 *
 * <code>P(i) = exp( Ui ) / Sum( U1 + U2 + ... + Un )</code>
 * <p>
 * For large utilities the exponential terms can exceed the value range of a
 * double. Therefore, the selector has a cutoff value, which is a maximum of
 * 700.0 by default. If this value is reached a warning will be shown.
 *
 * @author sebhoerl
 * @author rakow
 */
public class MultinomialLogitSelector implements Selector<PlanCandidate> {

	private static final double MAX_UTILITY = 700d;

	private final static Logger log = LogManager.getLogger(MultinomialLogitSelector.class);
	private final double scale;
	private final Random rnd;

	public MultinomialLogitSelector(double scale, Random rnd) {
		this.scale = scale;
		this.rnd = rnd;
	}

	@Nullable
	@Override
	public PlanCandidate select(Collection<PlanCandidate> candidates) {

		if (candidates.isEmpty())
			return null;

		// III) Create a probability distribution over candidates
		DoubleList density = new DoubleArrayList(candidates.size());
		List<PlanCandidate> pcs = new ArrayList<>(candidates);

		for (PlanCandidate candidate : pcs) {
			double utility = candidate.getUtility();

			// if there is a min and max, choose either randomly
			if (utility != candidate.getMinUtility()) {
				utility = rnd.nextBoolean() ? utility : candidate.getMinUtility();
			}

			// Warn if there is a utility that is exceeding the feasible range
			if (utility > MAX_UTILITY) {
				log.warn(String.format(
						"Encountered choice where a utility (%f) is larger than %f (maximum configured utility)",
						utility, MAX_UTILITY));
				utility = MAX_UTILITY;
			}

			density.add(Math.exp(utility * scale));
		}

		// IV) Build a cumulative density of the distribution
		DoubleList cumulativeDensity = new DoubleArrayList(density.size());
		double totalDensity = 0.0;

		for (int i = 0; i < density.size(); i++) {
			totalDensity += density.getDouble(i);
			cumulativeDensity.add(totalDensity);
		}

		// V) Perform a selection using the CDF
		double pointer = rnd.nextDouble() * totalDensity;

		int selection = (int) cumulativeDensity.doubleStream().filter(f -> f < pointer).count();
		return pcs.get(selection);
	}

}
