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
package playground.thibautd.maxess.nestedlogitaccessibility;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacilities;

/**
 * @author thibautd
 */
public class NestedLogitAccessibilityCalculator<N extends Enum<N>> {
	// act as "measuring points", located at the coordinate of the first activity
	private final Population population;
	// "universal choice set"
	private final ActivityFacilities facilities;

	private final NestedLogitModel<N> model;

	public NestedLogitAccessibilityCalculator(
			final Population population,
			final ActivityFacilities facilities,
			final NestedLogitModel<N> model ) {
		this.population = population;
		this.facilities = facilities;
		this.model = model;
	}

	public TObjectDoubleMap<Id<Person>> computeAccessibilities() {
		final TObjectDoubleMap<Id<Person>> accessibilities = new TObjectDoubleHashMap<>( );

		for ( Person p : population.getPersons().values() ) {
			accessibilities.put( p.getId(), computeAccessibility( p ) );
		}

		return accessibilities;
	}

	private double computeAccessibility( Person p ) {
		final NestedChoiceSet<N> choiceSet = model.getChoiceSetIdentifier().identifyChoiceSet( p );

		double sum = 0;
		for ( Nest<N> nest : choiceSet.getNests() ) {
			sum += Math.exp( logSumNestUtilities( p , nest ) );
			// TODO: prevent overflow!!!
			if ( sum == Double.POSITIVE_INFINITY ) {
				throw new RuntimeException( "overflow in exponential" );
			}
		}

		return Math.log( sum ) / model.getMu();
	}

	private double logSumNestUtilities(
			final Person p,
			final Nest<N> nest ) {
		double sum = 0;

		for ( Alternative<N> alternative : nest.getAlternatives() ) {
			sum += Math.exp( nest.getMu_n() *
							model.getUtility().calcUtility(
								p,
								alternative ) );
		}

		return ( model.getMu() / nest.getMu_n() ) * Math.log( sum );
	}

}
