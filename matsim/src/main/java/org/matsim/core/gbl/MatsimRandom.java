/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimRandom.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.gbl;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class provides a global instance of {@link Random}. The seed of this random number generator can be reset using the {@link #reset(long)} method.
 * The {@link #reset(long)} method is designed to be used in between iterations, so that random number sequences vary between iterations.
 * <p>
 * This class also provides different variants of the {@link #getLocalInstance()}, {@link #getLocalInstance(long)} function. These functions create new instances of {@link Random} and
 * should be used to obtain Random Number Generators (RNG) in concurrent settings. This class maintains an internal counter so that each {@link Random}
 * instance obtained with {@link #getLocalInstance()} receives a distinct random seed.
 *
 * @author mrieser
 */
public abstract class MatsimRandom {
	private static final long DEFAULT_RANDOM_SEED = 4711;

	private static long globalSeed = DEFAULT_RANDOM_SEED;
	private static AtomicInteger internalCounter = new AtomicInteger(0);

	/**
	 * the global random number generator
	 */
	private static final Random random = new Random(DEFAULT_RANDOM_SEED);

	/**
	 * Resets the random number generator with a default random seed.
	 */
	public static void reset() {
		reset(DEFAULT_RANDOM_SEED);
	}

	/**
	 * Resets the global random number generator with the given seed. Also resets the internal counter used to for random seeds of local {@link Random}
	 * instances.
	 *
	 * @param seed The seed used to draw random numbers.
	 */
	public static void reset(final long seed) {
		globalSeed = seed;
		internalCounter = new AtomicInteger(0);
		getRandom().setSeed(seed);
	}

	public static Random getRandom() {
		return random;
	}

	/**
	 * Returns an instance of an RNG, which can be used
	 * locally, e.g. in threads. Each instance of {@link Random} receives a distinct random seed. Based on the global seed and a local counter.
	 * Additionally, each instance of @{link Random} is warmed up by invoking {@link Random#nextDouble()} 100 times.
	 *
	 * @return pseudo random number generator
	 */
	public static Random getLocalInstance() {
		var localSeed = internalCounter.getAndIncrement();
		return getLocalInstance(globalSeed + localSeed * 23L);
	}

	/**
	 * Returns an instance of an RNG, which can be used in concurrent setups. In contrast to {@link #getLocalInstance()}, the caller is responsible
	 * to supply distinct random seeds. The supplied seed is combined with the global seed. Since the global seed is changed each iteration, the
	 * random number sequence of the returned RNG varies between MATSim iterations.
	 */
	public static Random getLocalInstance(final long seed) {
		var random = new Random(globalSeed + seed);
		prepareRNG(random);
		return random;
	}

	/**
	 * Draw some random numbers to better initialize the pseudo-random number generator.
	 *
	 * @param rng the random number generator to initialize.
	 */
	private static void prepareRNG(final Random rng) {
		for (int i = 0; i < 100; i++) {
			rng.nextDouble();
		}
	}
}
