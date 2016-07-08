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
package playground.ivt.maxess.nestedlogitaccessibility.depalmaconstrained;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Alternative;
import playground.ivt.maxess.nestedlogitaccessibility.framework.LogSumExpCalculator;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Nest;
import playground.ivt.maxess.nestedlogitaccessibility.framework.NestedChoiceSet;
import playground.ivt.maxess.nestedlogitaccessibility.framework.NestedLogitModel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

/**
 * @author thibautd
 */
class Demand<N extends Enum<N>> {
	private static final Logger log = Logger.getLogger( Demand.class );
	// store with "situation" strings, but for the moment only provide accessors for "unique" case
	private final Map<Id<Person>, Map<String, TObjectDoubleMap<Id<ActivityFacility>>>> probas = new ConcurrentHashMap<>();

	private final TObjectDoubleMap<Id<ActivityFacility>> demands = new TObjectDoubleHashMap<>();
	private final Map<Id<ActivityFacility>, TObjectDoubleMap<Id<Person>>> demandPerFacility = new HashMap<>();

	public Demand( final NestedLogitModel<N> model, final Scenario scenario ) {
		log.info( "initialize demand: "+scenario.getPopulation().getPersons().size()+" persons" );
		log.info( "compute choice proba for each agent" );
		// Trick to be able to set the number of desired threads. see http://stackoverflow.com/q/21163108
		final ForkJoinPool fjp = new ForkJoinPool( scenario.getConfig().global().getNumberOfThreads() );

		final Counter counter = new Counter( "look at agent # " , " / "+scenario.getPopulation().getPersons().size() );
		fjp.submit( () -> {
			scenario.getPopulation().getPersons().values().parallelStream()
					.forEach( p -> {
						counter.incCounter();
						probas.put(
								p.getId(),
								computeChoiceProbas(
										p,
										model ) );
					} );
		}).join();
		counter.printCounter();

		fjp.shutdown();

		log.info( "compute demand for each facility" );
		counter.reset();
		probas.values().stream()
				.flatMap( m -> m.values().stream() )
				.forEach( indivProbas -> {
					counter.incCounter();
					indivProbas.forEachEntry( (facility, proba) -> {
						demands.adjustOrPutValue( facility , proba , proba );
						return true;
					} );
				} );
		counter.printCounter();

		log.info( "compute disagregated demand for each facility" );
		counter.reset();
		probas.entrySet().stream()
				.forEach( entry -> {
					counter.incCounter();
					if ( entry.getValue().size() != 1 ) throw new UnsupportedOperationException();
					final TObjectDoubleMap<Id<ActivityFacility>> facilities = entry.getValue().values().stream().findAny().get();
					facilities.forEachEntry( (facility , proba) -> {
						TObjectDoubleMap<Id<Person>> m = demandPerFacility.get( facility );

						if ( m == null ) {
							m = new TObjectDoubleHashMap<>();
							demandPerFacility.put( facility , m );
						}
						m.put( entry.getKey() , proba );
						return true;
					} );
				} );
		counter.printCounter();
	}

	public TObjectDoubleMap<Id<ActivityFacility>> getSummedDemandPerFacility() {
		return demands;
	}

	public double getDemand( final Id<ActivityFacility> facilityId ) {
		return demands.containsKey( facilityId ) ? demands.get( facilityId ) : 0;
	}

	public TObjectDoubleMap<Id<ActivityFacility>> getProbabilitiesForIndividual( final Id<Person> person ) {
		final Map<String, TObjectDoubleMap<Id<ActivityFacility>>> situations = probas.get( person );
		if ( situations.size() != 1 ) throw new IllegalStateException( "unhandled size "+situations.size() );

		return situations.values().iterator().next();
	}

	public Map<Id<ActivityFacility>, TObjectDoubleMap<Id<Person>>> getDemandPerFacility() {
		return demandPerFacility;
	}

	private Map<String, TObjectDoubleMap<Id<ActivityFacility>>> computeChoiceProbas(
			final Person p,
			final NestedLogitModel<N> model ) {
		// different choice sets, one per choice situation (availability constrained etc.)
		final Map<String, NestedChoiceSet<N>> choiceSets =
				model.getChoiceSetIdentifier().identifyChoiceSet( p );

		final Map<String, TObjectDoubleMap<Id<ActivityFacility>>> map = new HashMap<>();

		if ( choiceSets.size() != 1 ) {
			throw new UnsupportedOperationException( "constrained accessibility with more than one situation is not yet implemented" );
		}

		for ( Map.Entry<String, NestedChoiceSet<N>> choiceSet : choiceSets.entrySet() ) {
			map.put(
					choiceSet.getKey(),
					computeChoiceProbas(
							model,
							p,
							choiceSet.getValue() ) );
		}

		return map;
	}

	private TObjectDoubleMap<Id<ActivityFacility>> computeChoiceProbas(
			final NestedLogitModel<N> model,
			final Person p,
			final NestedChoiceSet<N> choiceSet ) {
		final TObjectDoubleMap<Id<Alternative>> utilities = new TObjectDoubleHashMap<>();
		final TObjectDoubleMap<N> nestLogsums = new TObjectDoubleHashMap<>();

		for ( Nest<N> nest : choiceSet.getNests() ) {
			final LogSumExpCalculator logsumNest = new LogSumExpCalculator( nest.getAlternatives().size() );

			for ( Alternative<N> alternative : nest.getAlternatives() ) {
				final double utility = model.getUtility().calcUtility( p, alternative );
				utilities.put( alternative.getAlternativeId(), utility );
				logsumNest.addTerm( nest.getMu_n() * utility );
			}

			nestLogsums.put(
					nest.getNestId(),
					( model.getMu() / nest.getMu_n() ) * logsumNest.computeLogsumExp() );
		}

		final TObjectDoubleMap<Id<ActivityFacility>> probabilities = new TObjectDoubleHashMap<>();
		double sum = 0;
		for ( Nest<N> nest : choiceSet.getNests() ) {
			final LogitProbabilityCalculator nestProbabilityCalculator = new LogitProbabilityCalculator();
			nestProbabilityCalculator.setNumeratorUtility( nestLogsums.get( nest.getNestId() ) );
			choiceSet.getNests().stream()
					.mapToDouble( n -> nestLogsums.get( n.getNestId() ) )
					.forEach( nestProbabilityCalculator::addDenominatorUtility );
			final double nestProba = nestProbabilityCalculator.calcProbability();

			for ( Alternative<N> alternative : nest.getAlternatives() ) {
				final LogitProbabilityCalculator inNestProbabilityCalculator = new LogitProbabilityCalculator();
				inNestProbabilityCalculator.setNumeratorUtility( nest.getMu_n() * utilities.get( alternative.getAlternativeId() ) );
				nest.getAlternatives().stream()
						.mapToDouble( a -> nest.getMu_n() * utilities.get( a.getAlternativeId() ) )
						.forEach( inNestProbabilityCalculator::addDenominatorUtility );
				final double prob = nestProba * inNestProbabilityCalculator.calcProbability();
				probabilities.adjustOrPutValue(
						alternative.getAlternative().getDestination().getId(),
						prob , prob );
				sum += prob;
			}
		}

		assert Math.abs( sum - 1 ) < 1E-9 : sum;
		return probabilities;
	}
}
