/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import java.util.Objects;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.idsc.queuey.util.GlobalAssert;

/** @author Claudio Ruch */
public enum MinMaxTime {
    ;

    public static double[] of(Person person) {

        double[] minMax = new double[] { Double.MAX_VALUE, Double.MIN_VALUE };

        for (Plan plan : person.getPlans()) {
            for (PlanElement pE : plan.getPlanElements()) {
                if (pE instanceof Activity) {
                    Activity act = (Activity) pE;
                    double[] minMaxA = MinMaxTime.of(act);
                    minMax = MinMaxTime.of(minMax, minMaxA);
                }
                if (pE instanceof Leg) {
                    Leg leg = (Leg) pE;
                    double[] minMaxL = MinMaxTime.of(leg);
                    minMax = MinMaxTime.of(minMax, minMaxL);
                }
            }
        }
        return minMax;
    }

    public static double[] of(Activity activity) {
        double staTime = activity.getStartTime();
        double endTime = activity.getEndTime();

        boolean staTimeDefined = !(staTime == Double.NEGATIVE_INFINITY);
        boolean endTimeDefined = !(endTime == Double.NEGATIVE_INFINITY);

        if (staTimeDefined && endTimeDefined) {
            GlobalAssert.that(staTime <= endTime);
            return new double[] { staTime, endTime };
        }
        if (staTimeDefined && !endTimeDefined) {
            return new double[] { staTime, staTime };
        }
        if (!staTimeDefined && endTimeDefined) {
            return new double[] { endTime, endTime };
        }
        return null;
    }

    public static double[] of(Leg leg) {
        double depTime = leg.getDepartureTime();
        return new double[] { depTime, depTime };
    }

    public static double[] of(double[] minMax, double[] i2) {
        if (Objects.isNull(i2))
            return minMax;
        double min = minMax[0] <= i2[0] ? minMax[0] : i2[0];
        double max = minMax[1] >= i2[1] ? minMax[1] : i2[1];
        return new double[] { min, max };
    }
}
