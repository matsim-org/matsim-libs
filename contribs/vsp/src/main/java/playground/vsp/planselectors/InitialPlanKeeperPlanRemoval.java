/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.vsp.planselectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;

/**
 * The idea is to keep the initial plans or copy of then which are tagged in the beginning.
 *
 * Created by amit on 11.06.18.
 */

public class InitialPlanKeeperPlanRemoval implements PlanSelector<Plan, Person> {

    public static final String initial_plans_keeper_plan_remover = "INITIAL_PLANS_KEEPER_REMOVAL";
    public static final String plan_attribute_name = "initial_plan_attribute_name";
    public static final String plan_attribute_prefix = "initial_plan_attribute_";

    private final WorstPlanForRemovalSelector delegate;

    @Inject
    public InitialPlanKeeperPlanRemoval(StrategyConfigGroup strategyConfigGroup, Population population){
        this.delegate = new WorstPlanForRemovalSelector();
        if ( strategyConfigGroup.getMaxAgentPlanMemorySize() < 12) {
            Logger.getLogger(InitialPlanKeeperPlanRemoval.class).warn("A plans remover is used which keeps the initial plans or at least their copy \n " +
                    "and maximum number of plans in the choice set is limited to "+ strategyConfigGroup.getMaxAgentPlanMemorySize()+
            ".\n Lower number of plans in choice set is likely to end up in infinite loop. Setting it to 15.");
            strategyConfigGroup.setMaxAgentPlanMemorySize(15);
        }

        for (Person person : population.getPersons().values()){
            for (int index =0; index < person.getPlans().size(); index++) {
                Plan plan = person.getPlans().get(index);
                plan.getAttributes().putAttribute( InitialPlanKeeperPlanRemoval.plan_attribute_name, InitialPlanKeeperPlanRemoval.plan_attribute_prefix+index);
            }
        }
    }

    @Override
    public Plan selectPlan(HasPlansAndId<Plan, Person> member) {
        List<Plan> temporarilyRemovedPlans = new ArrayList<>();

        // WorstPlansForRemovalSelector will always return same plan if no change to choice set.
        // let's remove it --> store it --> get next worst plan remove it
        // repeat the process until a suitable plan is found.
        //put all plans back to the choice set.
        // Imp: putting plans back may change the positions of the plans in the choice set, assuming that it will not change the behaviour somewhere else.
        while ( true ) {
            Plan planForRemoval = delegate.selectPlan(member);

            Object value = planForRemoval.getAttributes().getAttribute(plan_attribute_name);
            if (value==null) throw new RuntimeException("The value for the attribute key "+plan_attribute_name+" does not exist in the plan for person "+ member.getId().toString()+".");

            // check if there exists a plan with same index
            int occurrences = (int) member.getPlans()
                                          .stream()
                                          .filter(pl -> Objects.equals(pl.getAttributes().getAttribute(plan_attribute_name),
                                                  value))
                                          .count();
            if (occurrences <= 1) { // occurrences could also be zero
                temporarilyRemovedPlans.add(planForRemoval);
                member.getPlans().remove(planForRemoval);
            } else {
                temporarilyRemovedPlans.forEach(member::addPlan);
                return planForRemoval;
            }
        }
    }
}
