/* *********************************************************************** *
 * project: org.matsim.*
 * PlansScoring.java.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.controler.corelisteners;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scoring.EventsToScore;

/**
 * A {@link org.matsim.core.controler.listener.ControlerListener} that manages the
 * scoring of plans in every iteration. Basically it integrates the
 * {@link org.matsim.core.scoring.EventsToScore} with the
 * {@link org.matsim.core.controler.Controler}.
 *
 * @author mrieser
 */
public class PlansScoring implements StartupListener, ScoringListener, IterationStartsListener {

	private EventsToScore planScorer;

	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler();
		this.planScorer = new EventsToScore(controler.getPopulation(), controler.getScoringFunctionFactory());
		Logger.getLogger(PlansScoring.class).debug("PlanScoring loaded ScoringFunctionFactory");
		event.getControler().getEvents().addHandler(this.planScorer);
	}

	public void notifyIterationStarts(final IterationStartsEvent event) {
		this.planScorer.reset(event.getIteration());
	}

	public void notifyScoring(final ScoringEvent event) {
		this.planScorer.finish();
	}

}
