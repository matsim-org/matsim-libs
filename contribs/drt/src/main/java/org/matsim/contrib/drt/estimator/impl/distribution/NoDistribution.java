package org.matsim.contrib.drt.estimator.impl.distribution;

public class NoDistribution implements DistributionGenerator{
	@Override
	public double generateRandomValue() {
		return 1.0;
	}
}
