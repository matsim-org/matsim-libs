package playground.clruch.trb18;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import playground.sebhoerl.avtaxi.framework.AVModule;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class PreparePopulationForTRB {
    static public void main(String[] args) {
        double avShare = Double.parseDouble(args[0]);
        String populationInputPath = args[1];
        String populationOutputPath = args[2];

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(populationInputPath);
        new PreparePopulationForTRB().run(scenario, avShare);
        new PopulationWriter(scenario.getPopulation()).write(populationOutputPath);
    }

    final private static Logger logger = Logger.getLogger(PreparePopulationForTRB.class);

    final private List<String> modePrecedence = Arrays.asList(TransportMode.car, TransportMode.pt, TransportMode.bike, TransportMode.walk);
    final private Set<String> allowedMainModes = new HashSet<>(Arrays.asList(TransportMode.car, TransportMode.pt));

    private String findMainMode(Plan plan) {
        String mainMode = null;

        for (PlanElement element : plan.getPlanElements()) {
            if (element instanceof Leg) {
                Leg leg = (Leg) element;

                if (mainMode == null || (modePrecedence.indexOf(leg.getMode()) < modePrecedence.indexOf(mainMode))) {
                    mainMode = leg.getMode();
                }
            }
        }

        return mainMode;
    }

    /**
     * - Removes all agents that do not have car or pt as a main mode
     * - Set all car or pt legs of avShare % of remaining agents to AV
     *
     * @param scenario
     * @param avShare
     */
    private void run(Scenario scenario, double avShare) {
        Iterator<? extends Person> personIterator = scenario.getPopulation().getPersons().values().iterator();
        Random random = new Random(0);

        long numberOfAgents = 0;
        long numberOfRemovedAgents = 0;
        long numberOfAdjustedAgents = 0;

        long numberOfRelevantLegs = 0;
        long numberOfAdjustedLegs = 0;

        Counter counter = new Counter("", " agents processed");

        while (personIterator.hasNext()) {
            Person person = personIterator.next();

            String mainMode = findMainMode(person.getSelectedPlan());

            if (allowedMainModes.contains(mainMode)) {
                if (random.nextDouble() <= avShare) {
                    for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
                        if (element instanceof Leg) {
                            Leg leg = (Leg) element;

                            if (allowedMainModes.contains(leg.getMode())) {
                                leg.setMode(AVModule.AV_MODE);
                                numberOfAdjustedLegs++;
                            }

                            numberOfRelevantLegs++;
                        }
                    }

                    numberOfAdjustedAgents++;
                }
            } else {
                numberOfRemovedAgents++;
                personIterator.remove();
            }

            numberOfAgents++;
            counter.incCounter();
        }

        long numberOfRemainingAgents = numberOfAgents - numberOfRemovedAgents;

        logger.info(String.format("Number of agents: %d", numberOfAgents));
        logger.info(String.format("Number of removed agents: %d (%.2f%%)", numberOfRemovedAgents, 100.0 * numberOfRemovedAgents / numberOfAgents));
        logger.info(String.format("Number of adjusted agents: %d (%.2f%% of total, %.2f%% of remaining)", numberOfAdjustedAgents, 100.0 * numberOfAdjustedAgents / numberOfAgents, 100.0 * numberOfAdjustedAgents / numberOfRemainingAgents));
        logger.info(String.format("Number of relevant legs: %d", numberOfRelevantLegs));
        logger.info(String.format("Number of adjusted legs: %d (%.2f%%)", numberOfAdjustedLegs, 100.0 * numberOfAdjustedLegs / numberOfRelevantLegs));
    }
}
