package playground.clruch.prep.timeinvariant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.queuey.util.GlobalAssert;

/** @author Claudio Ruch */
public enum TimeInvariantPopulation {
    ;

    private static double dayLength = 108000.0; // TODO magic const.
    private static int randomSeed = 12345;
    private static Random rand = new Random(randomSeed);

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
        return resampleDuringDay(interval, population);
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

    private static Population resampleDuringDay(double[] interval, Population population) {
        GlobalAssert.that(interval[1] >= interval[0]);

        // get unique IDs of all agents
        HashSet<Id<Person>> usedIDs = new HashSet<>();
        population.getPersons().values().forEach(p -> usedIDs.add(p.getId()));
        GlobalAssert.that(usedIDs.size() == population.getPersons().size());

        // save people
        HashMap<Id<Person>, Person> people = new HashMap<>();
        population.getPersons().entrySet().stream().forEach(e -> people.put(e.getKey(), e.getValue()));

        // remove all people from population
        people.keySet().stream().forEach(i -> population.removePerson(i));

        // calculate total for entire day
        double timeSpan = interval[1] - interval[0];
        int totalP = (int) ( (dayLength/timeSpan ) * ((double) people.size()));
        System.out.println(people.size() + " in interval " + interval[0] + " - " + interval[1]);
        System.out.println(totalP + " in interval " + 0 + " - " + 108000);

        for (int i = 0; i < totalP; ++i) {
            // take random person from "people"
            Person randomP = getRandomPerson(people);

            // select random time in day
            double time = getRandomDayTime();

            // adapt person to choice
            Person newPerson = createNewPerson(randomP, time, usedIDs);

            // add to new population
            population.addPerson(newPerson);

        }

        return population;

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

    /** @param people
     * @return random {@link Person} from the map */
    private static Person getRandomPerson(HashMap<Id<Person>, ? extends Person> people) {
        int el = rand.nextInt(people.size());
        return people.values().stream().collect(Collectors.toList()).get(el);
    }

    /** @return random time during daylength */
    private static double getRandomDayTime() {
        return rand.nextDouble() * dayLength;
    }

    /** @param randomP a {@link Person}
     * @param time {@link double} when the person should start its travel
     * @return new {@link Person} identical to @param randomP starting its first travel at @param time */
    private static Person createNewPerson(Person randomP, double time, HashSet<Id<Person>> usedIDs) {
        Integer i = 0;
        Id<Person> newId;

        do {
            ++i;
            String newIDs = Integer.toString(i);
            newId = Id.create(newIDs, Person.class);
        } while (usedIDs.contains(newId));

        Person newPerson = new PersonImplAdd(newId);

        PlanElement pEFirst = randomP.getPlans().get(0).getPlanElements().get(0);
        GlobalAssert.that(pEFirst instanceof Activity);
        Activity firstActivity = (Activity) pEFirst;
        double timediff = time - firstActivity.getEndTime();

        for (Plan plan : randomP.getPlans()) {
            Plan planShifted = new PlanImplAdd();
            planShifted.setPerson(newPerson);
            planShifted.setScore(plan.getScore());
            planShifted.setType(plan.getType());

            for (PlanElement pE : plan.getPlanElements()) {
                if (pE instanceof Activity) {
                    Activity act = (Activity) pE;
                    act.setStartTime(act.getStartTime() + timediff);
                    act.setEndTime(act.getEndTime() + timediff);
                    planShifted.addActivity(act);
                }
                if (pE instanceof Leg) {
                    Leg leg = (Leg) pE;
                    leg.setDepartureTime(leg.getDepartureTime() + timediff);
                    planShifted.addLeg(leg);
                }
            }
            newPerson.addPlan(planShifted);
        }
        return newPerson;
    }
}