package org.matsim.contrib.drt.estimator.impl;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.OptionalTime;

import java.util.Random;

public class EuclideanDistanceBasedDrtEstimator implements DrtEstimator {
	private final Network network;
	/**
	 * For travel distance related scoring (e.g., marginal utility distance), we need estimated network distance:
	 * Estimated network distance = Euclidean distance * network distance factor
	 */
	private final double networkDistanceFactor;
	/**
	 * Slope of the linear regression
	 */
	private final double slope;
	/**
	 * Intercept of the linear regression
	 */
	private final double intercept;

	private final double estimatedMeanWaitTime;

	private final double waitTimeStd;

	private final double mu;
	private final double sigma;
	private final Random random = new Random(1234);

	/**
	 * We use log normal distribution to estimate the ride duration of each individual trip. The distribution
	 * is based on the linear regression.
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
		this.slope = slope;
		this.intercept = intercept;
		this.estimatedMeanWaitTime = estimatedMeanWaitTime;
		this.waitTimeStd = waitTimeStd;
		this.mu = mu;
		this.sigma = sigma;
	}

	@Override
	public Estimate estimate(DrtRoute route, OptionalTime departureTime) {
		Coord fromCoord = network.getLinks().get(route.getStartLinkId()).getToNode().getCoord();
		Coord toCoord = network.getLinks().get(route.getEndLinkId()).getToNode().getCoord();
		double euclideanDistance = CoordUtils.calcEuclideanDistance(fromCoord, toCoord);
		double typicalRideDuration = euclideanDistance * slope + intercept;
		double typicalRideDistance = networkDistanceFactor * euclideanDistance;
		double randomFactor = nextLogNormal(mu, sigma);
		double waitTime = Math.max(estimatedMeanWaitTime * (1 + random.nextGaussian() * waitTimeStd), 0);

		return new Estimate(typicalRideDistance * randomFactor, typicalRideDuration * randomFactor,
			waitTime, 0);
	}

	public double nextLogNormal(double mu, double sigma) {
		if (sigma == 0)
			return Math.exp(mu);

		return Math.exp(sigma * random.nextGaussian() + mu);
	}
}
