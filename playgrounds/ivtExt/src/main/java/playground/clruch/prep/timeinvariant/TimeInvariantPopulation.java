/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
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

        removePeopleWithoutPlans(population);

        GlobalAssert.that(CountLegs.of(population, interval) == CountLegs.of(population));
    }

    private static void resampleDuringDay(Population population) {
        // TODO

    }

    /** removes all {@link Person} that do not have any {@link planElement} left int their single {@link Plan}
     * 
     * @param population */
    private static void removePeopleWithoutPlans(Population population) {

        HashSet<Id<Person>> unneededPeople = new HashSet<>();

        for (Entry<Id<Person>, ? extends Person> entry : population.getPersons().entrySet()) {
            List<Plan> plans = (List<Plan>) entry.getValue().getPlans();

            if (plans.size() == 1) {
                if (plans.get(0).getPlanElements().size() == 0) {
                    unneededPeople.add(entry.getKey());
                }
            }
        }

        unneededPeople.stream().forEach(id -> population.removePerson(id));

    }
}