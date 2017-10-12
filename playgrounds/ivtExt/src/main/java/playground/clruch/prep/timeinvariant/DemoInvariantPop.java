/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import playground.clruch.ScenarioOptions;
import playground.clruch.utils.PropertiesExt;

/** @author Claudio Ruch */
public class DemoInvariantPop {

    public static void main(String[] args) throws IOException {
        final int sTime = 27900;
        final int duration = 60 * 60;

        // demo
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        PropertiesExt simOptions = PropertiesExt.wrap(ScenarioOptions.load(workingDirectory));
        File configFile = new File(workingDirectory, simOptions.getString("simuConfig"));
        Config config = ConfigUtils.loadConfig(configFile.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();

        Population populationInvariant = TimeInvariantPopulation.at(sTime, duration, population);

    }

}
