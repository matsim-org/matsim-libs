package playground.clruch;



/**
 * Created by Claudio on 1/4/2017.
 */


import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import playground.clruch.utils.GZHandler;
import playground.clruch.utils.PopulationTools;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;


// TODO move to "prep" for "preprocessing" package
public class PopulationConverter {
    public static void main(String[] args) throws MalformedURLException {
        final File dir = new File(args[0]);
        File fileImport = new File(dir, "population.xml");
        File fileExportGz = new File(dir, "populationupdated.xml.gz");
        File fileExportGzV6 = new File(dir, "populationupdatedv6.xml.gz");
        File fileExport = new File(dir, "populationupdated.xml");

        System.out.println("Is directory?  " + dir.isDirectory());
        // load the existing population file
        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(fileImport.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);

        System.out.println("Population size:" + scenario.getPopulation().getPersons().values().size());

        // edit the configuration file - change all modes of transport to "av"
        PopulationTools.changeModesOfTransportToAV(scenario.getPopulation());

        System.out.println("Population size after conversion:" + scenario.getPopulation().getPersons().values().size());

        // write the edited population file
        PopulationWriter pw = new PopulationWriter(scenario.getPopulation());
        pw.write(fileExportGz.toString());
        pw.writeV6(fileExportGzV6.toString());

        // extract the created .gz file
        try {
            GZHandler.extract(fileExportGz, fileExport);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

