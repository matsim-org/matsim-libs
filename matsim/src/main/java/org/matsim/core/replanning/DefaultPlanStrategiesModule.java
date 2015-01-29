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

    @Override
    public void install() {
        addPlanSelectorForRemovalBinding("WorstPlanSelector").to(WorstPlanForRemovalSelector.class);
        addPlanSelectorForRemovalBinding("SelectRandom").to(new TypeLiteral<RandomPlanSelector<Plan, Person>>(){});
        addPlanSelectorForRemovalBinding("SelectExpBetaForRemoval").toProvider(ExpBetaPlanSelectorForRemoval.class);
        addPlanSelectorForRemovalBinding("ChangeExpBetaForRemoval").toProvider(ExpBetaPlanChangerForRemoval.class);
        addPlanSelectorForRemovalBinding("PathSizeLogitSelectorForRemoval").toProvider(PathSizeLogitSelectorForRemoval.class);

        // strategy packages that only select:
		addPlanStrategyBindingToFactory(Selector.KeepLastSelected.toString(), new KeepLastSelectedPlanStrategyFactory());
        addPlanStrategyBindingToFactory(Selector.BestScore.toString(), new SelectBestPlanStrategyFactory());
        addPlanStrategyBindingToFactory(Selector.SelectExpBeta.toString(), new SelectExpBetaPlanStrategyFactory());
        addPlanStrategyBindingToFactory(Selector.ChangeExpBeta.toString(), new ChangeExpBetaPlanStrategyFactory());
        addPlanStrategyBindingToFactory(Selector.SelectRandom.toString(), new SelectRandomStrategyFactory());
        addPlanStrategyBindingToFactory(Selector.SelectPathSizeLogit.toString(), new SelectPathSizeLogitStrategyFactory());

        // strategy packages that select, copy, and modify.  (The copying is done implicitly as soon as "addStrategyModule" is called
        // at least once).
        addPlanStrategyBindingToFactory(Names.ReRoute.toString(), new ReRoutePlanStrategyFactory());
        addPlanStrategyBindingToFactory(Names.TimeAllocationMutator.toString(), new TimeAllocationMutatorPlanStrategyFactory());
        addPlanStrategyBindingToFactory("TimeAllocationMutator_ReRoute", new TimeAllocationMutatorReRoutePlanStrategyFactory());
        addPlanStrategyBindingToFactory(Names.ChangeLegMode.toString(), new ChangeLegModeStrategyFactory());
        addPlanStrategyBindingToFactory("ChangeSingleLegMode", new ChangeSingleLegModeStrategyFactory());
        addPlanStrategyBindingToFactory("ChangeSingleTripMode", new ChangeSingleTripModeStrategyFactory());
        addPlanStrategyBindingToFactory("SubtourModeChoice", new SubtourModeChoiceStrategyFactory());
        addPlanStrategyBindingToFactory("ChangeTripMode", new ChangeTripModeStrategyFactory());
        addPlanStrategyBindingToFactory("TripSubtourModeChoice", new TripSubtourModeChoiceStrategyFactory());
    }

    public static enum Selector { KeepLastSelected, BestScore, ChangeExpBeta, SelectExpBeta, SelectRandom, SelectPathSizeLogit }

    public static enum Names { ReRoute, TimeAllocationMutator, ChangeLegMode }
    
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
