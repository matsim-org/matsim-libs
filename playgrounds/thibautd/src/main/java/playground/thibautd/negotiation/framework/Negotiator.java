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

import org.matsim.core.utils.misc.Counter;

import java.util.function.Consumer;

/**
 * @author thibautd
 */
public class Negotiator<P extends Proposition> {

	private final NegotiatorConfigGroup configGroup;

	public Negotiator(
			final NegotiatorConfigGroup configGroup ) {
		this.configGroup = configGroup;
	}

	public void negotiate(
			final NegotiatingAgents<P> agents,
			final Consumer<P> acceptedPropositionCallback ) {
		double currentSuccessFraction = 1;

		final Counter counter = new Counter( "negotiation round # " );
		while ( currentSuccessFraction > configGroup.getImprovingFractionThreshold() ) {
			counter.incCounter();
			final NegotiationAgent<P> agent = agents.getRandomAgent();
			final boolean success = agent.planActivity();

			currentSuccessFraction = updateSuccess( currentSuccessFraction , success );
		}
		counter.printCounter();

		for ( NegotiationAgent<P> agent : agents ) {
			acceptedPropositionCallback.accept( agent.getBestProposition() );
		}
	}

	private double updateSuccess( final double currentSuccessFraction, final boolean success ) {
		final double old = (configGroup.getRollingAverageWindow() - 1) * currentSuccessFraction;
		final double curr = success ? 1 : 0;
		return (old + curr) / configGroup.getRollingAverageWindow();
	}
}
