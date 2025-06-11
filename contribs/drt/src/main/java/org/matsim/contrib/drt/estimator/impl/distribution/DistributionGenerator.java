package org.matsim.contrib.drt.estimator.impl.distribution;

public interface DistributionGenerator {
	/**
	 * @return relative value to the typical ride duration (i.e., generate a distribution around 1.0)
	 */
	double generateRandomValue();

	enum DistributionType {NO_DISTRIBUTION, NORMAL, LOG_NORMAL, CUSTOM}
}
