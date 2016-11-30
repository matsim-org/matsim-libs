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
package playground.thibautd.negotiation.framework;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import playground.ivt.utils.ConcurrentStopWatch;
import playground.thibautd.utils.LambdaCounter;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author thibautd
 */
@Singleton
public class Negotiator<P extends Proposition> {
	private static final Logger log = Logger.getLogger( Negotiator.class );
	private final boolean logStopwatch;

	private final NegotiatorConfigGroup configGroup;
	private final NegotiatingAgents<P> agents;
	private final ConcurrentStopWatch<StopWatchMeasurement> stopwatch;

	@Inject
	public Negotiator(
			final NegotiatorConfigGroup configGroup,
			final NegotiatingAgents<P> agents,
			final ConcurrentStopWatch<StopWatchMeasurement> stopwatch ) {
		this.configGroup = configGroup;
		this.agents = agents;
		this.stopwatch = stopwatch;
		logStopwatch = configGroup.isLogStopwatch();
	}

	public void negotiate(
			final Consumer<NegotiationAgent<P>> acceptedPropositionCallback ) {
		final AtomicDouble currentSuccessFraction = new AtomicDouble( 1 );

		final LambdaCounter counter =
				new LambdaCounter( l -> {
					log.info( "Negotiation round # " + l + ": success fraction " + currentSuccessFraction );
					if ( logStopwatch ) stopwatch.printStats( TimeUnit.MILLISECONDS );
				} );
		while ( currentSuccessFraction.get() > configGroup.getImprovingFractionThreshold() ) {
			counter.incCounter();
			final NegotiationAgent<P> agent = agents.getRandomAgent();
			final boolean success = agent.planActivity();

			currentSuccessFraction.set( updateSuccess( currentSuccessFraction.get() , success ) );
		}
		counter.printCounter();

		for ( NegotiationAgent<P> agent : agents ) {
			acceptedPropositionCallback.accept( agent );
		}
	}

	private double updateSuccess( final double currentSuccessFraction, final boolean success ) {
		final double old = (configGroup.getRollingAverageWindow() - 1) * currentSuccessFraction;
		final double curr = success ? 1 : 0;
		return (old + curr) / configGroup.getRollingAverageWindow();
	}
}
