package org.matsim.contrib.drt.estimator.impl;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.impl.distribution.DistributionGenerator;
import org.matsim.contrib.drt.estimator.impl.distribution.NormalDistributionGenerator;
import org.matsim.contrib.drt.estimator.impl.trip_estimation.ConstantTripEstimator;
import org.matsim.contrib.drt.estimator.impl.trip_estimation.TripEstimator;
import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.ConstantWaitingTimeEstimator;
import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.WaitingTimeEstimator;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * DRT estimator that uses available data (e.g., real-world operational data, simulation-based data) to provide estimated data for DRT trips.
 */
public final class NetworkBasedDrtEstimator implements DrtEstimator {
	private final TripEstimator rideDurationEstimator;
	private final WaitingTimeEstimator waitingTimeEstimator;
	private final DistributionGenerator waitingTimeDistributionGenerator;
	private final DistributionGenerator rideTimeDistributionGenerator;

	static class Builder {
		private TripEstimator rideDurationEstimator;
		private WaitingTimeEstimator waitingTimeEstimator;
		private DistributionGenerator waitingTimeDistributionGenerator;
		private DistributionGenerator rideTimeDistributionGenerator;

		Builder setRideDurationEstimator(TripEstimator rideDurationEstimator) {
			this.rideDurationEstimator = rideDurationEstimator;
			return this;
		}

		Builder setWaitingTimeEstimator(WaitingTimeEstimator waitingTimeEstimator) {
			this.waitingTimeEstimator = waitingTimeEstimator;
			return this;
		}

		Builder setRideDurationDistributionGenerator(DistributionGenerator rideTimeDistributionGenerator) {
			this.rideTimeDistributionGenerator = rideTimeDistributionGenerator;
			return this;
		}

		Builder setWaitingTimeDistributionGenerator(DistributionGenerator waitingTimeDistributionGenerator) {
			this.waitingTimeDistributionGenerator = waitingTimeDistributionGenerator;
			return this;
		}

		NetworkBasedDrtEstimator build() {
			return new NetworkBasedDrtEstimator(rideDurationEstimator, waitingTimeEstimator, rideTimeDistributionGenerator, waitingTimeDistributionGenerator);
		}

	}

	public NetworkBasedDrtEstimator(TripEstimator rideDurationEstimator, WaitingTimeEstimator waitingTimeEstimator,
									 DistributionGenerator rideTimeDistribution, DistributionGenerator waitTimeDistribution) {
		this.rideDurationEstimator = rideDurationEstimator;
		this.waitingTimeEstimator = waitingTimeEstimator;
		this.rideTimeDistributionGenerator = rideTimeDistribution;
		this.waitingTimeDistributionGenerator = waitTimeDistribution;
	}

	/**
	 * Example DRT estimator based on the normal distributed ride time and waiting time
	 * @param estRideTimeAlpha typical ride duration = alpha * direct ride time + beta, alpha is specified here
	 * @param estRideTimeBeta typical ride duration = alpha * direct ride time + beta, beta is specified here
	 * @param rideTimeStd standard deviation of ride duration (normalized to 1)
	 * @param estMeanWaitTime estimated waiting time (i.e., mean wait time)
	 * @param waitTimeStd standard deviation of waiting time (normalized to 1)
	 * @return NetworkBasedDrtEstimator
	 */
	public static NetworkBasedDrtEstimator normalDistributed(double estRideTimeAlpha, double estRideTimeBeta, double rideTimeStd, double estMeanWaitTime,
															 double waitTimeStd) {
		return new Builder()
			.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(estMeanWaitTime))
			.setRideDurationEstimator(new ConstantTripEstimator(estRideTimeAlpha, estRideTimeBeta))
			.setWaitingTimeDistributionGenerator(new NormalDistributionGenerator(4711, waitTimeStd))
			.setRideDurationDistributionGenerator(new NormalDistributionGenerator(4711, rideTimeStd))
			.build();
	}

	@Override
	public Estimate estimate(DrtRoute route, OptionalTime departureTime) {
		double directRideTIme = route.getDirectRideTime();
		double directDistance = route.getDistance();
		Id<Link> fromLinkId = route.getStartLinkId();
		Id<Link> toLinkId = route.getEndLinkId();
		Tuple<Double, Double> alphaBetaTuple = rideDurationEstimator.getAlphaBetaValues(fromLinkId, toLinkId, departureTime);
		double alpha = alphaBetaTuple.getFirst();
		double beta = alphaBetaTuple.getSecond();
		double typicalRideDuration = directRideTIme * alpha + beta;
		double typicalRideDistance = directDistance * alpha + beta;
		double typicalWaitingTime = waitingTimeEstimator.estimateWaitTime(fromLinkId, toLinkId, departureTime);

		double estimatedWaitingTime = typicalWaitingTime * waitingTimeDistributionGenerator.generateRandomValue();

		double detourRandomFactor = rideTimeDistributionGenerator.generateRandomValue();
		double estimatedRideDuration = detourRandomFactor * typicalRideDuration;
		double estimatedRideDistance = detourRandomFactor * typicalRideDistance;

		double acceptanceRate = 1.0;

		return new Estimate(estimatedRideDistance, estimatedRideDuration, estimatedWaitingTime, acceptanceRate);
	}

}
