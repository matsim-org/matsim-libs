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

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;

/**
 * Provides methods to produce standard statistics for social networks
 * @author thibautd
 */
public class SnaUtils {
	private SnaUtils() {}

	public static double calcClusteringCoefficient(
			final SocialNetwork socialNetwork) {
		int nTriples = 0;
		int nTriangles = 0;
		for ( Id ego : socialNetwork.getEgos() ) {
			final Set<Id> alters = socialNetwork.getAlters( ego );
			final Set<Id> remainingAlters = new HashSet<Id>( alters );

			for ( Id alter1 : alters ) {
				remainingAlters.remove( alter1 );
				final Set<Id> altersOfAlter1 = socialNetwork.getAlters( alter1 );
				for ( Id alter2 : remainingAlters ) {
					// this is a new triple
					nTriples++;

					if ( altersOfAlter1.contains( alter2 ) ) {
						nTriangles++;
					}
				}
			}
		}

		// note: in Arentze's paper, it is 3 * triangle / triples.
		// but here, we count every triangle three times.
		assert nTriples >= 0;
		assert nTriangles >= 0;
		return nTriples > 0 ? (1d * nTriangles) / nTriples : 0;
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
}

