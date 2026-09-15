package org.matsim.core.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripStructureUtils;

/**
 * Computes a random seed based on person id, origin activity and routing mode.
 */
public final class DefaultPseudoRandomTripError implements PseudoRandomTripError {

	@Override
	public long getSeed(Id<Person> personId, String mainMode, TripStructureUtils.Trip trip) {

		int personHash = personId.toString().hashCode();

		int modeHash = mainMode.hashCode();
		int modeAndActHash = 31 * modeHash + trip.getOriginActivity().getType().hashCode();

		// Combine two integers to long
		return ((long) personHash << 32) | (modeAndActHash & 0xFFFFFFFFL);
	}
}
