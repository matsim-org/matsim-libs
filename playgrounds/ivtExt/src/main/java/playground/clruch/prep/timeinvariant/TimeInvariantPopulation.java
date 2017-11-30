package playground.clruch.prep.timeinvariant;

import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;

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

        TimeInvariantPopulationUtils.filterTo(interval, population);

        System.out.println("total legs: " + CountLegs.countLegsOf(population));
        System.out.println("total interval legs: " + CountLegs.of(population, interval));

        return population;
    }

    /** @param interval
     * @param population
     * @return {@link Population} with legs from the @param interval resample over the entire day */
    /* package */ static Population from(Interval interval, Population population, Config config, Network network) {
        TimeInvariantPopulationUtils.filterTo(interval, population);
        return resampleDuringDay(interval, population, config, network);
    }

    private static Population resampleDuringDay(Interval interval, Population populationOld,
            Config config, Network network) {
        
        Population populationNew = PopulationUtils.createPopulation(config,network);

        // get unique IDs of all agents
        HashSet<Id<Person>> usedIDs = new HashSet<>();
        populationOld.getPersons().values().forEach(p -> usedIDs.add(p.getId()));
        GlobalAssert.that(usedIDs.size() == populationOld.getPersons().size());


        // calculate total for entire day
        int totalP = (int) ((Constants.getDayLength() / interval.getLength()[0]) * ((double) populationOld.getPersons().size()));
        System.out.println(populationOld.getPersons().size() + " in interval " + interval.print());
        System.out.println(totalP + " in interval " + 0 + "-->" + 108000);

        int addedPeople = 0;
        IDGenerator generator = new IDGenerator(usedIDs);
        for (int i = 0; i < totalP; ++i) {
            if (i % 500 == 0)
                System.out.println("creating person " + i + " of " + totalP);

            // adapt a random person to choice random time in the day
            Person newPerson = createNewPerson(TimeInvariantPopulationUtils.getRandomPerson(populationOld.getPersons()), //
                    generator, populationNew.getFactory());

            // add to new population
            if (Consistency.of(newPerson)) {
                populationOld.addPerson(newPerson);
                addedPeople++;
            }
        }
        System.out.println("addedPeople = " + addedPeople);
        System.out.println("populationSize = " + populationOld.getPersons().size());
        HashSet<Id<Person>> toRemove = new HashSet<>();
        populationOld.getPersons().entrySet().forEach(e -> { //
            if (!Consistency.of(e.getValue())) {
                // printDepTimes(e.getValue());
                toRemove.add(e.getKey());
                // GlobalAssert.that(false);
            }
        });

        // TODO this is a hack, not clear why this step is needed as lines 78-80 should avoid
        // adding any person that is not consistent.
        toRemove.stream().forEach(id -> populationOld.removePerson(id));
        System.out.println("populationSize = " + populationOld.getPersons().size());

        populationOld.getPersons().entrySet().forEach(e -> { //
            if (!Consistency.of(e.getValue())) {
                // printDepTimes(e.getValue());
                GlobalAssert.that(false);
            }
        });

        // forEach(//
        // p-> if(!consistencyOf(p)){
        // printDepTimes(p);
        // GlobalAssert.that(false);
        // })
        return populationOld;
    }

    /** @param randomP a {@link Person}
     * @return new {@link Person} identical to @param randomP starting its first travel at a randomly shifted time */
    public static Person createNewPerson(Person randomP, IDGenerator generator, PopulationFactory populationFactory) {
        Id<Person> newID = generator.generateUnusedID();
        
        boolean isConsistent = false;
        Person newPerson = null;
        int counter = 0;
        while (!isConsistent && counter < 20) {
            double timediff = TimeInvariantPopulationUtils.getRandomDayTimeShift();
            GlobalAssert.that(timediff>=-108000.0 && timediff<=108000.0);
            newPerson = ShiftedPerson.of(randomP, timediff, newID, populationFactory);
            isConsistent = Consistency.of(newPerson);
            ++counter;
        }
        if(counter==10){
            System.out.println("not able to shift person " + randomP.getId().toString());
            return null;
        }
        
        return newPerson;
    }

}