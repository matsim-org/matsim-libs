package org.matsim.core.scoring;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source64.XoRoShiRo128PlusPlus;
import org.apache.commons.rng.sampling.distribution.ZigguratSampler;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.router.TripStructureUtils;

import java.util.SplittableRandom;

/**
 * Computes pseudo random errors (epsilons), which are frozen for certain choice situations, thus representing the unobserved heterogeneity.
 */
public final class PseudoRandomScorer {

	/**
	 * Number of random numbers to throw away before starting to use it.
	 */
	private static final int WARMUP_ITERATIONS = 100;

	private final long seed;

	private final PseudoRandomTripError tripScore;
	private final DistributionConfig tripConfig;

	@Inject
	PseudoRandomScorer(PseudoRandomTripError tripScore, Config config,
					   @Named(PseudoRandomScoringModule.TRIP) DistributionConfig tripConfig) {
		this.tripScore = tripScore;
		this.tripConfig = tripConfig;

		SplittableRandom rnd = new SplittableRandom(config.global().getRandomSeed());
		for (int i = 0; i < WARMUP_ITERATIONS; i++) {
			rnd.nextDouble();
		}

		// Create a random seed from the global one
		this.seed = rnd.nextLong();
	}

	/**
	 * Calculates the pseudo random score of a trip.
	 */
	public double scoreTrip(Id<Person> personId, String mainMode, TripStructureUtils.Trip trip) {

		if (tripConfig.distribution() == null || tripConfig.scale() == 0)
			return 0;

		long tripSeed = tripScore.getSeed(personId, mainMode, trip);

		// Need to create a new instance because reusing them will also create a lot of intermediate arrays
		XoRoShiRo128PlusPlus rng = new XoRoShiRo128PlusPlus(seed, tripSeed);
		for (int i = 0; i < WARMUP_ITERATIONS; i++) {
			rng.nextLong();
		}

		return switch (tripConfig.distribution()) {
			case gumbel -> sampleGumbel(rng, 0, tripConfig.scale());
			case normal -> sampleNormal(rng, 0, tripConfig.scale());
			default -> throw new IllegalStateException("Unsupported distribution: " + tripConfig.distribution());
		};
	}


	/**
	 * Sample from a Gumbel distribution.
	 *
	 * @param mu   location parameter
	 * @param beta scale parameter (must be positive)
	 */
	private double sampleGumbel(UniformRandomProvider rng, double mu, double beta) {

		double v = rng.nextDouble();
		if (v < 0.0 || v > 1.0) {
			throw new OutOfRangeException(v, 0.0, 1.0);
		} else if (v == 0) {
			return Double.NEGATIVE_INFINITY;
		} else if (v == 1) {
			return Double.POSITIVE_INFINITY;
		}

		return mu - FastMath.log(-FastMath.log(v)) * beta;
	}

	private double sampleNormal(UniformRandomProvider rng, double mu, double sigma) {
		return mu + ZigguratSampler.NormalizedGaussian.of(rng).sample() * sigma;
	}

}
