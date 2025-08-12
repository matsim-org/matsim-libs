package org.matsim.contribs.discrete_mode_choice.model.utilities;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * The MultinomialLogitSelector collects a set of candidates with given
 * utilities and then selects on according to the multinomial logit model. For
 * each candidate (i) a utility is calculated as:
 *
 * <code>P(i) = exp( Ui ) / Sum( U1 + U2 + ... + Un )</code>
 *
 * For large utilities the exponential terms can exceed the value range of a
 * double. Therefore, the selector has a cutoff value, which is a maximum of
 * 700.0 by default. If this value is reached a warning will be shown.
 *
 * @author sebhoerl
 */
public class MultinomialLogitSelector implements UtilitySelector {
	private final static Logger logger = LogManager.getLogger(MultinomialLogitSelector.class);

	final private List<UtilityCandidate> candidates = new LinkedList<>();

	private final double maximumUtility;
	private final double minimumUtility;
	private final boolean considerMinimumUtility;
	private final boolean writeDetailedUtilities;
	private final Id<Person> personId;
	private final List<DiscreteModeChoiceTrip> tourTrips;
	/**
	 * Creates a MultinomialSelector. The utility cutoff value defines the maximum
	 * utility possible.
	 */
	public MultinomialLogitSelector(double maximumUtility, double minimumUtility, boolean considerMinimumUtility, boolean writeDetailedUtilities,
									Person person, List<DiscreteModeChoiceTrip> tourTrips) {
		this.maximumUtility = maximumUtility;
		this.minimumUtility = minimumUtility;
		this.considerMinimumUtility = considerMinimumUtility;
		this.writeDetailedUtilities = writeDetailedUtilities;
		this.personId = (person!=null)? person.getId(): Id.create("unknown", Person.class);
		this.tourTrips = tourTrips;
	}

	@Override
	public void addCandidate(UtilityCandidate candidate) {
		candidates.add(candidate);
	}

	@Override
	public Optional<UtilityCandidate> select(Random random) {
		// I) If not candidates are available, give back nothing
		if (candidates.isEmpty()) {
			return Optional.empty();
		}

		// II) Filter candidates that have a very low utility
		List<UtilityCandidate> filteredCandidates = candidates;

		if (considerMinimumUtility) {
			filteredCandidates = candidates.stream() //
					.filter(c -> c.getUtility() > minimumUtility) //
					.toList();

			if (filteredCandidates.isEmpty()) {
				logger.warn(String.format(
						"Encountered choice where all utilities were smaller than %f (minimum configured utility)",
						minimumUtility));
				return Optional.empty();
			}
		}

		// III) Create a probability distribution over candidates
		List<Double> density = new ArrayList<>(filteredCandidates.size());

		for (UtilityCandidate candidate : filteredCandidates) {
			double utility = candidate.getUtility();

			// Warn if there is a utility that is exceeding the feasible range
			if (utility > maximumUtility) {
				logger.warn(String.format(
						"Encountered choice where a utility (%f) is larger than %f (maximum configured utility)",
						utility, maximumUtility));
				utility = maximumUtility;
			}

			density.add(Math.exp(utility));
		}

		// IV) Build a cumulative density of the distribution
		List<Double> cumulativeDensity = new ArrayList<>(density.size());
		double totalDensity = 0.0;

		for (Double obs : density) {
			totalDensity += obs;
			cumulativeDensity.add(totalDensity);
		}

		// V) Perform a selection using the CDF
		double pointer = random.nextDouble() * totalDensity;

		int selection = (int) cumulativeDensity.stream().filter(f -> f < pointer).count();
		UtilityCandidate selectedCandidate = filteredCandidates.get(selection);

		// ===== WRITE TO CSV HERE IF REQUESTED IN THE CONFIG =====
		if (writeDetailedUtilities && UtilityWriter.isWriterInitialized()) {
			UtilityWriter.writeCandidate(personId, tourTrips, filteredCandidates, selection);
		}
		// ================== END OF CSV WRITING ==================

		return Optional.of(selectedCandidate);
	}

	public static class Factory implements UtilitySelectorFactory {
		private final double minimumUtility;
		private final double maximumUtility;
		private final boolean considerMinimumUtility;
		private final boolean writeDetailedUtilities;

		public Factory(double minimumUtility, double maximumUtility, boolean considerMinimumUtility, boolean writeDetailedUtilities) {
			this.minimumUtility = minimumUtility;
			this.maximumUtility = maximumUtility;
			this.considerMinimumUtility = considerMinimumUtility;
			this.writeDetailedUtilities = writeDetailedUtilities;
		}

		public Factory(double minimumUtility, double maximumUtility, boolean considerMinimumUtility) {
			this(minimumUtility, maximumUtility, considerMinimumUtility, false);
		}

		@Override
		public MultinomialLogitSelector createUtilitySelector(Person person, List<DiscreteModeChoiceTrip> tourTrips) {
			return new MultinomialLogitSelector(maximumUtility, minimumUtility, considerMinimumUtility, writeDetailedUtilities, person, tourTrips);
		}
	}
}
