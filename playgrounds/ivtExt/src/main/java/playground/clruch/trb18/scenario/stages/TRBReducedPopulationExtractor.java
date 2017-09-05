package playground.clruch.trb18.scenario.stages;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class TRBReducedPopulationExtractor {
    final private static Logger logger = Logger.getLogger(TRBReducedPopulationExtractor.class);

    /**
     * Extracts ONLY the AV population to be used for the trip analysis of IDSC
     * - Expects that only agents with no subpopulation (i.e. no "noav") use AV
     * - They are expected to have a second plan, which includes AV legs
     * - This second plan is expected to be NOT the selected one
     *
     * The input population is not modified!
     */
    public Population run(Population population) {
        logger.info("Extracting reduced population for virtual networks etc ...");
        Population reducedPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());

        long numberOfAgents = 0;
        long numberOfReducedAgents = 0;

        for (Person person : population.getPersons().values()) {
            numberOfAgents++;

            if (population.getPersonAttributes().getAttribute(person.getId().toString(), "subpopulation") == null) {
                Person reducedPerson = reducedPopulation.getFactory().createPerson(person.getId());

                for (Plan plan : person.getPlans()) {
                    if (plan != person.getSelectedPlan()) {
                        reducedPerson.addPlan(PlanUtils.createCopy(plan));
                    }
                }

                reducedPopulation.addPerson(reducedPerson);
                numberOfReducedAgents++;
            }
        }

        logger.info(String.format("  Number of agents in population: %d", numberOfAgents));
        logger.info(String.format("  Number of agents in reduced population: %d (%.2f%%)", numberOfReducedAgents, 100.0 * numberOfReducedAgents / numberOfAgents));

        return reducedPopulation;
    }

    /**
     * For testing purposes
     */
    public static void main(String[] args) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(args[0]);
        new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes()).readFile(args[1]);

        Population reducedPopulation = new TRBReducedPopulationExtractor().run(scenario.getPopulation());
        new PopulationWriter(reducedPopulation).write(args[2]);
    }
}
