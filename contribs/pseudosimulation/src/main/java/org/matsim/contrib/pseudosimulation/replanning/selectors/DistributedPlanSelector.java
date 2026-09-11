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

package org.matsim.contrib.pseudosimulation.replanning.selectors;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.selectors.PlanSelector;

public class DistributedPlanSelector implements PlanSelector<Plan, Person> {

    private final String delegateName;
    private final PlanCatcher slave;
    private final MatsimServices controler;
    private final double selectionFrequency;
    private PlanSelector<Plan, Person> delegate;


    public DistributedPlanSelector(MatsimServices controler, String delegateName, PlanCatcher slave, boolean quickReplanning, int selectionInflationFactor) {
        this.slave = slave;
        this.delegateName = delegateName;
        this.controler = controler;
//        when doing quickReplanning, the weight of the selection strategy is inflated by selectionInflationFactor, so it needs to be deflated by that much to prevent repeated execution
        this.selectionFrequency = 1 / (double) (selectionInflationFactor * (quickReplanning ? selectionInflationFactor : 1));
    }

    DistributedPlanSelector(PlanSelector<Plan, Person> delegate, PlanCatcher slave, double selectionFrequency) {
        this.controler = null;
        this.delegateName = null;
        this.slave = slave;
        this.selectionFrequency = selectionFrequency;
        this.delegate = delegate;
    }


    @Override
    public Plan selectPlan(HasPlansAndId<Plan, Person> person) {
        if (delegate == null) {
            PlanStrategy strategy = controler.getInjector()
                    .getBinding(Key.get(PlanStrategy.class, Names.named(delegateName)))
                    .getProvider().get();
            delegate = planSelectorOf(strategy);
        }

        if (MatsimRandom.getLocalInstance().nextDouble() <= this.selectionFrequency) {
            Plan plan = delegate.selectPlan(person);
            if (slave != null) slave.addPlansForPsim(plan);
            return plan;
        } else
            return person.getSelectedPlan();
    }

    static PlanSelector<Plan, Person> planSelectorOf(PlanStrategy strategy) {
        GenericPlanStrategyImpl<?, ?> genericStrategy = (GenericPlanStrategyImpl<?, ?>) strategy;
        return castPlanSelector(genericStrategy.getPlanSelector());
    }

    /**
     * The named {@link PlanStrategy} binding erases its plan and person types.
     * PSim registers person plan strategies at this boundary.
     */
    @SuppressWarnings("unchecked")
    private static PlanSelector<Plan, Person> castPlanSelector(PlanSelector<?, ?> selector) {
        return (PlanSelector<Plan, Person>) selector;
    }

}
