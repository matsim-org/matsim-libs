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
package playground.thibautd.maxess.nestedlogitaccessibility.framework;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;

/**
 * @author thibautd
 */
public class NestedLogitAccessibilityCalculator<N extends Enum<N>> {
	private static final Logger log = Logger.getLogger( NestedLogitAccessibilityCalculator.class );
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

		log.info( "Compute accessibility for "+population.getPersons().size()+" persons" );
		final Counter counter = new Counter( "Compute accessibility for person # " );
		final Counter emptyCounter = new Counter( "Ignore empty plan # " );
		for ( Person p : population.getPersons().values() ) {
			counter.incCounter();
			if ( p.getPlans().isEmpty() || p.getSelectedPlan().getPlanElements().isEmpty() ) {
				emptyCounter.incCounter();
				continue;
			}
			accessibilities.put( p.getId(), computeAccessibility( p ) );
		}
		emptyCounter.printCounter();
		counter.printCounter();

		return accessibilities;
	}

	private double computeAccessibility( Person p ) {
		final NestedChoiceSet<N> choiceSet = model.getChoiceSetIdentifier().identifyChoiceSet( p );

		final LogSumExpCalculator calculator = new LogSumExpCalculator( choiceSet.getNests().size() );
		for ( Nest<N> nest : choiceSet.getNests() ) {
			calculator.addTerm( logSumNestUtilities( p , nest ) );
		}

		return calculator.computeLogsumExp() / model.getMu();
	}

	private double logSumNestUtilities(
			final Person p,
			final Nest<N> nest ) {
		final LogSumExpCalculator calculator = new LogSumExpCalculator( nest.getAlternatives().size() );
		for ( Alternative<N> alternative : nest.getAlternatives() ) {
			calculator.addTerm( nest.getMu_n() *
							model.getUtility().calcUtility(
								p,
								alternative ) );
		}

		return ( model.getMu() / nest.getMu_n() ) * calculator.computeLogsumExp();
	}

	private static class LogSumExpCalculator {
		private final TDoubleArrayList terms;

		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;

		public LogSumExpCalculator( final int size ) {
			terms = new TDoubleArrayList( size );
		}

		public void addTerm( final double util ) {
			terms.add( util );
			min = Math.min( util , min );
			max = Math.max( util , max );
		}

		public double computeLogsumExp() {
			// under and overflow avoidance
			// see http://jblevins.org/log/log-sum-exp
			// Note that this can only avoid underflow OR overflow,
			// not both at the same time

			// correcting constant: greatest term in absolute value
			final double c = (-min > max) ? -min : max;

			double sum = 0;
			for ( double d : terms.toArray() ) {
				// TODO check if under/overflow anyway
				sum += Math.exp( d - c );
			}

			return Math.log( sum ) + c;
		}
	}

}
