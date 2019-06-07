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

package org.matsim.core.replanning.strategies;

import com.google.inject.TypeLiteral;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.PathSizeLogitSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashSet;
import java.util.Set;

public class DefaultPlanStrategiesModule extends AbstractModule {
    private static final Logger log = Logger.getLogger( DefaultPlanStrategiesModule.class );

    public enum DefaultPlansRemover { WorstPlanSelector, SelectRandom, SelectExpBetaForRemoval, ChangeExpBetaForRemoval,
		PathSizeLogitSelectorForRemoval }

    @Override
    public void install() {
        if (getConfig().strategy().getPlanSelectorForRemoval().equals(DefaultPlansRemover.WorstPlanSelector.toString())) {
            bindPlanSelectorForRemoval().to(WorstPlanForRemovalSelector.class);
        }
        if (getConfig().strategy().getPlanSelectorForRemoval().equals(DefaultPlansRemover.SelectRandom.toString())) {
            bindPlanSelectorForRemoval().to(new TypeLiteral<RandomPlanSelector<Plan, Person>>(){});
        }
        if (getConfig().strategy().getPlanSelectorForRemoval().equals(DefaultPlansRemover.SelectExpBetaForRemoval.toString())) {
            bindPlanSelectorForRemoval().toProvider(ExpBetaPlanSelectorForRemoval.class);
        }
        if (getConfig().strategy().getPlanSelectorForRemoval().equals(DefaultPlansRemover.ChangeExpBetaForRemoval.toString())) {
            bindPlanSelectorForRemoval().toProvider(ExpBetaPlanChangerForRemoval.class);
        }
        if (getConfig().strategy().getPlanSelectorForRemoval().equals(DefaultPlansRemover.PathSizeLogitSelectorForRemoval.toString())) {
            bindPlanSelectorForRemoval().toProvider(PathSizeLogitSelectorForRemoval.class);
        }

        // We only bind those stategies that the StrategyManager is actually going to use,
        // according to the Config.
        // If you bind your own strategy from your own module, and then don't reference it from the Config,
        // that's fine: The StrategyManager will still only add those strategies to itself which are configured.
        // But we don't want to clutter the container here.
        Set<String> usedStrategyNames = new HashSet<>();
        for (StrategyConfigGroup.StrategySettings settings : getConfig().strategy().getStrategySettings()) {
            usedStrategyNames.add(settings.getStrategyName());
        }

        // strategy packages that only select:
        if (usedStrategyNames.contains(DefaultSelector.KeepLastSelected)) {
            addPlanStrategyBinding(DefaultSelector.KeepLastSelected).toProvider(KeepLastSelected.class);
        }
        if (usedStrategyNames.contains(DefaultSelector.BestScore)) {
            addPlanStrategyBinding(DefaultSelector.BestScore).toProvider(SelectBest.class);
        }
        if (usedStrategyNames.contains(DefaultSelector.SelectExpBeta)) {
            addPlanStrategyBinding(DefaultSelector.SelectExpBeta).toProvider(SelectExpBeta.class);
        }
        if (usedStrategyNames.contains(DefaultSelector.ChangeExpBeta)) {
            addPlanStrategyBinding(DefaultSelector.ChangeExpBeta).toProvider(ChangeExpBeta.class);
        }
        if (usedStrategyNames.contains(DefaultSelector.SelectRandom)) {
            addPlanStrategyBinding(DefaultSelector.SelectRandom).toProvider(SelectRandom.class);
        }
        if (usedStrategyNames.contains(DefaultSelector.SelectPathSizeLogit)) {
            addPlanStrategyBinding(DefaultSelector.SelectPathSizeLogit).toProvider(SelectPathSizeLogit.class);
        }

        // strategy packages that select, copy, and modify.  (The copying is done implicitly as soon as "addStrategyModule" is called
        // at least once).

        if (usedStrategyNames.contains(DefaultStrategy.ReRoute)) {
            addPlanStrategyBinding(DefaultStrategy.ReRoute).toProvider(ReRoute.class);
        }
        if (usedStrategyNames.contains(DefaultStrategy.TimeAllocationMutator)) {
            addPlanStrategyBinding(DefaultStrategy.TimeAllocationMutator).toProvider(TimeAllocationMutator.class);
        }
        if (usedStrategyNames.contains(DefaultStrategy.TimeAllocationMutator_ReRoute)) {
            addPlanStrategyBinding(DefaultStrategy.TimeAllocationMutator_ReRoute).toProvider(TimeAllocationMutatorReRoute.class);
        }
        if (usedStrategyNames.contains(DefaultStrategy.SubtourModeChoice)) {
            addPlanStrategyBinding(DefaultStrategy.SubtourModeChoice).toProvider(SubtourModeChoice.class);
        }
        if (usedStrategyNames.contains(DefaultStrategy.ChangeTripMode)) {
            addPlanStrategyBinding(DefaultStrategy.ChangeTripMode).toProvider(ChangeTripMode.class);
        }
        if (usedStrategyNames.contains(DefaultStrategy.ChangeSingleTripMode)) {
            addPlanStrategyBinding(DefaultStrategy.ChangeSingleTripMode).toProvider(ChangeSingleTripMode.class);
        }

        // td, 15 feb 16: removed the "Leg" versions of strategies. Notify the users that they should switch to the
        // "Trip" versions. Should be left in 0.8.XXX releases, and then deleted, along with their name in the enum.
        if ( usedStrategyNames.contains(DefaultStrategy.ChangeLegMode) ) {
            log.error( DefaultStrategy.ChangeLegMode+" replanning strategy does not exist anymore. Please use "+DefaultStrategy.ChangeTripMode+" instead." );
        }
        if ( usedStrategyNames.contains(DefaultStrategy.ChangeSingleLegMode) ) {
            log.error( DefaultStrategy.ChangeSingleLegMode+" replanning strategy does not exist anymore. Please use "+DefaultStrategy.ChangeSingleTripMode+" instead." );
        }
        if ( usedStrategyNames.contains(DefaultStrategy.TripSubtourModeChoice) ) {
            log.error( DefaultStrategy.TripSubtourModeChoice+" replanning strategy does not exist anymore. Please use "+DefaultStrategy.SubtourModeChoice+" instead." );
        }
    }

    public interface DefaultSelector {
	    String KeepLastSelected="KeepLastSelected" ;
	    String BestScore="BestScore";
	    String ChangeExpBeta="ChangeExpBeta";
	    String SelectExpBeta="SelectExpBeta";
	    String SelectRandom="SelectRandom";
	    String SelectPathSizeLogit="SelectPathSizeLogit" ;
    }
    

    public interface DefaultStrategy {
        String ReRoute="ReRoute";
        String TimeAllocationMutator="TimeAllocationMutator";
        @Deprecated String ChangeLegMode="ChangeLegMode";
        String TimeAllocationMutator_ReRoute="TimeAllocationMutator_ReRoute" ;
    	@Deprecated String ChangeSingleLegMode = "ChangeSingleLegMode" ;
    	String ChangeSingleTripMode="ChangeSingleTripMode" ;
    	String SubtourModeChoice = "SubtourModeChoice" ;
    	String ChangeTripMode = "ChangeTripMode" ;
    	@Deprecated String TripSubtourModeChoice = "TripSubtourModeChoice" ;
    }
    
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
