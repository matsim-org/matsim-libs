package org.matsim.contrib.drt.estimator.impl.distribution;

import java.util.Random;

public class NormalDistributionGenerator implements DistributionGenerator{
	private final Random random;
	private final double std;

	private final double minValue;

	private final double maxValue;

	public NormalDistributionGenerator(long seed, double std) {
		this.random = new Random(seed);
		this.std = std;
		this.minValue = 0.5;
		this.maxValue = 3.0;
	}

	public NormalDistributionGenerator(long seed, double std, double minValue, double maxValue) {
		this.random = new Random(seed);
		this.std = std;
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	@Override
	public double generateRandomValue() {
		double randomValue = 1 + random.nextGaussian() * std;
		randomValue = Math.min(maxValue, randomValue);
		randomValue = Math.max(minValue, randomValue);
		return randomValue;
	}
}
