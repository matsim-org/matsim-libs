package org.matsim.contrib.parking.parkingsearchparameterization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;

import java.util.Map;

/**
 * Representation of the Belloche parking search time function.
 * The Belloche parking search time function is defined as:
 * f(occ, K) = alpha * exp(-beta * (occ / K))
 */
public class BellochePenaltyFunction implements ParkingSearchTimeFunction {
	private static final Logger log = LogManager.getLogger(BellochePenaltyFunction.class);
	private final double alpha;
	private final double beta;

	public BellochePenaltyFunction(double alpha, double beta) {
		this.alpha = alpha;
		this.beta = beta;
	}

	@Override
	public double calculateParkingSearchTime(Map<Id<Link>, ParkingCount> parkingCount) {
		Tuple<Double, Double> weightedOccK = getWeightedOccK(parkingCount);

		if (weightedOccK.getSecond() == 0) {
			// No parking capacity exists in the considered area. This may occur when the
			// destination link does not support parking, for example on a motorway.
			// In that case, parking search is considered not applicable and no penalty is applied.
			// To model parking scarcity, provide an explicit positive parking capacity.
			// A parking-free area must be represented by relocating the parking destination
			// to a link outside that area. This function does not perform that relocation.
			// Once relocated, the search-time penalty is calculated from the parking supply
			// and occupancy around the actual parking link. Therefore, returning zero here is
			// appropriate when the considered kernel contains no applicable parking capacity.
			return 0.0;
		}

		return alpha * Math.exp(-beta * (weightedOccK.getFirst() / weightedOccK.getSecond()));
	}

	private Tuple<Double, Double> getWeightedOccK(Map<Id<Link>, ParkingCount> parkingCount) {
		double weightedOcc = 0;
		double weightedK = 0;
		for (ParkingCount pc : parkingCount.values()) {
			weightedOcc += pc.occupancy() * pc.weight();
			weightedK += pc.capacity() * pc.weight();
		}
		return new Tuple<>(weightedOcc, weightedK);
	}
}
