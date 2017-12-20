// code by andya
package playground.clruch.io.fleet;

import java.io.File;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
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

/** @author Andreas Aumiller */
enum PopulationCreator {
    ;

    public static void main(String[] args) throws MalformedURLException, Exception {
        // Loading simulationObjects
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        ScenarioOptions simOptions = ScenarioOptions.load(workingDirectory);
        Config configFile = ConfigUtils.loadConfig(simOptions.getSimulationConfigName());
        System.out.println(simOptions.getSimulationConfigName());
        File outputSubDirectory = new File(configFile.controler().getOutputDirectory());
        File outputDirectory = outputSubDirectory.getParentFile();

        // Initialize ConfigGroups and Files
        System.out.println("INFO loading simulation configuration");
        Scenario scenario = ScenarioUtils.loadScenario(configFile);
        Network network = scenario.getNetwork();
        ReferenceFrame referenceFrame = ReferenceFrame.SWITZERLAND;
        MatsimStaticDatabase.initializeSingletonInstance(network, referenceFrame);
        createAdamAndEva(workingDirectory, outputDirectory, network, MatsimStaticDatabase.INSTANCE);
    }

    public static void createAdamAndEva(File workingDirectory, File outputDirectory, Network network, MatsimStaticDatabase db) throws Exception {
        System.out.println("\nINFO Running PopulatioCreater with following data");
        System.out.println("INFO getting all output folders from: " + outputDirectory.getAbsolutePath());
        File[] outputFolders = MultiFileTools.getAllDirectoriesSorted(outputDirectory);
        String[] outputFolderNames = new String[outputFolders.length];
        for (int i = 0; i < outputFolders.length; ++i) {
            outputFolderNames[i] = outputFolders[i].getName();
        }
        StorageUtils storageUtils = new StorageUtils(new File(outputDirectory, outputFolderNames[0])); // TODO process all data from outputFolderNames
        storageUtils.printStorageProperties();

        PlansConfigGroup plansConfigGroup = new PlansConfigGroup();

        // Create new population
        System.out.println("INFO creating new population file");
        Population population = PopulationUtils.createPopulation(plansConfigGroup, network);

        population = PopulationDump.of(population, network, db, storageUtils);

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
        System.out.println("INFO successfully created population");
    }
}
