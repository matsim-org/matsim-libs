package org.matsim.contribs.discrete_mode_choice.model.utilities;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * The maximum utility selector collects a set of candidates with a given
 * utility value and then selects the one with the highest utility. Internally,
 * always only the best candidate is held.
 *
 * @author sebhoerl
 */
public class MaximumSelector implements UtilitySelector {
	private UtilityCandidate bestCandidate = null;

	@Override
	public void addCandidate(UtilityCandidate candidate) {
		if (bestCandidate == null) {
			bestCandidate = candidate;
		} else if (candidate.getUtility() > bestCandidate.getUtility()) {
			bestCandidate = candidate;
		}
	}

	@Override
	public Optional<UtilityCandidate> select(Random random) {
		if (bestCandidate == null) {
			return Optional.empty();
		}

		return Optional.of(bestCandidate);
	}

	public static class Factory implements UtilitySelectorFactory {
		@Override
		public UtilitySelector createUtilitySelector(Person person, List<DiscreteModeChoiceTrip> tourTrips) {
			return new MaximumSelector();
		}
	}
}
