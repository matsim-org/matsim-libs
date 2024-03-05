package org.matsim.contrib.drt.estimator.impl;

import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.utils.misc.OptionalTime;

import java.util.Random;

public class RealisticDrtEstimator implements DrtEstimator {

	private final DistributionGenerator distributionGenerator;

	public RealisticDrtEstimator(DistributionGenerator distributionGenerator) {
		this.distributionGenerator = distributionGenerator;
	}

	@Override
	public Estimate estimate(DrtRoute route, OptionalTime departureTime) {
		double directRideTIme = route.getDirectRideTime();
		double directDistance = route.getDistance();
		double waitTime = distributionGenerator.generateWaitTime();
		double rideTime = distributionGenerator.generateRideTime(directRideTIme);
		double rideDistance = distributionGenerator.generateRideDistance(rideTime, directRideTIme, directDistance);
		double acceptanceRate = distributionGenerator.generateAcceptanceRate();

		return new Estimate(rideDistance, waitTime + rideTime, waitTime, acceptanceRate);
	}


	public static class DistributionGenerator {
		private final Random random = new Random(4711);
		private final double estRideTimeAlpha;
		private final double estRideTimeBeta;
		private final double rideTimeStd;
		private final double estMeanWaitTime;
		private final double waitTimeStd;

		public DistributionGenerator(double estRideTimeAlpha, double estRideTimeBeta, double rideTimeStd, double estMeanWaitTime,
									 double waitTimeStd) {
			this.estRideTimeAlpha = estRideTimeAlpha;
			this.estRideTimeBeta = estRideTimeBeta;
			this.rideTimeStd = rideTimeStd;
			this.estMeanWaitTime = estMeanWaitTime;
			this.waitTimeStd = waitTimeStd;
		}

		public DistributionGenerator generateExampleDistributionGenerator() {
			return new DistributionGenerator(1.5, 300, 0.2, 300, 0.4);
		}

		public double generateRideTime(double directRideTime) {
			// TODO improve this distribution
			double estMeanRideTime = estRideTimeAlpha * directRideTime + estRideTimeBeta;
			return Math.max(directRideTime, estMeanRideTime * (1 + random.nextGaussian() * rideTimeStd));
		}

		public double generateRideDistance(double estRideTime, double directRideTime, double directRideDistance) {
			// TODO Currently, same ratio is used as in the ride time estimation; improve this distribution
			double ratio = estRideTime / directRideTime;
			return ratio * directRideDistance;
		}

		public double generateWaitTime() {
			// TODO improve this distribution
			return Math.max(estMeanWaitTime * (1 + random.nextGaussian() * waitTimeStd), 0);
		}

		public double generateAcceptanceRate() {
			return random.nextDouble();
		}
	}
}
