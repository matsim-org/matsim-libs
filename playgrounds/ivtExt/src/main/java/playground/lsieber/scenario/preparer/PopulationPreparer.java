package playground.lsieber.scenario.preparer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.io.PopulationWriter;

import ch.ethz.idsc.queuey.util.GZHandler;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.options.ScenarioOptions;
import playground.clruch.prep.PopulationTools;
import playground.clruch.prep.TheApocalypse;
import playground.lsieber.networkshapecutter.PopulationCutters;

public enum PopulationPreparer {
    ;
    public static void run(Network network, Population population, ScenarioOptions scenOptions) throws MalformedURLException, IOException {
        System.out.println("Original population size: " + population.getPersons().values().size());

        PopulationCutters populationCutters = scenOptions.getPopulationCutter();
        populationCutters.cut(network, population);

        // System.out.println("Population size after radius cut: " + population.getPersons().values().size());
        if (scenOptions.eliminateFreight())
            PopulationTools.eliminateFreight(population);
        System.out.println("Population size after removing freight: " + population.getPersons().values().size());
        if (scenOptions.eliminateWalking())
            PopulationTools.eliminateWalking(population);
        System.out.println("Population size after removing walking people: " + population.getPersons().values().size());
        if (scenOptions.changeModeToAV()) { // FIXME not sure if this is still required, or should always happen !?
            System.out.println("Population size after conversion to mode AV:" + population.getPersons().values().size());
            PopulationTools.changeModesOfTransportToAV(population);
        }
        System.out.println("Population size after conversion to mode AV:" + population.getPersons().values().size());
        TheApocalypse.decimatesThe(population).toNoMoreThan(scenOptions.getMaxPopulationSize()).people();
        System.out.println("Population after decimation:" + population.getPersons().values().size());
        GlobalAssert.that(population.getPersons().size() > 0);

        final File fileExportGz = new File(scenOptions.getPreparedPopulationName() + ".xml.gz");
        final File fileExport = new File(scenOptions.getPreparedPopulationName() + ".xml");

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
