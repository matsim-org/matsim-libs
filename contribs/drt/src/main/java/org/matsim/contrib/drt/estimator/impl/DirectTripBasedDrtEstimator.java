package org.matsim.contrib.drt.estimator.impl;

import org.checkerframework.checker.units.qual.C;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.impl.distribution.DistributionGenerator;
import org.matsim.contrib.drt.estimator.impl.distribution.LogNormalDistributionGenerator;
import org.matsim.contrib.drt.estimator.impl.distribution.NoDistribution;
import org.matsim.contrib.drt.estimator.impl.distribution.NormalDistributionGenerator;
import org.matsim.contrib.drt.estimator.impl.trip_estimation.ConstantRideDurationEstimator;
import org.matsim.contrib.drt.estimator.impl.trip_estimation.RideDurationEstimator;
import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.ConstantWaitingTimeEstimator;
import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.WaitingTimeEstimator;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * DRT estimator that uses available data (e.g., real-world operational data, simulation-based data) to provide estimated data for DRT trips.
 */
public final class DirectTripBasedDrtEstimator implements DrtEstimator {
	private final RideDurationEstimator rideDurationEstimator;
	private final WaitingTimeEstimator waitingTimeEstimator;
	private final DistributionGenerator waitingTimeDistributionGenerator;
	private final DistributionGenerator rideTimeDistributionGenerator;

	public static class Builder {
		// Initialize with default estimation
		private RideDurationEstimator rideDurationEstimator = new ConstantRideDurationEstimator(1.25, 300);
		private WaitingTimeEstimator waitingTimeEstimator = new ConstantWaitingTimeEstimator(300);
		private DistributionGenerator waitingTimeDistributionGenerator = new NoDistribution();
		private DistributionGenerator rideTimeDistributionGenerator = new NoDistribution();

		public Builder setRideDurationEstimator(RideDurationEstimator rideDurationEstimator) {
			this.rideDurationEstimator = rideDurationEstimator;
			return this;
		}

		public Builder setWaitingTimeEstimator(WaitingTimeEstimator waitingTimeEstimator) {
			this.waitingTimeEstimator = waitingTimeEstimator;
			return this;
		}

		public Builder setRideDurationDistributionGenerator(DistributionGenerator rideTimeDistributionGenerator) {
			this.rideTimeDistributionGenerator = rideTimeDistributionGenerator;
			return this;
		}

		public Builder setWaitingTimeDistributionGenerator(DistributionGenerator waitingTimeDistributionGenerator) {
			this.waitingTimeDistributionGenerator = waitingTimeDistributionGenerator;
			return this;
		}

		public DirectTripBasedDrtEstimator build() {
			return new DirectTripBasedDrtEstimator(rideDurationEstimator, waitingTimeEstimator, rideTimeDistributionGenerator, waitingTimeDistributionGenerator);
		}

	}

	public DirectTripBasedDrtEstimator(RideDurationEstimator rideDurationEstimator, WaitingTimeEstimator waitingTimeEstimator,
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
	public static DirectTripBasedDrtEstimator normalDistributedNetworkBasedDrtEstimator(double estRideTimeAlpha, double estRideTimeBeta,
																						double rideTimeStd, double estMeanWaitTime,
																						double waitTimeStd) {
		return new Builder()
			.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(estMeanWaitTime))
			.setRideDurationEstimator(new ConstantRideDurationEstimator(estRideTimeAlpha, estRideTimeBeta))
			.setWaitingTimeDistributionGenerator(new NormalDistributionGenerator(1, waitTimeStd))
			.setRideDurationDistributionGenerator(new NormalDistributionGenerator(2, rideTimeStd))
			.build();
	}

	/**
	 * Example DRT estimator based on the log-normal distributed ride time and normal distributed waiting time
	 * @param estRideTimeAlpha typical ride duration = alpha * direct ride time + beta, alpha is specified here
	 * @param estRideTimeBeta typical ride duration = alpha * direct ride time + beta, beta is specified here
	 * @param mu log-normal distribution parameter for ride duration (normalized to typical ride duration)
	 * @param sigma log-normal distribution parameter for ride duration (normalized to typical ride duration)
	 * @param estMeanWaitTime estimated waiting time (i.e., mean wait time)
	 * @param waitTimeStd standard deviation of waiting time (normalized to 1)
	 * @return NetworkBasedDrtEstimator
	 */
	public static DirectTripBasedDrtEstimator mixDistributedNetworkBasedDrtEstimator(double estRideTimeAlpha, double estRideTimeBeta,
																					 double mu, double sigma, double estMeanWaitTime,
																					 double waitTimeStd) {
		return new Builder()
			.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(estMeanWaitTime))
			.setRideDurationEstimator(new ConstantRideDurationEstimator(estRideTimeAlpha, estRideTimeBeta))
			.setWaitingTimeDistributionGenerator(new NormalDistributionGenerator(1, waitTimeStd))
			.setRideDurationDistributionGenerator(new LogNormalDistributionGenerator(2, mu, sigma))
			.build();
	}

	@Override
	public Estimate estimate(DrtRoute route, OptionalTime departureTime) {
		double directRideTIme = route.getDirectRideTime();
		double directDistance = route.getDistance();
		Id<Link> fromLinkId = route.getStartLinkId();
		Id<Link> toLinkId = route.getEndLinkId();
		double typicalRideDuration = rideDurationEstimator.getEstimatedRideDuration(fromLinkId, toLinkId, departureTime, directRideTIme);
		double typicalRideDistance = (typicalRideDuration / directRideTIme) * directDistance;
		double typicalWaitingTime = waitingTimeEstimator.estimateWaitTime(fromLinkId, toLinkId, departureTime);

		double estimatedWaitingTime = typicalWaitingTime * waitingTimeDistributionGenerator.generateRandomValue();

		double detourRandomFactor = rideTimeDistributionGenerator.generateRandomValue();
		double estimatedRideDuration = detourRandomFactor * typicalRideDuration;
		double estimatedRideDistance = detourRandomFactor * typicalRideDistance;

		double acceptanceRate = 1.0;

		return new Estimate(estimatedRideDistance, estimatedRideDuration, estimatedWaitingTime, acceptanceRate);
	}

}
