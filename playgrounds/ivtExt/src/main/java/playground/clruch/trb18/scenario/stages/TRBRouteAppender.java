package playground.clruch.trb18.scenario.stages;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

public class TRBRouteAppender {
    final private static Logger logger = Logger.getLogger(TRBRouteAppender.class);

    /**
     * Copies routes from the original (source) to the new (target) population
     * - Conversion is done by Agent ID and index within the activity chain
     * - The modified plan in the target population must be selected! (I.e. the non-av plan)
     */
    public void run(Population targetPopulation, Population sourcePopulation) {
        logger.info("Converting routes from the original population to the new one ...");

        long numberOfRemovedElements = 0;
        long numberOfImputedElements = 0;

        for (Person targetPerson : targetPopulation.getPersons().values()) {
            Person sourcePerson = sourcePopulation.getPersons().get(targetPerson.getId());
            if (sourcePerson == null) throw new RuntimeException();

            for (Plan targetPlan : targetPerson.getPlans()) {
                List<PlanElement> targetPlanElements = targetPlan.getPlanElements();
                List<PlanElement> sourcePlanElements = PlanUtils.createCopy(sourcePerson.getSelectedPlan()).getPlanElements();

                List<TripStructureUtils.Trip> targetTrips = TripStructureUtils.getTrips(targetPlanElements, new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
                List<TripStructureUtils.Trip> sourceTrips = TripStructureUtils.getTrips(sourcePlanElements, new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
                if (targetTrips.size() != sourceTrips.size()) throw new RuntimeException();

                for (int i = 0; i < targetTrips.size(); i++) {
                    int targetIndex = targetPlanElements.indexOf(targetTrips.get(i).getLegsOnly().get(0));

                    if (!targetTrips.get(i).getLegsOnly().get(0).getMode().equals("av")) {
                        targetPlanElements.remove(targetIndex);
                        targetPlanElements.addAll(targetIndex, sourceTrips.get(i).getTripElements());

                        numberOfImputedElements += sourceTrips.get(i).getTripElements().size();
                        numberOfRemovedElements++;
                    }
                }
            }
        }

        logger.info("  Number of removed elements: " + numberOfRemovedElements);
        logger.info("  Number of imputed elements: " + numberOfImputedElements);
    }

    static public void main(String[] args) {
        Scenario sourceScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Scenario targetScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        new PopulationReader(sourceScenario).readFile(args[0]);
        new PopulationReader(targetScenario).readFile(args[1]);

        new TRBRouteAppender().run(targetScenario.getPopulation(), sourceScenario.getPopulation());
        new PopulationWriter(targetScenario.getPopulation()).write(args[2]);
    }
}
