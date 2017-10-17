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
import playground.clruch.prep.timeinvariant.poptools.Constants;
import playground.clruch.prep.timeinvariant.poptools.CountLegs;
import playground.clruch.prep.timeinvariant.poptools.Interval;
import playground.clruch.prep.timeinvariant.poptools.PopulationUtils;

/** @author Claudio Ruch */
public enum TimeInvariantPopulation {
    ;

    /** @param interval
     * @param population
     * @return {@link Population} consisting only of legs with a departure time in the @param interval */
    public static Population at(Interval interval, Population population) {
        GlobalAssert.that(interval.getDim() == 1);
        System.out.println("calc. time-invariant pop. in time interval ");
        System.out.println(interval.print());

        System.out.println("total legs: " + CountLegs.countLegsOf(population));
        System.out.println("total interval legs: " + CountLegs.of(population, interval));

        PopulationUtils.filterTo(interval, population);

        System.out.println("total legs: " + CountLegs.countLegsOf(population));
        System.out.println("total interval legs: " + CountLegs.of(population, interval));

        return population;
    }

    /** @param interval
     * @param population
     * @return {@link Population} with legs from the @param interval resample over the entire day */
    public static Population from(Interval interval, Population population) {
        PopulationUtils.filterTo(interval, population);
        return resampleDuringDay(interval, population);
    }



    private static Population resampleDuringDay(Interval interval, Population population) {

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

        int totalP = (int) ((Constants.getDayLength() / interval.getLength()[0]) * ((double) people.size()));
        System.out.println(people.size() + " in interval " + interval.print());
        System.out.println(totalP + " in interval " + 0 + " - " + 108000);

        for (int i = 0; i < totalP; ++i) {
            if (i % 500 == 0)
                System.out.println("creating person " + i + " of " + totalP);

            // take random person from "people"
            Person randomP = getRandomPerson(people);

            // select random time in day
            double time = PopulationUtils.getRandomDayTime();

            // adapt person to choice
            Person newPerson = createNewPerson(randomP, time, usedIDs);

            // add to new population
            population.addPerson(newPerson);

        }

        return population;

    }



    /** @param people
     * @return random {@link Person} from the map */
    private static Person getRandomPerson(HashMap<Id<Person>, ? extends Person> people) {
        int el = Constants.nextInt(people.size());
        return people.values().stream().collect(Collectors.toList()).get(el);
    }

    /** @param randomP a {@link Person}
     * @param time {@link double} when the person should start its travel
     * @return new {@link Person} identical to @param randomP starting its first travel at @param time */
    private static Person createNewPerson(Person randomP, double time, HashSet<Id<Person>> usedIDs) {
        Id<Person> newID = generateUnusedID(usedIDs);
        usedIDs.add(newID);
        Person newPerson = new PersonImplAdd(newID);

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

    /** @param usedIDs
     * @return new ID which is not yet in set usedIDs */
    private static Id<Person> generateUnusedID(HashSet<Id<Person>> usedIDs) {
        Integer i = 0;
        Id<Person> newId;
        do {
            ++i;
            String newIDs = Integer.toString(i);
            newId = Id.create(newIDs, Person.class);
        } while (usedIDs.contains(newId));

        return newId;
    }

}