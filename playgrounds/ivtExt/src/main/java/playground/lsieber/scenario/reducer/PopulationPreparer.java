package playground.lsieber.scenario.reducer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.io.PopulationWriter;

import ch.ethz.idsc.queuey.util.GZHandler;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.prep.PopulationTools;
import playground.clruch.prep.TheApocalypse;
import playground.lsieber.networkshapecutter.PrepSettings;

public enum PopulationPreparer {
    ;
    public static void run(Network network, Population population, PrepSettings settings) throws MalformedURLException, IOException {
        System.out.println("Original population size: " + population.getPersons().values().size());
        settings.createPopulationCutter().cut(network, population);
        // System.out.println("Population size after radius cut: " + population.getPersons().values().size());
        if (settings.populationeliminateFreight)
            PopulationTools.eliminateFreight(population);
        System.out.println("Population size after removing freight: " + population.getPersons().values().size());
        if (settings.populationeliminateWalking)
            PopulationTools.eliminateWalking(population);
        System.out.println("Population size after removing walking people: " + population.getPersons().values().size());
        if (settings.populationchangeModeToAV) { // FIXME not sure if this is still required, or should always happen !?
            System.out.println("Population size after conversion to mode AV:" + population.getPersons().values().size());
            PopulationTools.changeModesOfTransportToAV(population);
        }
        System.out.println("Population size after conversion to mode AV:" + population.getPersons().values().size());
        TheApocalypse.decimatesThe(population).toNoMoreThan(settings.maxPopulationSize).people();
        System.out.println("Population after decimation:" + population.getPersons().values().size());
        GlobalAssert.that(population.getPersons().size() > 0);

        final File fileExportGz = new File(settings.workingDirectory, settings.POPULATIONUPDATEDNAME + ".xml.gz");
        final File fileExport = new File(settings.workingDirectory, settings.POPULATIONUPDATEDNAME + ".xml");

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
