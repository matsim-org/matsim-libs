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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author thibautd
 */
@Singleton
public class NestedLogitAccessibilityCalculator<N extends Enum<N>> {
	private static final Logger log = Logger.getLogger( NestedLogitAccessibilityCalculator.class );
	private final int nThreads;
	// act as "measuring points", located at the coordinate of the first activity
	private final Population population;
	// "universal choice set"
	private final ActivityFacilities facilities;

	private final Provider<NestedLogitModel<N>> model;

	@Inject
	NestedLogitAccessibilityCalculator(
			final Scenario scenario,
			final Provider<NestedLogitModel<N>> model ) {
		this.population = scenario.getPopulation();
		this.facilities = scenario.getActivityFacilities();
		this.nThreads = scenario.getConfig().global().getNumberOfThreads();
		this.model = model;
	}

	public TObjectDoubleMap<Id<Person>> computeAccessibilities() {
		log.info( "Compute accessibility for "+population.getPersons().size()+" persons using "+nThreads+" threads" );
		final ComputationThreadHandler<N> runner =
				new ComputationThreadHandler<>(
						model,
						nThreads );
		for ( Person p : population.getPersons().values() ) {
			runner.addPerson( p );
		}

		return runner.run();
	}

	private static class ComputationThreadHandler<N extends Enum<N>> {
		private List<ComputationRunnable<N>> runnables;
		private int n = 0;

		final Counter counter = new Counter( "Compute accessibility for person # " );
		final Counter emptyCounter = new Counter( "Ignore empty plan # " );
		final ComputationStopWatch stopWatch = new ComputationStopWatch();

		public ComputationThreadHandler(
				final Provider<NestedLogitModel<N>> models,
				final int nThreads ) {
			this.runnables = new ArrayList<>( nThreads );

			for ( int i = 0; i < nThreads; i++ ) {
				runnables.add(
						new ComputationRunnable<>(
								models.get(),
								stopWatch,
								counter,
								emptyCounter ) );
			}
		}

		public void addPerson( final Person p ) {
			runnables.get( n ).persons.add( p );
			n++;
			n %= runnables.size();
		}

		public TObjectDoubleMap<Id<Person>> run() {
			final List<Thread>  threads = new ArrayList<>( runnables.size() );
			for ( ComputationRunnable<N> r : runnables ) threads.add( new Thread( r ) );

			for ( Thread t : threads ) t.start();
			try {
				for ( Thread t : threads ) t.join();
			}
			catch ( InterruptedException e ) {
				throw new RuntimeException( e );
			}
			counter.printCounter();
			emptyCounter.printCounter();
			stopWatch.printStats();

			final TObjectDoubleMap<Id<Person>> results = new TObjectDoubleHashMap<>(  );
			for ( ComputationRunnable<N> r : runnables ) {
				results.putAll( r.result );
			}
			return results;
		}
	}

	private static class ComputationRunnable<N extends Enum<N>> implements Runnable {
		private final NestedLogitModel<N> model;
		private final List<Person> persons = new ArrayList<>(  );
		private final TObjectDoubleMap<Id<Person>> result = new TObjectDoubleHashMap<>( );
		private final ComputationStopWatch stopWatch;

		final Counter personCounter;
		final Counter emptyCounter;

		private ComputationRunnable(
				final NestedLogitModel<N> model,
				ComputationStopWatch stopWatch, final Counter personCounter,
				final Counter emptyCounter ) {
			this.model = model;
			this.stopWatch = stopWatch;
			this.personCounter = personCounter;
			this.emptyCounter = emptyCounter;
		}

		@Override
		public void run() {
			for ( Person p : persons ) {
				personCounter.incCounter();
				if ( p.getPlans().isEmpty() || p.getSelectedPlan().getPlanElements().isEmpty() ) {
					emptyCounter.incCounter();
					continue;
				}
				result.put(
						p.getId(),
						computeAccessibility( p ) );
			}
		}

		private double computeAccessibility( Person p ) {
			stopWatch.startChoice();
			final NestedChoiceSet<N> choiceSet = model.getChoiceSetIdentifier().identifyChoiceSet( p );
			stopWatch.endChoice();

			stopWatch.startLogsum();
			final LogSumExpCalculator calculator = new LogSumExpCalculator( choiceSet.getNests().size() );
			for ( Nest<N> nest : choiceSet.getNests() ) {
				calculator.addTerm( logSumNestUtilities( p , nest ) );
			}

			final double r = calculator.computeLogsumExp() / model.getMu();
			stopWatch.endLogsum();
			return r;
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

	}

	private static class ComputationStopWatch {
		private final AtomicLong choiceTime = new AtomicLong( 0 );
		private final AtomicLong logsumTime = new AtomicLong( 0 );

		public void startChoice() {
			// on the choice between currentTimeMillis and nanoTime, see
			// http://stackoverflow.com/a/1776053
			// currentTimeMillis is choosen because it does not depend on CPU
			// (basically, both might be wrong, but should give a reasonnable idea)
			choiceTime.addAndGet( -System.currentTimeMillis() );
		}

		public void endChoice() {
			choiceTime.addAndGet( System.currentTimeMillis() );
		}

		public void startLogsum() {
			logsumTime.addAndGet( -System.currentTimeMillis() );
		}

		public void endLogsum() {
			logsumTime.addAndGet( System.currentTimeMillis() );
		}

		public void printStats() {
			log.info( "Time elapsed in sampling choice sets (in seconds): "+
							TimeUnit.MILLISECONDS.toSeconds( choiceTime.get() ) );
			log.info( "Time elapsed in computing logsums (in seconds): "+
							TimeUnit.MILLISECONDS.toSeconds( logsumTime.get() ) );
		}
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
				sum += Math.exp( d - c );
				// TODO check if underflow (how? compare with 0?)
				if ( Double.isInfinite( sum ) ) {
					throw new RuntimeException( "got an overflow for exp "+d+" with correction "+c+"! (resulting in exp("+(d - c)+"))" );
				}
			}

			return Math.log( sum ) + c;
		}
	}

}
