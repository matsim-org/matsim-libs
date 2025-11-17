package org.matsim.contribs.discrete_mode_choice.model.estimation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

/**
 * This trip estimator wraps another TripEstimator for which it caches results.
 * One can configure which modes should be cached. This way, for instance when a
 * trip would be estimates many times, e.g. in a tour-based model, one makes
 * sure that this only happens once.
 *
 * @author sebhoerl
 */
public class CachedTripEstimator implements TripEstimator {
	final private Map<String, Map<DiscreteModeChoiceTrip, Map<Long, TripCandidate>>> cache = new HashMap<>();
	final private TripEstimator delegate;

	public CachedTripEstimator(TripEstimator delegate, Collection<String> cachedModes) {
		this.delegate = delegate;

		for (String mode : cachedModes) {
			cache.put(mode, new HashMap<>());
		}
	}

	@Override
	public TripCandidate estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> preceedingTrips) {
		Map<DiscreteModeChoiceTrip, Map<Long, TripCandidate>> modeCache = cache.get(mode);

		if (modeCache != null) {
			Long departureTime = Double.valueOf(Math.ceil(trip.getDepartureTime())).longValue();
			TripCandidate candidate = modeCache.computeIfAbsent(trip, t -> new HashMap<>()).get(departureTime);

			if (candidate == null) {
				candidate = delegate.estimateTrip(person, mode, trip, preceedingTrips);
				modeCache.get(trip).put(departureTime, candidate);
			}

			return candidate;
		} else {
			return delegate.estimateTrip(person, mode, trip, preceedingTrips);
		}
	}
}
