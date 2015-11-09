package playground.boescpa.baseline.preparation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;

import java.util.List;

/**
 * Repairs incomplete and / or broken activity chains.
 *
 * @author boescpa
 */
public class ActivityChainRepairer {

    public static void main(final String[] args) {
        final String pathToInputPopulation = args[0];
        final String pathToOutputPopulation = args[1];

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimPopulationReader plansReader = new MatsimPopulationReader(scenario);
        plansReader.readFile(pathToInputPopulation);
        Population population = scenario.getPopulation();

        repairActivityChains(population);

        PopulationWriter writer = new PopulationWriter(population);
        writer.write(pathToOutputPopulation);
    }

    private static void repairActivityChains(Population population) {
        Counter counter = new Counter(" person # ");
        List<PlanElement> plan;
        for (Person p : population.getPersons().values()) {
            counter.incCounter();
            if (p.getSelectedPlan() != null) {
                plan = p.getSelectedPlan().getPlanElements();
                ActivityImpl lastAct = (ActivityImpl) plan.get(plan.size()-1);
                if (!lastAct.getType().equals("home") && !lastAct.getType().equals("remote_home")) {
                    // create leg to return home...
                    LegImpl lastLeg = (LegImpl) plan.get(plan.size()-2);
                    LegImpl newLeg = new LegImpl(lastLeg.getMode());
                    // create final home activity (assumption that first activity was home or remote_home)...
                    ActivityImpl firstAct = (ActivityImpl) plan.get(0);
                    ActivityImpl newAct = new ActivityImpl(firstAct.getType(), firstAct.getCoord());
                    newAct.setFacilityId(firstAct.getFacilityId());
                    newAct.setStartTime(24*3600+1); // start immediately after midnight...
                    // complete act chain...
                    p.getSelectedPlan().addLeg(newLeg);
                    p.getSelectedPlan().addActivity(newAct);
                }
            }
        }
    }

}
