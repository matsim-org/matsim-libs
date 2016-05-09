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

package org.matsim.core.scoring;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ScoringListener;


/**
 * A {@link org.matsim.core.controler.listener.ControlerListener} that manages the
 * scoring of plans in every iteration. Basically it integrates the
 * {@link org.matsim.core.scoring.ScoringFunctionsForPopulation} with the
 * {@link org.matsim.core.controler.Controler}.
 *
 * @author mrieser, michaz
 */
@Singleton
final class PlansScoringImpl implements PlansScoring, ScoringListener, IterationEndsListener {

	@Inject private PlanCalcScoreConfigGroup planCalcScoreConfigGroup;
	@Inject private ControlerConfigGroup controlerConfigGroup;
	@Inject private Population population;
	@Inject private OutputDirectoryHierarchy controlerIO;
	@Inject private ScoringFunctionsForPopulation scoringFunctionsForPopulation;
	@Inject private ExperiencedPlansService experiencedPlansService;

	@Override
	public void notifyScoring(final ScoringEvent event) {
		scoringFunctionsForPopulation.finishScoringFunctions();
		NewScoreAssignerImpl newScoreAssigner = new NewScoreAssignerImpl(this.planCalcScoreConfigGroup, this.controlerConfigGroup);
		newScoreAssigner.assignNewScores(event.getIteration(), this.scoringFunctionsForPopulation, this.population);
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if(planCalcScoreConfigGroup.isWriteExperiencedPlans()) {
			final int writePlansInterval = controlerConfigGroup.getWritePlansInterval();
			if (writePlansInterval > 0 && event.getIteration() % writePlansInterval == 0) {
				this.experiencedPlansService.writeExperiencedPlans(controlerIO.getIterationFilename(event.getIteration(), "experienced_plans.xml.gz"));
				this.scoringFunctionsForPopulation.writePartialScores(controlerIO.getIterationFilename(event.getIteration(), "experienced_plans_scores.xml.gz"));
			}
		}
		if (planCalcScoreConfigGroup.isMemorizingExperiencedPlans() ) {
			for ( Person person : this.population.getPersons().values() ) {
				Plan experiencedPlan = this.experiencedPlansService.getAgentRecords().get( person.getId() ) ;
				if ( experiencedPlan==null ) {
					throw new RuntimeException("experienced plan is null; I don't think this should happen") ;
				}
				Plan selectedPlan = person.getSelectedPlan() ;
				selectedPlan.getCustomAttributes().put(PlanCalcScoreConfigGroup.EXPERIENCED_PLAN_KEY, experiencedPlan ) ;
			}
		}
	}

}
