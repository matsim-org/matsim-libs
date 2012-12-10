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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.EventsToScore2;
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
public class PlansScoring implements ScoringListener, IterationStartsListener, IterationEndsListener {

	private EventsToScore eventsToScore = null ;
	private EventsToScore2 eventsToScore2 = null ;

	private Scenario sc;

	private EventsManager events;

	private ScoringFunctionFactory scoringFunctionFactory;

	private OutputDirectoryHierarchy controlerIO;
	
	public PlansScoring( Scenario sc, EventsManager events, OutputDirectoryHierarchy controlerIO, ScoringFunctionFactory scoringFunctionFactory ) {
		this.sc = sc ;
		this.events = events ;
		this.scoringFunctionFactory = scoringFunctionFactory ;
		this.controlerIO = controlerIO;
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		// yyyy the awkward syntax with eventsToScore and eventsToScore2 is to facilitate easy switching between the two
		// (done by modifying ONLY the following line).  If eventsToScore2 is successful, eventsToScore can be removed.   kai, dec'12
		this.eventsToScore2 = new EventsToScore2( this.sc, this.scoringFunctionFactory, this.sc.getConfig().planCalcScore().getLearningRate() );
		if ( eventsToScore != null ) {
			this.events.addHandler(this.eventsToScore);
		} else {
			this.events.addHandler(this.eventsToScore2);
		}
			
	}

	@Override
	public void notifyScoring(final ScoringEvent event) {
		if ( eventsToScore != null ) {
			this.eventsToScore.finish();
		} else {
			this.eventsToScore2.finish() ;
		}
	}
	
	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if ( eventsToScore != null ) { 
			this.events.removeHandler(this.eventsToScore);
		} else {
			this.events.removeHandler(this.eventsToScore2) ;
		}
		if(sc.getConfig().planCalcScore().isWriteExperiencedPlans()) {
			if ( eventsToScore != null ){
				this.eventsToScore.writeExperiencedPlans(controlerIO.getIterationFilename(event.getIteration(), "experienced_plans.xml"));
			} else {
				this.eventsToScore2.writeExperiencedPlans(controlerIO.getIterationFilename(event.getIteration(), "experienced_plans.xml"));
			}
		}
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
