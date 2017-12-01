/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.owly.data.GlobalAssert;
import ch.ethz.idsc.tensor.Tensors;

/** @author Claudio Ruch */
public enum CountLegs {
    ;

    /** @param population
     * @return number of {@link Leg} in population. */
    public static int countLegsOf(Population population) {
        return of(population, new Interval(Tensors.vector(-Double.MIN_VALUE), Tensors.vector(Double.MAX_VALUE)));
    }

    /** @param population
     * @param interval
     * @return number of {@link Leg} in population during interval */
    public static int of(Population population, Interval interval) {
        GlobalAssert.that(interval.getDim() == 1);
        int legCount = 0;

        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement planEl : plan.getPlanElements()) {
                    if (planEl instanceof Leg) {
                        Leg leg = (Leg) planEl;
                        double depTime = leg.getDepartureTime();
                        GlobalAssert.that(TimeConstants.getDayInterval().contains(Tensors.vector(depTime)));

                        if (interval.contains(Tensors.vector(depTime))) {
                            ++legCount;

                        }
                    }
                }
            }
        }
        return legCount;
    }

}
