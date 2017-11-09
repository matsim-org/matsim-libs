/**
 * 
 */
package playground.clruch.io.fleet;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.GZHandler;
import playground.clruch.ScenarioOptions;
import playground.clruch.utils.PropertiesExt;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

/** @author Claudio Ruch */
public class PopulationCreator {
    public static void main(String[] args) throws IOException {
        System.out.println("creating a population...");
        File wd = MultiFileTools.getWorkingDirectory();
        PropertiesExt simOptions = PropertiesExt.wrap(ScenarioOptions.load(wd));
        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();        
        File configFile = new File(wd, simOptions.getString("simuConfig"));
        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        
        PlansConfigGroup plansConfigGroup = new PlansConfigGroup();
        Population population = PopulationUtils.createPopulation(plansConfigGroup, network);
        fill(population); 
        
        

        final File fileExport = new File(wd, "AndysPopulation.xml");
        final File fileExportGz = new File(wd, "AndysPopulation.xml.gz");

        {
            // write the modified population to file
            PopulationWriter pw = new PopulationWriter(population);
            pw.write(fileExportGz.toString());
        }

        // extract the created .gz file

        GZHandler.extract(fileExportGz, fileExport);

    }
    
    private static void fill(Population population){        
        PopulationFactory pc =  population.getFactory();
//        IdC
//        pc.createPerson(//TODO id)
        
    }

}
