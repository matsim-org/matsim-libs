package org.matsim.contrib.drt.estimator.impl;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.impl.acceptance_estimation.RejectionRateEstimator;
import org.matsim.contrib.drt.estimator.impl.acceptance_estimation.UniformRejectionEstimator;
import org.matsim.contrib.drt.estimator.impl.distribution.DistributionGenerator;
import org.matsim.contrib.drt.estimator.impl.distribution.LogNormalDistributionGenerator;
import org.matsim.contrib.drt.estimator.impl.distribution.NoDistribution;
import org.matsim.contrib.drt.estimator.impl.distribution.NormalDistributionGenerator;
import org.matsim.contrib.drt.estimator.impl.trip_estimation.ConstantRideDurationEstimator;
import org.matsim.contrib.drt.estimator.impl.trip_estimation.RideDurationEstimator;
import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.ConstantWaitingTimeEstimator;
import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.WaitingTimeEstimator;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.OptionalTime;

public class EuclideanDistanceBasedDrtEstimator implements DrtEstimator {
	private final Network network;
	/**
	 * For travel distance related scoring (e.g., marginal utility distance), we need estimated network distance:
	 * Estimated network distance = Euclidean distance * network distance factor
	 */
	private final double networkDistanceFactor;
	private final RideDurationEstimator rideDurationEstimator;
	private final WaitingTimeEstimator waitingTimeEstimator;
	private final DistributionGenerator rideDurationDistributionGenerator;
	private final DistributionGenerator waitingTimeDistributionGenerator;
	private final RejectionRateEstimator rejectionRateEstimator;

	private EuclideanDistanceBasedDrtEstimator(Network network, double networkDistanceFactor, RideDurationEstimator rideDurationEstimator,
											  WaitingTimeEstimator waitingTimeEstimator, DistributionGenerator rideTimeDistributionGenerator,
											  DistributionGenerator waitingTimeDistributionGenerator, RejectionRateEstimator rejectionRateEstimator) {
		this.network = network;
		this.networkDistanceFactor = networkDistanceFactor;
		this.rideDurationEstimator = rideDurationEstimator;
		this.waitingTimeEstimator = waitingTimeEstimator;
		this.rideDurationDistributionGenerator = rideTimeDistributionGenerator;
		this.waitingTimeDistributionGenerator = waitingTimeDistributionGenerator;
		this.rejectionRateEstimator = rejectionRateEstimator;
	}

	public static class Builder {
		private final Network network;
		private final double networkDistanceFactor;
		private RideDurationEstimator rideDurationEstimator = new ConstantRideDurationEstimator(0.158, 103);
		private WaitingTimeEstimator waitingTimeEstimator = new ConstantWaitingTimeEstimator(300);
		private DistributionGenerator waitingTimeDistributionGenerator = new NoDistribution();
		private DistributionGenerator rideTimeDistributionGenerator = new NoDistribution();
		private RejectionRateEstimator rejectionRateEstimator = new UniformRejectionEstimator(0.);

		public Builder(Network network, double networkDistanceFactor) {
			this.network = network;
			this.networkDistanceFactor = networkDistanceFactor;
		}

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

		Builder setRejectionRateEstimator(RejectionRateEstimator rejectionRateEstimator) {
			this.rejectionRateEstimator = rejectionRateEstimator;
			return this;
		}

		public EuclideanDistanceBasedDrtEstimator build() {
			return new EuclideanDistanceBasedDrtEstimator(network, networkDistanceFactor, rideDurationEstimator, waitingTimeEstimator,
				rideTimeDistributionGenerator, waitingTimeDistributionGenerator, rejectionRateEstimator);
		}
	}

	@Override
	public Estimate estimate(DrtRoute route, OptionalTime departureTime) {
		Coord fromCoord = network.getLinks().get(route.getStartLinkId()).getToNode().getCoord();
		Coord toCoord = network.getLinks().get(route.getEndLinkId()).getToNode().getCoord();
		double euclideanDistance = CoordUtils.calcEuclideanDistance(fromCoord, toCoord);

		double typicalRideDuration = rideDurationEstimator.getEstimatedRideDuration(route.getStartLinkId(), route.getEndLinkId(), departureTime, euclideanDistance);
		double typicalRideDistance = networkDistanceFactor * euclideanDistance;
		double typicalWaitingTime = waitingTimeEstimator.estimateWaitTime(route.getStartLinkId(), route.getEndLinkId(), departureTime);
		double randomFactor = rideDurationDistributionGenerator.generateRandomValue();
		double waitTime = Math.max(typicalWaitingTime * waitingTimeDistributionGenerator.generateRandomValue(), 0);
		double rejectionRate = rejectionRateEstimator.getEstimatedProbabilityOfRejection(route.getStartLinkId(), route.getEndLinkId(), departureTime);

		return new Estimate(typicalRideDistance * randomFactor, typicalRideDuration * randomFactor,
			waitTime, rejectionRate);
	}
	
	/** Example Euclidean distance DRT estimator:
	 * We use log normal distribution to estimate the ride duration of each individual trip. The distribution
	 * is based on the linear regression.
	 *
	 * @params networkDistanceFactor: Estimated network distance = Euclidean distance * network distance factor
	 * @params slope: slope for the linear regression
	 * @params intercept: intercept for linear regression
	 * @params mu: mu for log normal distribution
	 * @params sigma: sigma for log normal distribution.
	 */
	public EuclideanDistanceBasedDrtEstimator euclideanDistanceBasedDrtEstimatorWithLogNormalDistribution(Network network, double networkDistanceFactor, double slope,
											  double intercept, double typicalWaitTime, double waitTimeStd,
											  double mu, double sigma, double probabilityOfRejection) {
		return new Builder(network, networkDistanceFactor)
			.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(typicalWaitTime))
			.setWaitingTimeDistributionGenerator(new NormalDistributionGenerator(2, waitTimeStd))
			.setRideDurationEstimator(new ConstantRideDurationEstimator(slope, intercept))
			.setRideDurationDistributionGenerator(new LogNormalDistributionGenerator(2, mu, sigma))
			.setRejectionRateEstimator(new UniformRejectionEstimator(probabilityOfRejection))
			.build();
	}
}
