package playground.clruch.prep.timeinvariant;

import java.util.HashMap;
import java.util.HashSet;

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

    /** @param interval
     * @param population
     * @return {@link Population} consisting only of legs with a departure time in the @param interval */
    /* package */ static Population at(Interval interval, Population population) {
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
    /* package */ static Population from(Interval interval, Population population) {
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
        System.out.println(totalP + " in interval " + 0 + "-->" + 108000);

        IDGenerator generator = new IDGenerator(usedIDs);
        for (int i = 0; i < totalP; ++i) {
            if (i % 500 == 0)
                System.out.println("creating person " + i + " of " + totalP);

            // adapt a random person to choice random time in the day
            Person newPerson = createNewPerson(PopulationUtils.getRandomPerson(people), //
                    PopulationUtils.getRandomDayTime(), generator);

            // add to new population
            if (consistencyOf(newPerson)) {
                population.addPerson(newPerson);
            }
        }
        return population;
    }

    /** @param randomP a {@link Person}
     * @param time {@link double} when the person should start its travel
     * @return new {@link Person} identical to @param randomP starting its first travel at @param time */
    private static Person createNewPerson(Person randomP, double time, IDGenerator generator) {

        Id<Person> newID = generator.generateUnusedID();
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

    private static boolean consistencyOf(Person person) {
        boolean isOk = true;
        for (Plan plan : person.getPlans()) {
            for (PlanElement planElement : plan.getPlanElements()) {
                if (planElement instanceof Leg) {
                    Leg leg = (Leg) planElement;
                    double depTime = leg.getDepartureTime();
                    if (depTime < 0.0 || depTime >= 108000.0) { // TODO magic const.
                        isOk = false;
                    }
                }
            }
        }
        return isOk;
    }
}