/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;

/** @author Claudio Ruch */
public enum Utils {
    ;

    public static void removeAllButFramedA(Person person, FramedActivity framedA) {
        List<Plan> plans = (List<Plan>) person.getPlans();

        for (Plan plan : plans) {

            // create adapted plan
            Plan newPlan = createAdapted(plan, framedA);

            // remove old, add new
            person.removePlan(plan);
            person.addPlan(newPlan);

        }
    }

    /** takes all Legs and Activities in plan which are not in framedA and adds them to newPlan
     * 
     * @param plan
     * @param newPlan
     * @param framedA */
    private static Plan createAdapted(Plan plan, FramedActivity framedA) {

        Plan newPlan = PopulationUtils.createPlan();
        newPlan.setType(plan.getType());
        newPlan.setPerson(plan.getPerson());
        newPlan.setScore(plan.getScore());

        for (PlanElement planENew : plan.getPlanElements()) {

            if (planENew instanceof Leg) {

                Leg legP = (Leg) planENew;
                if (framedA.leg.equals(legP)) {
                    newPlan.addLeg(legP);
                }

            }

            if (planENew instanceof Activity) {

                Activity actP = (Activity) planENew;

                if (actP.equals(framedA.aaft)) {
                    newPlan.addActivity(actP);
                }

                if (actP.equals(framedA.abef)) {
                    newPlan.addActivity(actP);
                }

            }

        }

        return newPlan;

    }

}
