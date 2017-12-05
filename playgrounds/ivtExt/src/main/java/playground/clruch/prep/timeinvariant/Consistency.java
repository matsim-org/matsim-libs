/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.idsc.tensor.Tensors;

/** @author Claudio Ruch */
public enum Consistency {
    ;

    /**
     * 
     * @param person
     * @return true if all timestamps of person are within the specified day
     */
    /* package */static boolean of(Person person) {
        if (person == null)
            return false;
        boolean isOk = true;
        for (Plan plan : person.getPlans()) {
            for (PlanElement planElement : plan.getPlanElements()) {
                if (planElement instanceof Leg) {
                    Leg leg = (Leg) planElement;
                    if (!Consistency.of(leg)) {
                        isOk = false;
                        break;
                    }
                }
                if (planElement instanceof Activity) {
                    Activity act = (Activity) planElement;
                    if (!Consistency.of(act)) {
                        isOk = false;
                        break;
                    }
                }
            }
        }
        return isOk;
    }

    private static boolean of(Activity act) {
        boolean isConsistent = true;
        double startTime = act.getStartTime();
        double endTime = act.getEndTime();

        if (startTime != Double.NEGATIVE_INFINITY && !isInDay(startTime)) {
            System.out.println("startTime = " + startTime);
            isConsistent = false;
        }

        if (endTime != Double.NEGATIVE_INFINITY && !isInDay(endTime)) {
            System.out.println("endTime = " + endTime);
            isConsistent = false;
        }

        return isConsistent;
    }

    private static boolean of(Leg leg) {
        boolean isConsistent = true;
        double depTime = leg.getDepartureTime();
        if (!isInDay(depTime)) {
            System.out.println("depTime = " + depTime);
            isConsistent = false;
        }
        return isConsistent;
    }

    public static boolean isInDay(double time) {
        return TimeConstants.getDayInterval().contains(Tensors.vector(time));
    }
}
