/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.owly.data.GlobalAssert;

/** @author Claudio Ruch */
public enum CountLegs {
    ;
    private static double TIME_MIN = 0.0; // TODO Magic const.
    private static double TIME_MAX = 108000.0; // TODO Magic const.

    public static int of(Population population) {
        return of(population, new double[] { -Double.MIN_VALUE , Double.MAX_VALUE });
    }

    public static int of(Population population, double[] interval) {
        GlobalAssert.that(interval.length == 2);
        double min = interval[0];
        double max = interval[1];

        int legCount = 0;

        Map<Id<Person>, Person> people = (Map<Id<Person>, Person>) population.getPersons();

        for (Person person : people.values()) {
            List<Plan> plans = (List<Plan>) person.getPlans();
            for (Plan plan : plans) {
                for (PlanElement planEl : plan.getPlanElements()) {
                    if (planEl instanceof Leg) {
                        Leg leg = (Leg) planEl;
                        double depTime = leg.getDepartureTime();
                        GlobalAssert.that(depTime >= TIME_MIN);
                        GlobalAssert.that(depTime <= TIME_MAX);
                        

                        if (min <= depTime && depTime <= max) {
                            ++legCount;

                        }
                    }
                }
            }
        }
        return legCount;
    }

}
