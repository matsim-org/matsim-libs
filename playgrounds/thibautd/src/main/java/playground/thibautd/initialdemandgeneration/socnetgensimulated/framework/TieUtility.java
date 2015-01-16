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

import playground.thibautd.initialdemandgeneration.socnetgen.framework.Agent;
import playground.thibautd.utils.SoftCache;

/**
 * @author thibautd
 */
public class TieUtility<T extends Agent> {
	private final DeterministicPart<T> deterministicPart;
	private final ErrorTerm errorTerm;

	/**
	 * store combined seed and error term.
	 * Hopefully more lightweight than associating to ties,
	 * because (a) several ties might have the same seed,
	 * and thus the same error term, and (b) it avoids having
	 * to create "tie" objects
	 */
	private final SoftCache< Integer , Double >  cache = new SoftCache< >();

	public TieUtility(
			final DeterministicPart<T> deterministicPart,
			final ErrorTerm errorTerm ) {
		this.deterministicPart = deterministicPart;
		this.errorTerm = errorTerm;
	}

	public double getTieUtility(
			final T ego,
			final T alter ) {
		final int seed = ego.getId().hashCode() + alter.getId().hashCode();

		final Double cached = cache.get( seed );

		if ( cached != null ) return deterministicPart.calcDeterministicPart( ego , alter ) + cached;

		final double sampledError = errorTerm.calcError( seed );
		cache.put( seed , sampledError );

		return deterministicPart.calcDeterministicPart( ego , alter ) + cached;
	}

	public static interface DeterministicPart< T extends Agent> {
		public double calcDeterministicPart( T ego , T alter );
	}

	public static interface ErrorTerm {
		public double calcError( final int seed );
	}

	public static class GumbelErrorTerm implements ErrorTerm {
		private final double scale;

		public GumbelErrorTerm( ) {
			this( 1d );
		}

		public GumbelErrorTerm( final double scale ) {
			this.scale = scale;
		}

		@Override
		public double calcError( int seed ) {
			final Random rnd = new Random( seed );

			// take a few draws to come to the "chaotic region"
			for (int i = 0; i < 5; i++) {
				rnd.nextDouble();
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

