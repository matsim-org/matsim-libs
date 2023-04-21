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


/**
 * An abstract class, providing random numbers for MATSim. Also provides
 * Random Number Generators (RNG) for use in threads, which should all
 * use their own RNGs for deterministic behavior.
 *
 * @author mrieser
 */
public abstract class MatsimRandom {
	private static final long DEFAULT_RANDOM_SEED = 4711;

	private static long lastUsedSeed = DEFAULT_RANDOM_SEED;
	private static int internalCounter = 0;

	/** the global random number generator */
	private static final Random random = new Random(DEFAULT_RANDOM_SEED);

	/** Resets the random number generator with a default random seed. */
	public static void reset() {
		reset(DEFAULT_RANDOM_SEED);
	}

	/** Resets the random number generator with the given seed.
	 *
	 * @param seed The seed used to draw random numbers.
	 */
	public static void reset(final long seed) {
		lastUsedSeed = seed;
		internalCounter = 0;
		getRandom().setSeed(seed);
//		prepareRNG(random);
	}
	public static Random getRandom() {
		return random;
	}

	/** Returns an instance of a random number generator, which can be used
	 * locally, e.g. in threads.
	 *
	 * @return pseudo random number generator
	 */
	public static Random getLocalInstance() {
		internalCounter++;
		Random r = new Random(lastUsedSeed + internalCounter*23l);
		prepareRNG(r);
		return r;
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
