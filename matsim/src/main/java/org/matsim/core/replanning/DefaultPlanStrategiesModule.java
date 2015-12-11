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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.replanning.modules.*;
import org.matsim.core.replanning.selectors.*;

import javax.inject.Inject;
import javax.inject.Provider;

public class DefaultPlanStrategiesModule extends AbstractModule {
	
	public static enum DefaultPlansRemover { WorstPlanSelector, SelectRandom, SelectExpBetaForRemoval, ChangeExpBetaForRemoval, 
		PathSizeLogitSelectorForRemoval }

	public static final String Selector = null; ;

    @Override
    public void install() {
        addPlanSelectorForRemovalBinding(DefaultPlansRemover.WorstPlanSelector.toString()).to(WorstPlanForRemovalSelector.class);
        addPlanSelectorForRemovalBinding(DefaultPlansRemover.SelectRandom.toString()).to(new TypeLiteral<RandomPlanSelector<Plan, Person>>(){});
        addPlanSelectorForRemovalBinding(DefaultPlansRemover.SelectExpBetaForRemoval.toString()).toProvider(ExpBetaPlanSelectorForRemoval.class);
        addPlanSelectorForRemovalBinding(DefaultPlansRemover.ChangeExpBetaForRemoval.toString()).toProvider(ExpBetaPlanChangerForRemoval.class);
        addPlanSelectorForRemovalBinding(DefaultPlansRemover.PathSizeLogitSelectorForRemoval.toString()).toProvider(PathSizeLogitSelectorForRemoval.class);

        // strategy packages that only select:
        addPlanStrategyBinding(DefaultSelector.KeepLastSelected.toString()).toProvider(KeepLastSelectedPlanStrategyFactory.class);
        addPlanStrategyBinding(DefaultSelector.BestScore.toString()).toProvider(SelectBestPlanStrategyFactory.class);
        addPlanStrategyBinding(DefaultSelector.SelectExpBeta.toString()).toProvider(SelectExpBetaPlanStrategyFactory.class);
        addPlanStrategyBinding(DefaultSelector.ChangeExpBeta.toString()).toProvider(ChangeExpBetaPlanStrategyFactory.class);
        addPlanStrategyBinding(DefaultSelector.SelectRandom.toString()).toProvider(SelectRandomStrategyFactory.class);
        addPlanStrategyBinding(DefaultSelector.SelectPathSizeLogit.toString()).toProvider(SelectPathSizeLogitStrategyFactory.class);

        // strategy packages that select, copy, and modify.  (The copying is done implicitly as soon as "addStrategyModule" is called
        // at least once).
        addPlanStrategyBinding(DefaultStrategy.ReRoute.toString()).toProvider(ReRoutePlanStrategyFactory.class);
        addPlanStrategyBinding(DefaultStrategy.TimeAllocationMutator.toString()).toProvider(TimeAllocationMutatorPlanStrategyFactory.class);
        addPlanStrategyBinding(DefaultStrategy.TimeAllocationMutator_ReRoute.toString()).toProvider(TimeAllocationMutatorReRoutePlanStrategyFactory.class);

        addPlanStrategyBinding(DefaultStrategy.ChangeSingleLegMode.toString()).toProvider(ChangeSingleLegModeStrategyFactory.class);
        addPlanStrategyBinding(DefaultStrategy.ChangeLegMode.toString()).toProvider(ChangeLegModeStrategyFactory.class);
        addPlanStrategyBinding(DefaultStrategy.SubtourModeChoice.toString()).toProvider(SubtourModeChoiceStrategyFactory.class);

        addPlanStrategyBinding(DefaultStrategy.ChangeTripMode.toString()).toProvider(ChangeTripModeStrategyFactory.class);
        addPlanStrategyBinding(DefaultStrategy.ChangeSingleTripMode.toString()).toProvider(ChangeSingleTripModeStrategyFactory.class);
        addPlanStrategyBinding(DefaultStrategy.TripSubtourModeChoice.toString()).toProvider(TripSubtourModeChoiceStrategyFactory.class);
    }

    public static enum DefaultSelector { KeepLastSelected, BestScore, ChangeExpBeta, SelectExpBeta, SelectRandom, SelectPathSizeLogit }

    public static enum DefaultStrategy { ReRoute, TimeAllocationMutator, ChangeLegMode, TimeAllocationMutator_ReRoute, 
    	ChangeSingleLegMode, ChangeSingleTripMode, SubtourModeChoice, ChangeTripMode, TripSubtourModeChoice }
    
    // yyyy Why are the following always implementing Providers of the full implementations, and not just the interface 
    // (i.e. Provider<GenericPlanSelector<Plan,Person>)?  kai, jan'15
    
    private static class ExpBetaPlanSelectorForRemoval implements Provider<ExpBetaPlanSelector<Plan, Person>> {

        @Inject private PlanCalcScoreConfigGroup config;

        @Override
        public ExpBetaPlanSelector<Plan, Person> get() {
            return new ExpBetaPlanSelector<>( - config.getBrainExpBeta());
        }
    }

    private static class ExpBetaPlanChangerForRemoval implements Provider<ExpBetaPlanChanger<Plan, Person>> {

        @Inject private PlanCalcScoreConfigGroup config;

        @Override
        public ExpBetaPlanChanger<Plan, Person> get() {
            return new ExpBetaPlanChanger<>( - config.getBrainExpBeta());
        }
    }

    private static class PathSizeLogitSelectorForRemoval implements Provider<PathSizeLogitSelector> {

        @Inject PlanCalcScoreConfigGroup config;
        @Inject Network network;

        @Override
        public PathSizeLogitSelector get() {
            return new PathSizeLogitSelector(config.getPathSizeLogitBeta(), -config.getBrainExpBeta(),
                    network);
        }
    }

}
