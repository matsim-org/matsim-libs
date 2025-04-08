package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

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
			// The total capacity of parking spots is 0. As fallback, we assume that the occupancy rate is 1, so each link is full.
			return alpha * Math.exp(-beta);
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
