package org.matsim.contrib.drt.estimator.impl.distribution;

import java.util.Random;

public class LogNormalDistributionGenerator implements DistributionGenerator {
	private final Random random;
	private final double mu;
	private final double sigma;
	private final double minValue;

	private final double maxValue;

	public LogNormalDistributionGenerator(long seed, double mu, double sigma) {
		this.random = new Random(seed);
		this.mu = mu;
		this.sigma = sigma;
		this.minValue = 0.5;
		this.maxValue = 3;
	}

	public LogNormalDistributionGenerator(long seed, double mu, double sigma, double minValue, double maxValue) {
		this.random = new Random(seed);
		this.mu = mu;
		this.sigma = sigma;
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	@Override
	public double generateRandomValue() {
		if (sigma == 0)
			return Math.exp(mu);
		return Math.max(Math.min(Math.exp(sigma * random.nextGaussian() + mu), maxValue), minValue);
	}
}
