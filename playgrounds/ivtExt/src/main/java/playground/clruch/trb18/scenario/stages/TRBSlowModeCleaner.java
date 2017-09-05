package playground.clruch.trb18.scenario.stages;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class TRBSlowModeCleaner {
    final static private Logger logger = Logger.getLogger(TRBSlowModeCleaner.class);

    /**
     * Cleans the population from plans that do solely include slow modes (walk and bike)
     * - Requires a population with "noav" tags for quicker filtering (and NO other subpopulations!)
     * - The non-AV plan should be selected!
     */
    public void clean(Population population) {
        logger.info("Cleaning slow mode agents from the population ...");
        Iterator<? extends Person> personIterator = population.getPersons().values().iterator();

        long numberOfAgents = 0;
        long numberOfRemovedAgents = 0;

        while (personIterator.hasNext()) {
            Person person = personIterator.next();
            numberOfAgents++;

            if (population.getPersonAttributes().getAttribute(person.getId().toString(), "subpopulation") == null) {
                continue;
            }

            boolean hasCarOrPtLeg = false;

            for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan(), new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE))) {
                Leg leg = trip.getLegsOnly().get(0);

                if (leg.getMode().equals("car") || leg.getMode().equals("pt")) {
                    hasCarOrPtLeg = true;
                    break;
                }
            }

            if (hasCarOrPtLeg) {
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
        new TRBSlowModeCleaner().clean(scenario.getPopulation());
        new PopulationWriter(scenario.getPopulation()).write(args[2]);
    }
}
