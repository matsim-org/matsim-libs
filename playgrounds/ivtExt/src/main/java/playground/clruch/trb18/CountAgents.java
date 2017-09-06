package playground.clruch.trb18;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import contrib.baseline.preparation.ZHCutter;

public class CountAgents {
    static public void main(String[] args) {
        ZHCutter.ZHCutterConfigGroup zhConfig = new ZHCutter.ZHCutterConfigGroup("");
        Coord scenarioCenterCoord = new Coord(zhConfig.getxCoordCenter(), zhConfig.getyCoordCenter());

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(args[0]);

        for (Person person : scenario.getPopulation().getPersons().values()) {
            if (scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), "subpopulation") == null) {
                for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
                    if (element instanceof Activity) {

                    }
                }
            }
        }
    }
}
