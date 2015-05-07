/* *********************************************************************** *
 * project: org.matsim.*
 * PlansScoring.java
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

import org.matsim.analysis.TravelDistanceStats;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import com.google.inject.Inject;

/**
 * A {@link org.matsim.core.controler.listener.ControlerListener} that manages the
 * scoring of plans in every iteration. Basically it integrates the
 * {@link org.matsim.core.scoring.EventsToScore} with the
 * {@link org.matsim.core.controler.Controler}.
 *
 * @author mrieser, michaz
 */
public final class PlansScoringImpl implements PlansScoring, ScoringListener, IterationStartsListener, IterationEndsListener, ShutdownListener {

	private EventsToScore eventsToScore;

	private Scenario sc;

	private EventsManager events;

	private ScoringFunctionFactory scoringFunctionFactory;

	private OutputDirectoryHierarchy controlerIO;

	private TravelDistanceStats travelDistanceStats; 

	@Inject
	public PlansScoringImpl( Scenario sc, EventsManager events, OutputDirectoryHierarchy controlerIO, ScoringFunctionFactory scoringFunctionFactory ) {
		this.sc = sc ;
		this.events = events ;
		this.scoringFunctionFactory = scoringFunctionFactory ;
		this.controlerIO = controlerIO;
		this.travelDistanceStats = new TravelDistanceStats(sc.getConfig(), sc.getNetwork(), sc.getTransitSchedule(), controlerIO.getOutputFilename(Controler.FILENAME_TRAVELDISTANCESTATS), sc.getConfig().controler().isCreateGraphs());
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		this.eventsToScore = new EventsToScore( this.sc, this.scoringFunctionFactory, this.sc.getConfig().planCalcScore().getLearningRate() );
		this.events.addHandler(this.eventsToScore);
	}

	@Override
	public void notifyScoring(final ScoringEvent event) {
		this.eventsToScore.finish();
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		this.events.removeHandler(this.eventsToScore);
		if(sc.getConfig().planCalcScore().isWriteExperiencedPlans()) {
			final int writePlansInterval = sc.getConfig().controler().getWritePlansInterval();
			if (writePlansInterval > 0 && event.getIteration() % writePlansInterval == 0) {
				this.eventsToScore.writeExperiencedPlans(controlerIO.getIterationFilename(event.getIteration(), "experienced_plans"));
			}
		}
		this.travelDistanceStats.addIteration(event.getIteration(), eventsToScore.getAgentRecords());
		if ( sc.getConfig().planCalcScore().isMemorizingExperiencedPlans() ) {
			for ( Person person : this.sc.getPopulation().getPersons().values() ) {
				Plan experiencedPlan = eventsToScore.getAgentRecords().get( person.getId() ) ;
				if ( experiencedPlan==null ) {
					throw new RuntimeException("experienced plan is null; I don't think this should happen") ;
				}
				Plan selectedPlan = person.getSelectedPlan() ;
				selectedPlan.getCustomAttributes().put(PlanCalcScoreConfigGroup.EXPERIENCED_PLAN_KEY, experiencedPlan ) ;
			}
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent controlerShudownEvent) {
		travelDistanceStats.close();
	}

	/** 
	 * 
	 * @deprecated It is not a good idea to allow ScoringFunctions to be plucked out of this module in the middle of the scoring process.
	 * Let's try and get rid of it. michaz '2012
	 */
	@Deprecated
	public ScoringFunction getScoringFunctionForAgent(Id<Person> agentId) {
		return this.eventsToScore.getScoringFunctionForAgent(agentId);
	}

}
