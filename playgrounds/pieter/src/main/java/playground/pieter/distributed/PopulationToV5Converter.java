package playground.pieter.distributed;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Created by fouriep on 10/7/14.
 */
public class PopulationToV5Converter {
    public static void main(String[] args) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        PopulationImpl population = (PopulationImpl) scenario.getPopulation();
        population.setIsStreaming(true);
        MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
        PopulationWriter writer = new PopulationWriter(population);
        writer.startStreaming(args[1]);
        population.addAlgorithm(writer);
        reader.readFile(args[0]);
        writer.closeStreaming();
    }
}
