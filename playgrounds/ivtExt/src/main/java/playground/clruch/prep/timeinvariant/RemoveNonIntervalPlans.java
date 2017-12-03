/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;

import ch.ethz.idsc.owly.data.GlobalAssert;
import ch.ethz.idsc.tensor.Tensors;

/** @author Claudio Ruch */
public enum RemoveNonIntervalPlans {
    ;

    /** removes all plans of person which are not in the interval
     * 
     * @param person
     * @param interval */
    /* package */ static void of(Person person, Interval interval) {
        GlobalAssert.that(interval.getDim() == 1);

        for (Plan plan : person.getPlans()) {

            // create new plan
            Plan newPlan = PopulationUtils.createPlan();
            newPlan.setType(plan.getType());
            newPlan.setPerson(plan.getPerson());
            newPlan.setScore(plan.getScore());

            // iterate plans saving three plans
            PlanElement planE1 = null;
            PlanElement planE2 = null;
            PlanElement planE3 = null;
            for (PlanElement planENew : plan.getPlanElements()) {

                planE3 = planE2;
                planE2 = planE1;
                planE1 = planENew;

                if (planE2 instanceof Leg) {
                    Leg leg = (Leg) planE2;
                    double depTime = leg.getDepartureTime();
                    if (interval.contains(Tensors.vector(depTime))) {
                        GlobalAssert.that(planE3 instanceof Activity);
                        newPlan.addActivity((Activity) planE3);

                        newPlan.addLeg(leg);

                        GlobalAssert.that(planE1 instanceof Activity);
                        newPlan.addActivity((Activity) planE1);

                    }

                }
            }

            person.removePlan(plan);
            person.addPlan(newPlan);
        }
    }
}
