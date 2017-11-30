/**
 * 
 */
package playground.clruch.io.fleet;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.GZHandler;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.StorageUtils;
import playground.clruch.options.ScenarioOptions;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

/** @author Andreas Aumiller */
public class PopulationCreator {

    public static int STEPSIZE = 10; // TODO this should be derived from storage files
    private static StorageUtils storageUtils;
    private static File[] outputFolders;
    private static String[] outputFolderNames;

    public static void main(String[] args) throws Exception {
        // Loading simulationObjects
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        ScenarioOptions simOptions = ScenarioOptions.load(workingDirectory);
        Config configFile = ConfigUtils.loadConfig(simOptions.getSimulationConfigName());
        System.out.println(simOptions.getSimulationConfigName());
        File outputDirectory = new File(configFile.controler().getOutputDirectory());
        // File outputDirectory = new File(workingDirectory, simOptions.getString("visualizationFolder"));
        System.out.println("INFO getting all output folders from: " + outputDirectory.getAbsolutePath());
        outputFolders = MultiFileTools.getAllDirectoriesSorted(outputDirectory);
        outputFolderNames = new String[outputFolders.length];
        for (int i = 0; i < outputFolders.length; ++i) {
            outputFolderNames[i] = outputFolders[i].getName();
        }
        storageUtils = new StorageUtils(new File(outputDirectory, outputFolderNames[0]));

        // Initialize ConfigGroups and Files
        System.out.println("INFO loading simulation configuration");
        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        // File configFile = new File(workingDirectory, simOptions.getString("simuConfig"));
        Config config = ConfigUtils.loadConfig(simOptions.getSimulationConfigName(), new AVConfigGroup(), dvrpConfigGroup);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        PlansConfigGroup plansConfigGroup = new PlansConfigGroup();

        // Create new population
        System.out.println("INFO creating new population file");
        Population population = PopulationUtils.createPopulation(plansConfigGroup, network);
        ReferenceFrame referenceFrame = ReferenceFrame.SWITZERLAND;
        MatsimStaticDatabase.initializeSingletonInstance(network, referenceFrame);

        population = PopulationDump.of(population, network, MatsimStaticDatabase.INSTANCE, storageUtils);
        // populate(population);

        // Write new population to file
        final File populationFile = new File(workingDirectory, "TestPopulation.xml");
        final File populationGzFile = new File(workingDirectory, "TestPopulation.xml.gz");

        // write the modified population to file
        System.out.println("INFO writing new population to:");
        System.out.println(populationFile.getPath());
        System.out.println(populationGzFile.getPath());
        PopulationWriter pw = new PopulationWriter(population);
        pw.write(populationGzFile.toString());

        // extract the created .gz file
        GZHandler.extract(populationGzFile, populationFile);
    }
}
