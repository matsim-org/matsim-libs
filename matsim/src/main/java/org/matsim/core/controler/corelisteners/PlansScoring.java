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
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.PlanElementsToScore;
import org.matsim.core.scoring.ScoringFunction;

/**
 * A {@link org.matsim.core.controler.listener.ControlerListener} that manages the
 * scoring of plans in every iteration. Basically it integrates the
 * {@link org.matsim.core.scoring.EventsToScore} with the
 * {@link org.matsim.core.controler.Controler}.
 *
 * @author mrieser, michaz
 */
public class PlansScoring implements StartupListener, ScoringListener, IterationStartsListener {

	private final static Logger log = Logger.getLogger(PlansScoring.class);
	private EventsToActivities eventsToActivities;
	private EventsToLegs eventsToLegs;
	private PlanElementsToScore planElementsToScore;

	@Override
	public void notifyStartup(final StartupEvent event) {
		eventsToActivities = new EventsToActivities();
		eventsToLegs = new EventsToLegs();
		event.getControler().getEvents().addHandler(eventsToActivities);
		event.getControler().getEvents().addHandler(eventsToLegs);
		event.getControler().getEvents().addHandler(new AgentStuckEventHandler() {

			@Override
			public void reset(int iteration) {
				
			}

			@Override
			public void handleEvent(AgentStuckEvent event) {
				ScoringFunction sf = planElementsToScore.getScoringFunctionForAgent(event.getPersonId());
				if (sf != null) {
					sf.agentStuck(event.getTime());
				}
			}
			
		});
		event.getControler().getEvents().addHandler(new AgentMoneyEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(AgentMoneyEvent event) {
				ScoringFunction sf = planElementsToScore.getScoringFunctionForAgent(event.getPersonId());
				if (sf != null) {
					sf.addMoney(event.getAmount());
				}
			}
			
		});
		log.debug("PlanScoring startup.");
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		planElementsToScore = new PlanElementsToScore(event.getControler().getScenario(), event.getControler().getScoringFunctionFactory(), event.getControler().getConfig().planCalcScore().getLearningRate());
		eventsToActivities.setActivityHandler(planElementsToScore);
		eventsToLegs.setLegHandler(planElementsToScore);
	}

	@Override
	public void notifyScoring(final ScoringEvent event) {
		this.eventsToActivities.finish();
		this.planElementsToScore.finish();
	}

	/** 
	 * 
	 * @deprecated It is not a good idea to allow ScoringFunctions to be plucked out of this module in the middle of the scoring process.
	 * Let's try and get rid of it. michaz '2012
	 */
	@Deprecated
	public ScoringFunction getScoringFunctionForAgent(Id agentId) {
		return planElementsToScore.getScoringFunctionForAgent(agentId);
	}

}
