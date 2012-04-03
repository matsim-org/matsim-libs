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
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;

/**
 * A {@link org.matsim.core.controler.listener.ControlerListener} that manages the
 * scoring of plans in every iteration. Basically it integrates the
 * {@link org.matsim.core.scoring.EventsToScore} with the
 * {@link org.matsim.core.controler.Controler}.
 *
 * @author mrieser, michaz
 */
public class PlansScoring implements StartupListener, ScoringListener, IterationStartsListener, IterationEndsListener {

	private final static Logger log = Logger.getLogger(PlansScoring.class);

	private EventsToScore eventsToScore;

	@Override
	public void notifyStartup(final StartupEvent event) {
		log.debug("PlanScoring startup.");
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		this.eventsToScore = new EventsToScore(event.getControler().getScenario(), event.getControler().getScoringFunctionFactory(), event.getControler().getConfig().planCalcScore().getLearningRate());
		event.getControler().getEvents().addHandler(this.eventsToScore);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		event.getControler().getEvents().removeHandler(this.eventsToScore);
	}

	@Override
	public void notifyScoring(final ScoringEvent event) {
		this.eventsToScore.finish();
	}

	/** 
	 * 
	 * @deprecated It is not a good idea to allow ScoringFunctions to be plucked out of this module in the middle of the scoring process.
	 * Let's try and get rid of it. michaz '2012
	 */
	@Deprecated
	public ScoringFunction getScoringFunctionForAgent(Id agentId) {
		return this.eventsToScore.getScoringFunctionForAgent(agentId);
	}

}
