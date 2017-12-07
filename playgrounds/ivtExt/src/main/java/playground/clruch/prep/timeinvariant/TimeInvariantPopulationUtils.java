/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.queuey.util.GlobalAssert;

/** @author Claudio Ruch */
enum TimeInvariantPopulationUtils {
    ;



    /** @param people
     * @return random {@link Person} from the map */
    /* package */ static Person getRandomPerson(Map<Id<Person>, ? extends Person> people) {
        int el = TimeConstants.nextInt(people.size());
        return people.values().stream().collect(Collectors.toList()).get(el);
    }

    /** removes all {@link Person} that do not have any {@link planElement} left int their single {@link Plan}
     * 
     * @param population */
    /* package */ static void removePeopleWithoutPlans(Population population) {
        HashSet<Id<Person>> unneededPeople = new HashSet<>();
        for (Entry<Id<Person>, ? extends Person> entry : population.getPersons().entrySet()) {
            @SuppressWarnings("unchecked")
            List<Plan> plans = (List<Plan>) entry.getValue().getPlans();

            if (plans.size() == 1) {
                if (plans.get(0).getPlanElements().size() == 0) {
                    unneededPeople.add(entry.getKey());
                }
            }
        }
        unneededPeople.stream().forEach(id -> population.removePerson(id));
    }

    /** filters population, i.e. removes all legs which do not have a start time in the interval and then removes
     * agents without any plans left.
     * 
     * @param interval
     * @param population */
    /* package */ static void filterTo(Interval interval, Population population) {
        GlobalAssert.that(interval.getDim() == 1);

        @SuppressWarnings("unchecked")
        Map<Id<Person>, Person> people = (Map<Id<Person>, Person>) population.getPersons();
        for (Person person : people.values()) {
            RemoveNonIntervalPlans.of(person, interval);
        }

        TimeInvariantPopulationUtils.removePeopleWithoutPlans(population);

        GlobalAssert.that(CountLegs.of(population, interval) == CountLegs.countLegsOf(population));
    }

}
