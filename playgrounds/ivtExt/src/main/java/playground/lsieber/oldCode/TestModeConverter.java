package playground.lsieber.oldCode;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import playground.clruch.ScenarioOptions;
import playground.clruch.utils.PropertiesExt;

public class TestModeConverter {

    /** @author Lukas Sieber
     * Hint: if Out of Memory error appears give the program more Memory with the -Xmx8192m argument in the Run Configurations VM paart
     */
    public static void main(String[] args) throws IOException {
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        PropertiesExt simOptions = PropertiesExt.wrap(ScenarioOptions.load(workingDirectory));
        File file = new File(workingDirectory, simOptions.getString("simuConfig"));
        Config config = ConfigUtils.loadConfig(file.toString());
        Scenario originalScenario = ScenarioUtils.loadScenario(config);
        Population newPopulation = new ModeConverter(originalScenario.getPopulation()).run();
        
        new PopulationWriter(newPopulation).write("ConvertedPopulation.xml");

        System.out.println("got it");
    }
    

    

}
