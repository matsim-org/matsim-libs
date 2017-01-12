/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.negotiation.locationnegotiation;

import com.google.inject.Singleton;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.utils.objectattributes.attributable.Attributable;

import java.util.Random;

/**
 * Designed to abstract way to retrieve random seeds.
 *
 * @author thibautd
 */
@Singleton
public class RandomSeedHelper {
	private final Random random = MatsimRandom.getLocalInstance();

	private final ThreadLocal<Random> resetableRandom =
			new ThreadLocal<Random>() {
				protected Random initialValue() { return new Random(); }
			};

	public long getSeed( final Attributable person ) {
		Long seed = (Long) person.getAttributes().getAttribute( "seed" );
		if ( seed != null ) return seed;

		seed = random.nextLong();
		person.getAttributes().putAttribute( "seed" , seed );
		return seed;
	}

	public long getSeed( final Attributable o1 ,final Attributable o2 ) {
		final long seed1 = getSeed( o1 );
		final long seed2 = getSeed( o2 );

		// symmetric
		return seed1 ^ seed2;
	}

	private Random getRandom( final Attributable o1 ,final Attributable o2 ) {
		final Random r = resetableRandom.get();
		r.setSeed( getSeed( o1 , o2 ) );
		return r;
	}

	public double getUniformErrorTerm( final Attributable o1 ,final Attributable o2 ) {
		return getRandom( o1 , o2 ).nextDouble();
	}

	// profiling shows that this is the expensive bit.
	private static final double MULT = StrictMath.sqrt( 2 * StrictMath.exp( -1 ) );
	public double getGaussianErrorTerm( final Attributable o1 ,final Attributable o2 ) {
		// ratio of uniform method. see http://www2.econ.osaka-u.ac.jp/~tanizaki/class/2013/econome3/13.pdf
		// slightly faster than default Random.nextRandomGaussian version, and should be better and generating the tails
		// of the normal distribution (polar method tends to cap at -6 and 6, if I trust what I read)
		final Random r = getRandom( o1 , o2 );

		while ( true ) {
			final double u1 = r.nextDouble();
			final double v = r.nextDouble();

			final double u2 = (2 * v - 1 ) * MULT;

			if ( -4 * u1 * u1 * Math.log( u1 ) >= u2 * u2 ) return u2 / u1;
		}
	}
}
