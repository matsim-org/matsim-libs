/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

/** @author Claudio Ruch */
enum MinMaxTime {
    ;

    public static Interval of(Person person) {

        Interval minMax = new Interval(Tensors.vector(Double.MAX_VALUE), Tensors.vector(Double.MIN_VALUE));

        for (Plan plan : person.getPlans()) {
            for (PlanElement pE : plan.getPlanElements()) {
                if (pE instanceof Activity) {
                    Activity act = (Activity) pE;
                    Interval minMaxA = MinMaxTime.of(act);
                    minMax = IntervalWrap.of(minMax, minMaxA);
                }
                if (pE instanceof Leg) {
                    Leg leg = (Leg) pE;
                    Interval minMaxL = MinMaxTime.of(leg);
                    minMax = IntervalWrap.of(minMax, minMaxL);
                }
            }
        }
        return minMax;
    }

    public static Interval of(Activity activity) {
        double staTime = activity.getStartTime();
        double endTime = activity.getEndTime();

        boolean staTimeDefined = !(staTime == Double.NEGATIVE_INFINITY);
        boolean endTimeDefined = !(endTime == Double.NEGATIVE_INFINITY);

        if (staTimeDefined && endTimeDefined) {
            GlobalAssert.that(staTime <= endTime);
            return new Interval(Tensors.vector(staTime), Tensors.vector(endTime));
        }
        if (staTimeDefined && !endTimeDefined) {
            return new Interval(Tensors.vector(staTime), Tensors.vector(staTime));
        }
        if (!staTimeDefined && endTimeDefined) {
            return new Interval(Tensors.vector(endTime), Tensors.vector(endTime));
        }
        return null;
    }

    public static Interval of(Leg leg) {
        Tensor depTime = Tensors.vector(leg.getDepartureTime());
        return new Interval(depTime, depTime);
    }

}
