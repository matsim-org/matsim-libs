package org.matsim.contrib.drt.estimator.impl;

import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.utils.misc.OptionalTime;

import java.util.Random;

/**
 * A simple DRT estimator that uses normal distributions to estimate the ride time, wait time, ride distance and acceptance.
 */
public final class DetourBasedDrtEstimator implements DrtEstimator {

	private final NormalDistributionGenerator distributionGenerator;

	private DetourBasedDrtEstimator(double estRideTimeAlpha, double estRideTimeBeta, double rideTimeStd, double estMeanWaitTime,
								   double waitTimeStd) {
		this.distributionGenerator = new NormalDistributionGenerator(estRideTimeAlpha, estRideTimeBeta, rideTimeStd, estMeanWaitTime, waitTimeStd);
	}

	public static DetourBasedDrtEstimator normalDistributed(double estRideTimeAlpha, double estRideTimeBeta, double rideTimeStd, double estMeanWaitTime,
																	  double waitTimeStd) {
		return new DetourBasedDrtEstimator(estRideTimeAlpha, estRideTimeBeta, rideTimeStd, estMeanWaitTime, waitTimeStd);
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

	private static class NormalDistributionGenerator {
		private final Random random = new Random(4711);
		private final double estRideTimeAlpha;
		private final double estRideTimeBeta;
		private final double rideTimeStd;
		private final double estMeanWaitTime;
		private final double waitTimeStd;

		public NormalDistributionGenerator(double estRideTimeAlpha, double estRideTimeBeta, double rideTimeStd, double estMeanWaitTime,
									 double waitTimeStd) {
			this.estRideTimeAlpha = estRideTimeAlpha;
			this.estRideTimeBeta = estRideTimeBeta;
			this.rideTimeStd = rideTimeStd;
			this.estMeanWaitTime = estMeanWaitTime;
			this.waitTimeStd = waitTimeStd;
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
			return 1;
		}
	}
}
