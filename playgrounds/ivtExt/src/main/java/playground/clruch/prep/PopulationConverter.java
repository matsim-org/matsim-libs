package playground.clruch.prep;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.clruch.utils.GZHandler;

/**
 * Created by Claudio on 1/4/2017.
 */
@Deprecated // integrated into ScenarioPreparer
public class PopulationConverter {
    public static void main(String[] args) throws MalformedURLException {
        final File dir = new File(args[0]);
        if (!dir.isDirectory()) {
            new RuntimeException("not a directory: " + dir).printStackTrace();
            System.exit(-1);
        }
        final File fileImport = new File(dir, "population.xml");
        final File fileExportGz = new File(dir, "populationZurichOnlyTest.xml.gz");
        final File fileExport = new File(dir, "populationZurichOnlyTest.xml");

        // load the existing population file
        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(fileImport.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);

        final Population population = scenario.getPopulation();
        TheApocalypse.decimatesThe(population).toNoMoreThan(235594).people();
        //System.out.println("Population size:" + population.getPersons().values().size());

        // edit the configuration file - change all modes of transport to "av"
        // increasing the second value goes north
        // increasing the first value goes right
        Coord center = new Coord(2682000.0,1247492.0);
        double radius = 12000;
        PopulationTools.elminateOutsideRadius(population, center, radius);
        System.out.println("Population size after radius cut: " + population.getPersons().values().size());
        PopulationTools.eliminateFreight(population);
        System.out.println("Population size after removing freight: " + population.getPersons().values().size());
        PopulationTools.changeModesOfTransportToAV(population);
        System.out.println("Population size after conversion:" + population.getPersons().values().size());

        {
            // write the modified population to file
            PopulationWriter pw = new PopulationWriter(population);
            pw.write(fileExportGz.toString());
        }

        // extract the created .gz file
        try {
            GZHandler.extract(fileExportGz, fileExport);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
