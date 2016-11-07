package playground.singapore.calibration.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.singapore.typesPopulation.population.PopulationReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Created by fouriep on 11/7/16.
 */
public class WorkTypeReassignment {
    public static void main(String[] args) throws IOException {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(args[0]);
        BufferedReader reader = IOUtils.getBufferedReader(args[1]);
        // skip the first line
        String text = reader.readLine();
        text = reader.readLine();
        while (text != null) {
            String[] split = text.split(",");
            String idString = split[0];
            String actType = split[1];
            Person person = scenario.getPopulation().getPersons().get(Id.createPersonId(idString));
            for (Plan plan : person.getPlans()) {

                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity activity = (Activity) planElement;
                        if (activity.getType().startsWith("w_")) {
                            activity.setType(actType);
                        }
                    }
                }
            }


            text = reader.readLine();
        }
        reader.close();

        //write out the new population
        new PopulationWriter(scenario.getPopulation()).write(args[2]);

    }
}
