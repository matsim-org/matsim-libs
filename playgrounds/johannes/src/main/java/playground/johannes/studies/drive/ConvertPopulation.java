package playground.johannes.studies.drive;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Created by johannesillenberger on 12.04.17.
 */
public class ConvertPopulation {

    public static void main(String args[]) {
        String inFile = args[0];
        String outFile = args[1];

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);

        PopulationReader popReader = new PopulationReader(scenario);
        popReader.readFile(inFile);

        PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
        writer.writeV5(outFile);
    }
}
