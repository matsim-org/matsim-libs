/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.queuey.util.GlobalAssert;

/** @author Claudio Ruch */
public enum TimeInvariantPopulation {
    ;

    /** @param interval
     * @param population
     * @return {@link Population} consisting only of legs with a departure time in the @param interval */
    public static Population at(double[] interval, Population population) {

        GlobalAssert.that(interval.length == 2);
        System.out.println("calc. time-invariant pop. from " + interval[0] + " to " + interval[1]);

        System.out.println("total legs: " + CountLegs.of(population));
        System.out.println("total interval legs: " + CountLegs.of(population, interval));

        filterTo(interval, population);

        System.out.println("total legs: " + CountLegs.of(population));
        System.out.println("total interval legs: " + CountLegs.of(population, interval));

        return population;
    }

    /** @param interval
     * @param population
     * @return {@link Population} with legs from the @param interval resample over the entire day */
    public static Population from(double[] interval, Population population) {
        filterTo(interval, population);
        resampleDuringDay(population);

        return population;
    }

    private static void filterTo(double[] interval, Population population) {
        GlobalAssert.that(interval.length == 2);
        Map<Id<Person>, Person> people = (Map<Id<Person>, Person>) population.getPersons();
        for (Person person : people.values()) {
            RemoveNonIntervalPlans.of(person, interval);
        }
        GlobalAssert.that(CountLegs.of(population, interval) == CountLegs.of(population));
    }

    private static void resampleDuringDay(Population population) {
        // 
        

    }

}