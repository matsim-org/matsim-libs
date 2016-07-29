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
package playground.ivt.maxess.nestedlogitaccessibility.framework;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import playground.ivt.utils.ConcurrentStopWatch;

import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * @author thibautd
 */
@Singleton
public class NestedLogitAccessibilityCalculator<N extends Enum<N>> {
	private static final Logger log = Logger.getLogger( NestedLogitAccessibilityCalculator.class );
	private final int nThreads;
	// population acts as "measuring points", located at the coordinate of the first activity
	private final Scenario scenario;
	// "universal choice set"
	private final ActivityFacilities facilities;

	private final Provider<NestedLogitModel<N>> model;

	private enum Measurement { choiceSampling, logsumComputation;}

	@Inject
	NestedLogitAccessibilityCalculator(
			final Scenario scenario,
			final Provider<NestedLogitModel<N>> model ) {
		this.scenario = scenario;
		this.facilities = scenario.getActivityFacilities();
		this.nThreads = scenario.getConfig().global().getNumberOfThreads();
		this.model = model;
	}

	public AccessibilityComputationResult computeAccessibilities() {
		log.info( "Compute accessibility for "+scenario.getPopulation().getPersons().size()+" persons using "+nThreads+" threads" );

		final Counter counter = new Counter( "Compute accessibility for person # " , " / "+scenario.getPopulation().getPersons().size() );
		final Counter emptyCounter = new Counter( "Ignore empty plan # " );
		final AccessibilityComputationResult result = new AccessibilityComputationResult();

		final ConcurrentStopWatch<Measurement> stopWatch =
				new ConcurrentStopWatch<>(
						Measurement.class );

		final ThreadLocal<NestedLogitModel<N>> localModel =
				new ThreadLocal<NestedLogitModel<N>>() {
					@Override
					protected NestedLogitModel<N> initialValue() {
						return NestedLogitAccessibilityCalculator.this.model.get();
					}
				};

		// Trick to be able to set the number of desired threads. see http://stackoverflow.com/q/21163108
		final ForkJoinPool fjp = new ForkJoinPool( scenario.getConfig().global().getNumberOfThreads() );

		fjp.submit( () -> {
			scenario.getPopulation().getPersons().values().parallelStream().forEach(
					p -> {
						counter.incCounter();
						if ( p.getPlans().isEmpty() || p.getSelectedPlan().getPlanElements().isEmpty() ) {
							emptyCounter.incCounter();
							return;
						}
						result.addResults(
								p.getId(),
								computeAccessibility(
										localModel.get(),
										stopWatch,
										p ) );
					}
			);
		}).join();
		fjp.shutdown();

		counter.printCounter();
		emptyCounter.printCounter();
		stopWatch.printStats( TimeUnit.SECONDS );

		return result;
	}

	private static <N extends Enum<N>> AccessibilityComputationResult.PersonAccessibilityComputationResult computeAccessibility(
			final NestedLogitModel<N> model,
			final ConcurrentStopWatch<Measurement> stopWatch,
			final Person p ) {
		stopWatch.startMeasurement( Measurement.choiceSampling );
		final Map<String, NestedChoiceSet<N>> choiceSets =
				model.getChoiceSetIdentifier().identifyChoiceSet(
						p );
		stopWatch.endMeasurement( Measurement.choiceSampling );

		final AccessibilityComputationResult.PersonAccessibilityComputationResult accessibilities =
				new AccessibilityComputationResult.PersonAccessibilityComputationResult();

		for ( Map.Entry<String, NestedChoiceSet<N>> choiceSet : choiceSets.entrySet() ){
			accessibilities.addAccessibility(
					choiceSet.getKey(),
					computeExpectedMaximumUtility(
							stopWatch, p, model,
							choiceSet.getValue() ) );
		}

		return accessibilities;
	}

	private static <N extends Enum<N>> double computeExpectedMaximumUtility(
			final ConcurrentStopWatch<Measurement> stopWatch,
			final Person p,
			final NestedLogitModel<N> model,
			final NestedChoiceSet<N> choiceSet ) {
		stopWatch.startMeasurement( Measurement.logsumComputation );
		final LogSumExpCalculator calculator = new LogSumExpCalculator( choiceSet.getNests().size() );
		for ( Nest<N> nest : choiceSet.getNests() ) {
			if ( nest.getAlternatives().isEmpty() ) continue;
			calculator.addTerm( logSumNestUtilities( p , model, nest ) );}

		final double r = calculator.computeLogsumExp() / model.getMu();
		stopWatch.endMeasurement( Measurement.logsumComputation );
		return r;
	}

	private static <N extends Enum<N>> double logSumNestUtilities(
			final Person p,
			final NestedLogitModel<N> model,
			final Nest<N> nest ) {
		final LogSumExpCalculator calculator = new LogSumExpCalculator( nest.getAlternatives().size() );
		assert !nest.getAlternatives().isEmpty();
		for ( Alternative<N> alternative : nest.getAlternatives() ) {
			try {
				calculator.addTerm( nest.getMu_n() *
						model.getUtility().calcUtility(
								p,
								alternative ) );
			}
			catch (Exception e){
				throw new RuntimeException( "Problem with utility of alternative "+alternative , e );
			}
		}

		return ( model.getMu() / nest.getMu_n() ) * calculator.computeLogsumExp();
	}
}
