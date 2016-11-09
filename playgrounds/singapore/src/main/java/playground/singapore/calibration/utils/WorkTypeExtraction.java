package playground.singapore.calibration.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.singapore.typesPopulation.population.PopulationReader;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Created by fouriep on 11/7/16.
 */
public class WorkTypeExtraction {
    public static void main(String[] args) throws IOException {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(args[0]);
        BufferedWriter writer = IOUtils.getBufferedWriter(args[1]);
        writer.write(String.format("%s\t%s\t%s\n", "id", "act",   "end"));
        for (Person person : scenario.getPopulation().getPersons().values()) {
            Plan selectedPlan = person.getSelectedPlan();
            double endTime = Double.NEGATIVE_INFINITY;
            String actType = "";
            boolean hasWork = false;
            for (PlanElement planElement : selectedPlan.getPlanElements()) {
                if (planElement instanceof Activity) {
                    Activity activity = (Activity) planElement;
                    if (activity.getType().startsWith("w_")) {
                        hasWork = true;
                        actType = activity.getType();
                        if (activity.getEndTime() > endTime)
                            endTime = activity.getEndTime();
                    }
                }
            }
            if (hasWork)
                writer.write(String.format("%s\t%s\t%.1f\n", person.getId().toString(), actType, endTime));
        }
        writer.close();
    }
}
