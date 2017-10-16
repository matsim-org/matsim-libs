/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import java.util.HashSet;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;

import ch.ethz.idsc.owly.data.GlobalAssert;

/** @author Claudio Ruch */
public enum RemoveNonIntervalPlans {
    ;

    public static void of(Person person, double[] interval) {
        GlobalAssert.that(interval.length == 2);
        List<Plan> plans = (List<Plan>) person.getPlans();
        for (Plan plan : plans) {

            Plan newPlan = PopulationUtils.createPlan();
            newPlan.setType(plan.getType());
            newPlan.setPerson(plan.getPerson());
            newPlan.setScore(plan.getScore());

            HashSet<Activity> activities = new HashSet<>();

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
                    if (depTime >= interval[0] && depTime <= interval[1]) {
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
