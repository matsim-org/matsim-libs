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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.degreebased;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.Ego;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.EgoCharacteristicsDistribution;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.SnowballCliques;

import java.util.Random;

/**
 * Very simple implementation, that does not care about socio-demographics
 *
 * @author thibautd
 */
@Singleton
public class SimpleEgoDistribution implements EgoCharacteristicsDistribution {
	private final Random random = MatsimRandom.getLocalInstance();
	// could be compressed a lot, by storing (cumulative) counts in another array and searching with binary search on count
	private final int[] degrees;

	@Inject
	public SimpleEgoDistribution( final SnowballCliques cliques ) {
		degrees = new int[ cliques.getEgos().size() ];
		for ( int i = 0; i < degrees.length; i++ ) degrees[ i ] = cliques.getEgos().get( i ).getDegree();
	}

	@Override
	public Ego sampleEgo( final Person person ) {
		return new Ego( person , degrees[ random.nextInt( degrees.length ) ] );
	}
}
