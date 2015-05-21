/* *********************************************************************** *
 * project: org.matsim.*
 * AnnealingCoalitionExpBetaFactory.java
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
package org.matsim.contrib.socnetsim.framework.replanning.selectors;

import java.util.Collection;
import java.util.Collections;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;

import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.gbl.MatsimRandom;

import org.matsim.contrib.socnetsim.framework.replanning.GroupLevelPlanSelectorFactory;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.coalitionselector.CoalitionSelector;

/**
 * To use to select the plan to mutate when using coaliton selection
 * and maximum number of plans per joint plan structure.
 * <br>
 * In this setting, an agent may have a lot of plans, making random plan
 * selection too inneficient. Just using the same selector as the non-innovative
 * strategy to select the plan to mutate seems to result in too few exploration:
 * initial joint plans may be bad but become very good with iterations.
 * <br>
 * The idea here is to start with an almost random selection scheme, and evolve
 * towards the standard factor.
 * @author thibautd
 */
public class AnnealingCoalitionExpBetaFactory implements GroupLevelPlanSelectorFactory, IterationStartsListener {
	private static final Logger log =
		Logger.getLogger(AnnealingCoalitionExpBetaFactory.class);

	private final double initialScale;
	private final double finalScale;

	private final int iterationStartDescent;
	private final int iterationEndDescent;

	// keep track of the weight calculators we spit out.
	// Let them be GC'd if the selector is.
	// We can't just use one weight instance, because it uses a Random,
	// and hence it is not thread safe.
	private final Collection<LogitWeight> weights = Collections.newSetFromMap( new WeakHashMap<LogitWeight, Boolean>() );
	private double currentScale;

	public AnnealingCoalitionExpBetaFactory(
			final double initialScale,
			final double finalScale,
			final int iterationStartDescent,
			final int iterationEndDescent) {
		if ( iterationStartDescent >= iterationEndDescent ) {
			throw new IllegalArgumentException( "descent start iteration "+iterationStartDescent+" must be before end descent iteration "+iterationEndDescent );
		}

		this.initialScale = initialScale;
		this.finalScale = finalScale;
		this.iterationStartDescent = iterationStartDescent;
		this.iterationEndDescent = iterationEndDescent;
		this.currentScale = initialScale;

		log.info( this+" has been initialized successfully" );
	}

	@Override
	public String toString() {
		return "["+getClass().getSimpleName()+": scale=("+initialScale+";"+finalScale+
			"),descent=("+iterationStartDescent+";"+iterationEndDescent+"),"+
			"currentScale="+currentScale+"]";
	}

	@Override
	public GroupLevelPlanSelector createSelector() {
		final LogitWeight weight =
			new LogitWeight(
					MatsimRandom.getLocalInstance(),
					currentScale );
		weights.add( weight );
		return new CoalitionSelector( weight );
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		if ( event.getIteration() <= iterationStartDescent ) {
			this.currentScale = initialScale;
		}
		else if ( event.getIteration() >= iterationEndDescent ) {
			this.currentScale = finalScale;
		}
		else {
			this.currentScale =
				initialScale +
				( event.getIteration() - iterationStartDescent) *
				( (finalScale - initialScale) / (iterationEndDescent - iterationStartDescent));
		}
		log.info( "setting scale to "+currentScale );

		assert currentScale >= Math.min( initialScale , finalScale ) : currentScale+" < "+Math.min( initialScale , finalScale );
		assert currentScale <= Math.max( initialScale , finalScale ) : currentScale+" > "+Math.max( initialScale , finalScale );

		for ( LogitWeight w : weights ) w.setScaleParameter( currentScale );
	}
}

