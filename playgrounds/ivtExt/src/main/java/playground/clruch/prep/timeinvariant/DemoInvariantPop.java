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
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.GZHandler;
import playground.clruch.ScenarioOptions;
import playground.clruch.prep.PopulationTools;
import playground.clruch.prep.TheApocalypse;
import playground.clruch.utils.PropertiesExt;

/** @author Claudio Ruch */
public class DemoInvariantPop {

    /** Takes the full config population in the working directory and creates a population only in the interval
     * or the interval population resampled during the entire day.
     * 
     * @param args
     * @throws IOException */
    public static void main(String[] args) throws IOException {
        final Interval interval = new Interval(new double[] { 27900.0, 27900.0 + 3600.0 });
        final String POPULATIONUPDATEDNAME = "populationMorning";

        // demo
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        File configFile = new File(workingDirectory,"astra_config_fullBeforeMorning.xml");
        Config config = ConfigUtils.loadConfig(configFile.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();

        System.out.println(population.getPersons().size());
//        TheApocalypse.decimatesThe(population).toNoMoreThan(50000);
        PopulationTools.changeModesOfTransportToAV(population);
        Population populationInvariant = TimeInvariantPopulation.from(interval, population);
        System.out.println(population.getPersons().size());

        // write the modified population to file
        final File fileExportGz = new File(workingDirectory, POPULATIONUPDATEDNAME + ".xml.gz");
        final File fileExport = new File(workingDirectory, POPULATIONUPDATEDNAME + ".xml");
        PopulationWriter pw = new PopulationWriter(populationInvariant);
        pw.write(fileExportGz.toString());
        GZHandler.extract(fileExportGz, fileExport);

    }

}
