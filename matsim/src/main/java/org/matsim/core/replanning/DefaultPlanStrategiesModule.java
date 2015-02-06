/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultPlanStrategiesModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.core.replanning;

import com.google.inject.TypeLiteral;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.replanning.modules.*;
import org.matsim.core.replanning.selectors.*;

import javax.inject.Inject;
import javax.inject.Provider;

public class DefaultPlanStrategiesModule extends AbstractModule {
	
	public static enum DefaultPlansRemovers { WorstPlanSelector, SelectRandom, SelectExpBetaForRemoval, ChangeExpBetaForRemoval, 
		PathSizeLogitSelectorForRemoval } ;

    @Override
    public void install() {
        addPlanSelectorForRemovalBinding(DefaultPlansRemovers.WorstPlanSelector.toString()).to(WorstPlanForRemovalSelector.class);
        addPlanSelectorForRemovalBinding(DefaultPlansRemovers.SelectRandom.toString()).to(new TypeLiteral<RandomPlanSelector<Plan, Person>>(){});
        addPlanSelectorForRemovalBinding(DefaultPlansRemovers.SelectExpBetaForRemoval.toString()).toProvider(ExpBetaPlanSelectorForRemoval.class);
        addPlanSelectorForRemovalBinding(DefaultPlansRemovers.ChangeExpBetaForRemoval.toString()).toProvider(ExpBetaPlanChangerForRemoval.class);
        addPlanSelectorForRemovalBinding(DefaultPlansRemovers.PathSizeLogitSelectorForRemoval.toString()).toProvider(PathSizeLogitSelectorForRemoval.class);

        // strategy packages that only select:
		addPlanStrategyBindingToFactory(DefaultSelectors.KeepLastSelected.toString(), new KeepLastSelectedPlanStrategyFactory());
        addPlanStrategyBindingToFactory(DefaultSelectors.BestScore.toString(), new SelectBestPlanStrategyFactory());
        addPlanStrategyBindingToFactory(DefaultSelectors.SelectExpBeta.toString(), new SelectExpBetaPlanStrategyFactory());
        addPlanStrategyBindingToFactory(DefaultSelectors.ChangeExpBeta.toString(), new ChangeExpBetaPlanStrategyFactory());
        addPlanStrategyBindingToFactory(DefaultSelectors.SelectRandom.toString(), new SelectRandomStrategyFactory());
        addPlanStrategyBindingToFactory(DefaultSelectors.SelectPathSizeLogit.toString(), new SelectPathSizeLogitStrategyFactory());

        // strategy packages that select, copy, and modify.  (The copying is done implicitly as soon as "addStrategyModule" is called
        // at least once).
        addPlanStrategyBindingToFactory(DefaultStrategies.ReRoute.toString(), new ReRoutePlanStrategyFactory());
        addPlanStrategyBindingToFactory(DefaultStrategies.TimeAllocationMutator.toString(), new TimeAllocationMutatorPlanStrategyFactory());
        addPlanStrategyBindingToFactory(DefaultStrategies.TimeAllocationMutator_ReRoute.toString(), new TimeAllocationMutatorReRoutePlanStrategyFactory());
        addPlanStrategyBindingToFactory(DefaultStrategies.ChangeLegMode.toString(), new ChangeLegModeStrategyFactory());
        addPlanStrategyBindingToFactory(DefaultStrategies.ChangeSingleLegMode.toString(), new ChangeSingleLegModeStrategyFactory());
        addPlanStrategyBindingToFactory(DefaultStrategies.ChangeSingleTripMode.toString(), new ChangeSingleTripModeStrategyFactory());
        addPlanStrategyBindingToFactory(DefaultStrategies.SubtourModeChoice.toString(), new SubtourModeChoiceStrategyFactory());
        addPlanStrategyBindingToFactory(DefaultStrategies.ChangeTripMode.toString(), new ChangeTripModeStrategyFactory());
        addPlanStrategyBindingToFactory(DefaultStrategies.TripSubtourModeChoice.toString(), new TripSubtourModeChoiceStrategyFactory());
    }

    public static enum DefaultSelectors { KeepLastSelected, BestScore, ChangeExpBeta, SelectExpBeta, SelectRandom, SelectPathSizeLogit }

    public static enum DefaultStrategies { ReRoute, TimeAllocationMutator, ChangeLegMode, TimeAllocationMutator_ReRoute, 
    	ChangeSingleLegMode, ChangeSingleTripMode, SubtourModeChoice, ChangeTripMode, TripSubtourModeChoice }
    
    // yyyy Why are the following always implementing Providers of the full implementations, and not just the interface 
    // (i.e. Provider<GenericPlanSelector<Plan,Person>)?  kai, jan'15
    
    private static class ExpBetaPlanSelectorForRemoval implements Provider<ExpBetaPlanSelector<Plan, Person>> {

        private Config config;

        @Inject
        ExpBetaPlanSelectorForRemoval(Config config) {
            this.config = config;
        }

        @Override
        public ExpBetaPlanSelector<Plan, Person> get() {
            return new ExpBetaPlanSelector<>( - config.planCalcScore().getBrainExpBeta());
        }
    }

    private static class ExpBetaPlanChangerForRemoval implements Provider<ExpBetaPlanChanger<Plan, Person>> {

        private Config config;

        @Inject
        ExpBetaPlanChangerForRemoval(Config config) {
            this.config = config;
        }

        @Override
        public ExpBetaPlanChanger<Plan, Person> get() {
            return new ExpBetaPlanChanger<>( - config.planCalcScore().getBrainExpBeta());
        }
    }

    private static class PathSizeLogitSelectorForRemoval implements Provider<PathSizeLogitSelector> {

        private Scenario scenario;

        @Inject
        PathSizeLogitSelectorForRemoval(Scenario scenario) {
            this.scenario = scenario;
        }

        @Override
        public PathSizeLogitSelector get() {
            return new PathSizeLogitSelector(scenario.getConfig().planCalcScore().getPathSizeLogitBeta(), -scenario.getConfig().planCalcScore().getBrainExpBeta(),
                    scenario.getNetwork());
        }
    }

}
