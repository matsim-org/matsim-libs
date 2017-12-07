package playground.clruch.prep.timeinvariant;

import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;

import ch.ethz.idsc.queuey.util.GlobalAssert;

/** @author Claudio Ruch */
enum TimeInvariantPopulation {
    ;

    /** @param interval
     * @param population
     * @return {@link Population} with legs from the @param interval resample over the entire day */
    /* package */ static Population from(Interval interval, Population population, Config config, Network network) {
        TimeInvariantPopulationUtils.filterTo(interval, population);
        return resampleDuringDay(interval, population, config, network);
    }

    /** @param interval interval during which the population should be resampled
     * @param populationOld original population
     * @param config config file
     * @param network network
     * @return population for entire day resampled from populationOld during interval */
    private static Population resampleDuringDay(Interval interval, Population populationOld, //
            Config config, Network network) {

        Population populationNew = PopulationUtils.createPopulation(config, network);

        // get unique IDs of all agents
        HashSet<Id<Person>> usedIDs = new HashSet<>();
        populationOld.getPersons().values().forEach(p -> usedIDs.add(p.getId()));
        GlobalAssert.that(usedIDs.size() == populationOld.getPersons().size());

        // calculate total for entire day
        int totalP = (int) ((TimeConstants.getDayLength() / interval.getLength().Get(0).number().doubleValue()) * ((double) populationOld.getPersons().size()));
        System.out.println(populationOld.getPersons().size() + " in interval " + interval);
        System.out.println(totalP + " in interval " + 0 + "-->" + 108000);

        IDGenerator generator = new IDGenerator(usedIDs);
        for (int i = 0; i < totalP; ++i) {
            if (i % 500 == 0)
                System.out.println("creating person " + populationNew.getPersons().size() + " of " + totalP);

            // adapt a random person to choice random time in the day
            Id<Person> newID = generator.generateUnusedID();
            Person newPerson = ShiftedPerson.of(//
                    TimeInvariantPopulationUtils.getRandomPerson(populationOld.getPersons()), //
                    newID, populationNew.getFactory());

            if (Consistency.of(newPerson)) {
                populationNew.addPerson(newPerson);
            }

        }

        return populationNew;

    }

}