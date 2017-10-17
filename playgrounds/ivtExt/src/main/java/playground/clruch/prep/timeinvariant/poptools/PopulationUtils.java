/**
 * 
 */
package playground.clruch.prep.timeinvariant.poptools;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.prep.timeinvariant.RemoveNonIntervalPlans;

/** @author Claudio Ruch */
public enum PopulationUtils {
    ;

    /** @return random time during daylength */
    /* package */ public static double getRandomDayTime() {
        return Constants.rand.nextDouble() * Constants.getDayLength();
    }

    /** removes all {@link Person} that do not have any {@link planElement} left int their single {@link Plan}
     * 
     * @param population */
    public static void removePeopleWithoutPlans(Population population) {
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

    /** filters population, i.e. removes all legs which do not have a start time in the interval and then removes
     * agents without any plans left.
     * 
     * @param interval
     * @param population */
    public static void filterTo(Interval interval, Population population) {
        GlobalAssert.that(interval.getDim() == 1);

        Map<Id<Person>, Person> people = (Map<Id<Person>, Person>) population.getPersons();
        for (Person person : people.values()) {
            RemoveNonIntervalPlans.of(person, interval);
        }

        PopulationUtils.removePeopleWithoutPlans(population);

        GlobalAssert.that(CountLegs.of(population, interval) == CountLegs.countLegsOf(population));
    }

}
