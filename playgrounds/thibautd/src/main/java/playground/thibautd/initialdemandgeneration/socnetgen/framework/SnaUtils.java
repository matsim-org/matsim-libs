/* *********************************************************************** *
 * project: org.matsim.*
 * SnaUtils.java
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
package playground.thibautd.initialdemandgeneration.socnetgen.framework;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.utils.CollectionUtils;

/**
 * Provides methods to produce standard statistics for social networks
 * @author thibautd
 */
public class SnaUtils {
	private SnaUtils() {}

	public static double estimateClusteringCoefficient(
			final double samplingRate,
			final SocialNetwork socialNetwork) {
		return estimateClusteringCoefficient( new Random( 20150130 ) , samplingRate , socialNetwork );
	}

	public static double estimateClusteringCoefficient(
			final Random random,
			final double samplingRate,
			final SocialNetwork socialNetwork) {
		if ( samplingRate <= 0 ) throw new IllegalArgumentException( "sampling rate must be positive, got "+samplingRate );
		if ( samplingRate > 1 ) throw new IllegalArgumentException( "sampling rate must lower than 1, got "+samplingRate );

		final Counter tripleCounter = new Counter( "clustering calculation: look at triple # " );
		long nTriples = 0;
		long nTriangles = 0;

		for ( Id<Person> ego : socialNetwork.getEgos() ) {
			if ( random != null && random.nextDouble() > samplingRate ) continue;

			final Set<Id<Person>> alterSet = socialNetwork.getAlters( ego );
			final Id<Person>[] alters = alterSet.toArray( new Id[ alterSet.size() ] ); 

			for ( int alter1index = 0; alter1index < alters.length; alter1index++ ) {
				final Set<Id<Person>> altersOfAlter1 = socialNetwork.getAlters( alters[ alter1index ] );
				for ( int alter2index = alter1index + 1; alter2index < alters.length; alter2index++ ) {
					// this is a new triple
					tripleCounter.incCounter();
					nTriples++;

					if ( altersOfAlter1.contains( alters[ alter2index ] ) ) {
						nTriangles++;
					}
				}
			}
		}
		tripleCounter.printCounter();

		// note: in Arentze's paper, it is 3 * triangle / triples.
		// but here, we count every triangle three times.
		assert nTriples >= 0 : nTriples;
		assert nTriangles >= 0 : nTriangles;
		return nTriples > 0 ? (1d * nTriangles) / nTriples : 0;
	}

	public static double calcClusteringCoefficient(
			final SocialNetwork socialNetwork) {
		return estimateClusteringCoefficient( null , 1 , socialNetwork );
	}

	public static double calcAveragePersonalNetworkSize(final SocialNetwork socialNetwork) {
		int count = 0;
		int sum = 0;
		for ( Id ego : socialNetwork.getEgos() ) {
			count++;
			sum += socialNetwork.getAlters( ego ).size();
		}
		return ((double) sum) / count;
	}

	public static Collection<Set<Id>> identifyConnectedComponents(
			final SocialNetwork sn) {
		if ( !sn.isReflective() ) {
			throw new IllegalArgumentException( "the algorithm is valid only with reflective networks" );
		}
		final Map<Id<Person>, Set<Id<Person>>> altersMap = new LinkedHashMap<>( sn.getMapRepresentation() );
		final Collection< Set<Id> > components = new ArrayList< Set<Id> >();
	
		while ( !altersMap.isEmpty() ) {
			// DFS implemented as a loop (recursion results in a stackoverflow on
			// big networks)
			final Id<Person> seed = CollectionUtils.getElement( 0 , altersMap.keySet() );
	
			final Set<Id> component = new HashSet<Id>();
			components.add( component );
			component.add( seed );
	
			final Queue<Id<Person>> stack = Collections.asLifoQueue( new ArrayDeque<Id<Person>>( altersMap.size() ) );
			stack.add( seed );
	
			while ( !stack.isEmpty() ) {
				final Id current = stack.remove();
				final Set<Id<Person>> alters = altersMap.remove( current );
	
				for ( Id<Person> alter : alters ) {
					if ( component.add( alter ) ) {
						stack.add( alter );
					}
				}
			}
	
		}
	
		return components;
	}
}

