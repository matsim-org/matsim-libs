package playground.clruch.trb18.scenario.stages;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class TRBPopulationPreparer {
    final private static Logger logger = Logger.getLogger(TRBPopulationPreparer.class);
    final private TRBPlanModifier planModifier;

    public TRBPopulationPreparer(TRBPlanModifier planModifier) {
        this.planModifier = planModifier;
    }

    /**
     * Prepares the population
     * - Adds a new AV plan to each agent whoses plan can contain AV legs
     * - Adds every non-eligible agent to the subpopulation "noav"
     * - Freight and cross-border agents are NOT eligible!
     */
    public void filter(Population population, ObjectAttributes originalPopulationAttributes) {
        logger.info("Searching for eligible agents and adding AV plans ...");

        long numberOfPlans = 0;
        long numberOfAvPlans = 0;

        for (Person person : population.getPersons().values()) {
            Plan defaultPlan = person.getSelectedPlan();

            String subPopulation = (String) originalPopulationAttributes.getAttribute(person.getId().toString(), "subpopulation");
            boolean isFreight = subPopulation != null && subPopulation.equals("freight");
            boolean isCrossBorder = subPopulation != null && subPopulation.equals("cb");

            if (planModifier.canUseAVs(defaultPlan) && !isFreight && !isCrossBorder) {
                person.addPlan(planModifier.createModifiedPlan(defaultPlan));
                numberOfAvPlans++;
            } else {
                population.getPersonAttributes().putAttribute(person.getId().toString(), "subpopulation", "noav");
            }

            numberOfPlans++;
        }

        logger.info(String.format("  Number of agents: %d", numberOfPlans));
        logger.info(String.format("  Number of agents with AV plans: %d (%.2f%%)", numberOfAvPlans, 100.0 * numberOfAvPlans / numberOfPlans));
    }

    /**
     * For testing purposes
     */
    static public void main(String[] args) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        new PopulationReader(scenario).readFile(args[0]);
        new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes()).readFile(args[1]);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(args[2]);

        TRBPlanModifier planModifier = new TRBPlanModifier(scenario.getNetwork(), true);
        new TRBPopulationPreparer(planModifier).filter(scenario.getPopulation(), scenario.getPopulation().getPersonAttributes());

        new PopulationWriter(scenario.getPopulation()).write(args[3]);
        new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes()).writeFile(args[4]);
    }
}
