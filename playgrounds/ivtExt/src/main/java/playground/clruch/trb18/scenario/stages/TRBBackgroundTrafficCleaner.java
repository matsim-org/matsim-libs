package playground.clruch.trb18.scenario.stages;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class TRBBackgroundTrafficCleaner {
    static private final Logger logger = Logger.getLogger(TRBBackgroundTrafficCleaner.class);

    /**
     * Cleans the population from background traffic
     * - Population should already include (unselected) AV plans
     * - Agents which are not using AVs should be marked with the "noav" subpopulation attribute
     */
    public void clean(Population population) {
        logger.info("Cleaning background traffic from population ...");

        long numberOfAgents = 0;
        long numberOfRemovedAgents = 0;

        Iterator<? extends Person> personIterator = population.getPersons().values().iterator();

        while (personIterator.hasNext()) {
            Person person = personIterator.next();
            numberOfAgents++;

            if (population.getPersonAttributes().getAttribute(person.getId().toString(), "subpopulation") == null) {
                continue;
            }

            personIterator.remove();
            population.getPersonAttributes().removeAllAttributes(person.getId().toString());
            numberOfRemovedAgents++;
        }

        logger.info(String.format("  Number of agents: %d", numberOfAgents));
        logger.info(String.format("  Number of remaining agents: %d (%.2f%%)", numberOfAgents - numberOfRemovedAgents, 100.0 * (numberOfAgents - numberOfRemovedAgents) / numberOfAgents));
    }

    /**
     * For testing purposes
     */
    static public void main(String[] args) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(args[0]);
        new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes()).readFile(args[1]);
        new TRBBackgroundTrafficCleaner().clean(scenario.getPopulation());
        new PopulationWriter(scenario.getPopulation()).write(args[2]);
    }
}
