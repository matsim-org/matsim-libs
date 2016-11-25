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

		return seed1 ^ seed2;
	}

	public double getUniformErrorTerm( final Attributable o1 ,final Attributable o2 ) {
		// TODO make sure creating new random each time does not impact perf too much.
		return new Random( getSeed( o1 , o2 ) ).nextDouble();
	}

	public double getGaussianErrorTerm( final Attributable o1 ,final Attributable o2 ) {
		return new Random( getSeed( o1 , o2 ) ).nextGaussian();
	}
}
