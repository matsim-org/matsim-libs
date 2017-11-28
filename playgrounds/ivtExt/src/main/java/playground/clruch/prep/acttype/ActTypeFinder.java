/**
 * 
 */
package playground.clruch.prep.acttype;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.ScenarioOptions;
import playground.clruch.utils.ScenarioOptionsExt;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

/**
 * @author Claudio Ruch
 *
 */
public class ActTypeFinder {


    /** execute in working directory of simulation, finds all activities of puplation and creates source
     * code to include it
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {

        File workingDirectory = MultiFileTools.getWorkingDirectory();
        ScenarioOptionsExt simOptions = ScenarioOptionsExt.wrap(ScenarioOptions.load(workingDirectory));
        File configFile = new File(workingDirectory, simOptions.getString("fullConfig"));
        System.out.println("loading config file " + configFile.getAbsoluteFile());
        GlobalAssert.that(configFile.exists()); // Test wheather the config file directory exists
        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();

        HashSet<String> activities = new HashSet<>();
        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement pE : plan.getPlanElements()) {
                    if (pE instanceof Activity) {
                        Activity act = (Activity) pE;
                        activities.add(act.getType());
                        // System.out.println(act.getType());
                    }
                }
            }
        }
        
        activities.stream().forEach(//                
                a-> System.out.println("config.planCalcScore().addActivityParams(new ActivityParams(\""   +a+"\"));"))  ;

    }

}
