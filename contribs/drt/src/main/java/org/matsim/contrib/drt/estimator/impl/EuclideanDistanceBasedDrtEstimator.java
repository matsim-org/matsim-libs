package org.matsim.contrib.drt.estimator.impl;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.impl.distribution.DistributionGenerator;
import org.matsim.contrib.drt.estimator.impl.distribution.LogNormalDistributionGenerator;
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


	/**
	 * We use log normal distribution to estimate the ride duration of each individual trip. The distribution
	 * is based on the linear regression.
	 *
	 * @params networkDistanceFactor: Estimated network distance = Euclidean distance * network distance factor
	 * @params slope: slope for the linear regression
	 * @params intercept: intercept for linear regression
	 * @params mu: mu for log normal distribution
	 * @params sigma: sigma for log normal distribution.
	 */
	public EuclideanDistanceBasedDrtEstimator(Network network, double networkDistanceFactor, double slope,
											  double intercept, double estimatedMeanWaitTime, double waitTimeStd,
											  double mu, double sigma) {
		this.network = network;
		this.networkDistanceFactor = networkDistanceFactor;
		this.rideDurationEstimator = new ConstantRideDurationEstimator(slope, intercept);
		this.waitingTimeEstimator = new ConstantWaitingTimeEstimator(estimatedMeanWaitTime);
		this.rideDurationDistributionGenerator = new LogNormalDistributionGenerator(1, mu, sigma);
		this.waitingTimeDistributionGenerator = new NormalDistributionGenerator(2, waitTimeStd);
	}

	public EuclideanDistanceBasedDrtEstimator(Network network, double networkDistanceFactor, RideDurationEstimator rideDurationEstimator,
											  WaitingTimeEstimator waitingTimeEstimator, DistributionGenerator rideDurationDistributionGenerator,
											  DistributionGenerator waitingTimeDistributionGenerator) {
		this.network = network;
		this.networkDistanceFactor = networkDistanceFactor;
		this.rideDurationEstimator = rideDurationEstimator;
		this.waitingTimeEstimator = waitingTimeEstimator;
		this.rideDurationDistributionGenerator = rideDurationDistributionGenerator;
		this.waitingTimeDistributionGenerator = waitingTimeDistributionGenerator;
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

		return new Estimate(typicalRideDistance * randomFactor, typicalRideDuration * randomFactor,
			waitTime, 0);
	}

}
