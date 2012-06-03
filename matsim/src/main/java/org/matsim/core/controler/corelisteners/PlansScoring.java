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
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.SimplifiedIterationEndsEvent;
import org.matsim.core.controler.events.SimplifiedIterationStartsEvent;
import org.matsim.core.controler.events.SimplifiedScoringEvent;
import org.matsim.core.controler.listener.SimplifiedIterationEndsListener;
import org.matsim.core.controler.listener.SimplifiedIterationStartsListener;
import org.matsim.core.controler.listener.SimplifiedScoringListener;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

/**
 * A {@link org.matsim.core.controler.listener.ControlerListener} that manages the
 * scoring of plans in every iteration. Basically it integrates the
 * {@link org.matsim.core.scoring.EventsToScore} with the
 * {@link org.matsim.core.controler.Controler}.
 *
 * @author mrieser, michaz
 */
public class PlansScoring implements SimplifiedScoringListener, SimplifiedIterationStartsListener, SimplifiedIterationEndsListener {

	private final static Logger log = Logger.getLogger(PlansScoring.class);

	private EventsToScore eventsToScore;

	private Scenario sc;

	private EventsManager events;

	private ScoringFunctionFactory scoringFunctionFactory;
	
	public PlansScoring( Scenario sc, EventsManager events, ScoringFunctionFactory scoringFunctionFactory ) {
		this.sc = sc ;
		this.events = events ;
		this.scoringFunctionFactory = scoringFunctionFactory ;
	}

	@Override
	public void notifyIterationStarts(final SimplifiedIterationStartsEvent event) {
//		this.eventsToScore = new EventsToScore(event.getControler().getScenario(), event.getControler().getScoringFunctionFactory(), 
//				event.getControler().getConfig().planCalcScore().getLearningRate());
//		event.getControler().getEvents().addHandler(this.eventsToScore);
		this.eventsToScore = new EventsToScore( this.sc, this.scoringFunctionFactory, 
				this.sc.getConfig().planCalcScore().getLearningRate() );
		this.events.addHandler(this.eventsToScore);
	}

	@Override
	public void notifyIterationEnds(final SimplifiedIterationEndsEvent event) {
		this.events.removeHandler(this.eventsToScore);
	}

	@Override
	public void notifyScoring(final SimplifiedScoringEvent event) {
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
