/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlansScoring.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.scoring;

import org.apache.log4j.Logger;

import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;

import playground.thibautd.jointtripsoptimizer.population.PopulationWithCliques;
import playground.thibautd.jointtripsoptimizer.run.JointControler;

/**
 * Same as {@link org.matsim.core.controler.corelisteners.PlansScoring},
 * but using a JointEventsToScore instance instead of a {@link org.matsim.core.scoring.EventsToScore}
 * one.
 *
 * @author thibautd
 */
public class JointPlansScoring implements StartupListener, ScoringListener, IterationStartsListener {

	private final static Logger log = Logger.getLogger(PlansScoring.class);
	private JointEventsToScore planScorer;

	@Override
	public void notifyStartup(final StartupEvent event) {
		JointControler controler = null;

		try {
			controler = (JointControler) event.getControler();
		} catch (ClassCastException e) {
			throw new RuntimeException("JointPlanScoring requires a JointControler");
		}

		this.planScorer = new JointEventsToScore(
				(PopulationWithCliques) controler.getPopulation(),
				controler.getScoringFunctionFactory(),
				controler.getConfig().planCalcScore().getLearningRate());
		log.debug("PlanScoring loaded ScoringFunctionFactory");
		event.getControler().getEvents().addHandler(this.planScorer);
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		this.planScorer.reset(event.getIteration());
	}

	@Override
	public void notifyScoring(final ScoringEvent event) {
		this.planScorer.finish();
	}

	public JointEventsToScore getPlanScorer() {
		return planScorer;
	}
}

