/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.pieter.distributed.replanning.selectors;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.GenericPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;
import playground.pieter.distributed.SlaveControler;


public class DistributedPlanSelector<T extends PlanStrategyFactory>implements PlanSelector {

    private double selectionFrequency;
    T delegateFactory;
    GenericPlanSelector delegate;
    SlaveControler slave;

    public DistributedPlanSelector(T delegateFactory, SlaveControler slave) {
        this.slave = slave;
        this.delegate = ((PlanStrategyImpl) delegateFactory.createPlanStrategy(slave.getMATSimControler().getScenario(),slave.getMATSimControler().getEvents())).getPlanSelector();
        this.selectionFrequency=1/slave.numberOfPSimIterationsPerCycle;
    }

    @Override
    public Plan selectPlan(HasPlansAndId<Plan, Person> person) {
        if (MatsimRandom.getLocalInstance().nextDouble() <= this.selectionFrequency){
            Plan plan = (Plan)delegate.selectPlan(person);
            slave.addPlansForPsim(plan);
            return plan;
        }
        else
            return person.getSelectedPlan();
    }

}
