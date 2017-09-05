package playground.clruch.trb18.scenario.stages;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class TRBAVPlanSelector {
    final private static Logger logger = Logger.getLogger(TRBAVPlanSelector.class);

    final private Random random;

    public TRBAVPlanSelector(Random random) {
        this.random = random;
    }

    /**
     * Selects the AV plan with a certain probability
     * - 1.0 means ALL agents will use AV
     * - Expects that AV plan is NOT selected and that there are only two plans
     * - Expects that all agents that do not have an AV plan are in subpopulation "noav"
     */
    public void selectAVPlans(Population population, double probability) {
        logger.info(String.format("Selecting AV plans (Probability %f)", probability));

        long numberOfAgents = 0;
        long numberOfPotentialAVAgents = 0;
        long numberOfActualAVAgents = 0;

        for (Person person : population.getPersons().values()) {
            if (population.getPersonAttributes().getAttribute(person.getId().toString(), "subpopulation") == null) {
                if (random.nextDouble() <= probability) {
                    Plan unselectedPlan = null;

                    for (Plan plan : person.getPlans()) {
                        if (plan != person.getSelectedPlan()) {
                            unselectedPlan = plan;
                        }
                    }

                    person.setSelectedPlan(unselectedPlan);
                    numberOfActualAVAgents++;
                }

                numberOfPotentialAVAgents++;
            }

            numberOfAgents++;
        }

        logger.info(String.format("  Number of agents: %d", numberOfAgents));
        logger.info(String.format("  Number of potential AV agents: %d (%.2f%%)", numberOfPotentialAVAgents, 100.0 * numberOfPotentialAVAgents / numberOfAgents));
        logger.info(String.format("  Number of actual AV agents: %d (%.2f%%)", numberOfActualAVAgents, 100.0 * numberOfActualAVAgents / numberOfAgents));
    }

    /**
     * For testing purposes
     */
    static public void main(String[] args) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(args[0]);
        new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes()).readFile(args[1]);

        new TRBAVPlanSelector(new Random(0L)).selectAVPlans(scenario.getPopulation(), 0.8);
        new PopulationWriter(scenario.getPopulation()).write(args[2]);
    }
}
