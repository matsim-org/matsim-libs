package org.matsim.contrib.drt.estimator.impl.distribution;

public interface DistributionGenerator {
	/**
	 * @return relative value to the typical ride duration
	 */
	double generateRandomValue();

	enum DistributionType {NORMAL, LOG_NORMAL, POISSON, CUSTOM}
}
