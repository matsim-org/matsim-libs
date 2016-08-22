package contrib.baseline.preparation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;

import java.util.List;
import java.util.Random;

import static contrib.baseline.preparation.IVTConfigCreator.HOME;
import static contrib.baseline.preparation.IVTConfigCreator.REMOTE_HOME;

/**
 * Repairs incomplete and / or broken activity chains.
 *
 * @author boescpa
 */
public class ActivityChainRepairer {

    private final static Random random = new Random(37835409);

    public static void main(final String[] args) {
        final String pathToInputPopulation = args[0];
        final String pathToOutputPopulation = args[1];

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        PopulationReader plansReader = new PopulationReader(scenario);
        plansReader.readFile(pathToInputPopulation);
        Population population = scenario.getPopulation();

        repairActivityChains(population);

        PopulationWriter writer = new PopulationWriter(population);
        writer.write(pathToOutputPopulation);
    }

    private static void repairActivityChains(Population population) {
        Counter counter = new Counter(" person # ");
        double minDurationLastAct, durationLastAct;
        List<PlanElement> plan;
        for (Person p : population.getPersons().values()) {
            counter.incCounter();
            if (p.getSelectedPlan() != null) {
                plan = p.getSelectedPlan().getPlanElements();
                Activity lastAct = (Activity) plan.get(plan.size()-1);
                if (!lastAct.getType().equals(HOME) && !lastAct.getType().equals(REMOTE_HOME)) {
                    // add end-time to lastAct given typical duration
                    minDurationLastAct = PrefsCreator.actCharacteristics.valueOf(lastAct.getType().toUpperCase()).getMinDur();
                    durationLastAct = Math.round(minDurationLastAct + (random.nextInt(30) / 10 * minDurationLastAct));
                    if (lastAct.getStartTime() + durationLastAct > 24*3600) {
                        durationLastAct = 24*3600 - lastAct.getStartTime() - 1;
                        // assumption that (in their initial plan) everyone should be at home at latest at midnight...
                    }
                    lastAct.setEndTime(lastAct.getStartTime() + durationLastAct);
                    // create leg to return home...
                    Leg lastLeg = (Leg) plan.get(plan.size()-2);
                    Leg newLeg = PopulationUtils.createLeg(lastLeg.getMode());
                    // create final home activity (assumption that first activity was home or remote_home)...
                    Activity firstAct = (Activity) plan.get(0);
                    Activity newAct = PopulationUtils.createActivityFromCoord(firstAct.getType(), firstAct.getCoord());
                    newAct.setFacilityId(firstAct.getFacilityId());
                    newAct.setStartTime(lastAct.getEndTime() + 1);
                    // complete act chain...
                    p.getSelectedPlan().addLeg(newLeg);
                    p.getSelectedPlan().addActivity(newAct);
                }
            }
        }
    }

}
