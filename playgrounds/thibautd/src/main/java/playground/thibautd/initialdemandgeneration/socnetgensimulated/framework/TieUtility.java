/* *********************************************************************** *
 * project: org.matsim.*
 * TieUtility.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.framework;

import java.util.Random;

import playground.thibautd.utils.SoftCache;

/**
 * @author thibautd
 */
public class TieUtility {
	private final DeterministicPart deterministicPart;
	private final ErrorTerm errorTerm;

	/**
	 * store combined seed and error term.
	 * Hopefully more lightweight than associating to ties,
	 * because (a) several ties might have the same seed,
	 * and thus the same error term, and (b) it avoids having
	 * to create "tie" objects
	 */
	private final SoftCache< Integer , Double >  cache;

	public TieUtility(
			final DeterministicPart deterministicPart,
			final ErrorTerm errorTerm,
			final boolean doCache) {
		this.deterministicPart = deterministicPart;
		this.errorTerm = errorTerm;
		this.cache = doCache ? new SoftCache< Integer , Double >() : null;
	}

	public double getTieUtility(
			final int ego,
			final int alter ) {
		return deterministicPart.calcDeterministicPart( ego , alter ) +
			error( ego , alter );
	}

	private double error( final int ego , final int alter ) {
		final int seed = ego + alter;
		if ( cache != null ) {
			final Double cached = cache.get( seed );
			if ( cached != null ) return cached.doubleValue();
		}

		final double sampledError = errorTerm.calcError( seed );
		if ( cache != null ) cache.put( seed , sampledError );
		return sampledError;
	}

	public static interface DeterministicPart {
		public double calcDeterministicPart( int ego , int alter );
	}

	public static interface ErrorTerm {
		public double calcError( final int seed );
	}

	public static class GumbelErrorTerm implements ErrorTerm {
		private static final int N_THROWN_DRAWS = 2;

		// avoid re-instanciating random over and over,
		// while remaining thread-safe
		private final ThreadLocal<Random> random =
			new ThreadLocal<Random>() {
				@Override
				protected Random initialValue() {
					return new Random();
				}
			};
		private final double scale;

		public GumbelErrorTerm( ) {
			this( 1d );
		}

		public GumbelErrorTerm( final double scale ) {
			this.scale = scale;
		}

		@Override
		public double calcError( int seed ) {
			final Random rnd = random.get();
			rnd.setSeed( seed );

			// take a few draws to come to the "chaotic region"
			for (int i = 0; i < N_THROWN_DRAWS; i++) {
				rnd.nextInt();
			}

			double uniform = rnd.nextDouble();
			// interval MUST be ]0,1[
			while (uniform == 0.0 || uniform == 1.0) {
				uniform = rnd.nextDouble();
			}

			return - scale * Math.log( -Math.log( uniform ) );
		}
	}
}

