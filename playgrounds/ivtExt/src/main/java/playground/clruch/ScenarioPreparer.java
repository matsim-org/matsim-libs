package playground.clruch;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import playground.lsieber.networkshapecutter.PrepSettings;
import playground.lsieber.networkshapecutter.PrepSettings.SettingsType;
import playground.lsieber.scenario.preparer.NetworkPreparer;
import playground.lsieber.scenario.preparer.PopulationPreparer;
import playground.lsieber.scenario.preparer.VirtualNetworkPreparer;

/** Class to prepare a given scenario for MATSim, includes preparation of netowrk, population, creation of virtualNetwork
 * and travelData objects.
 * 
 * @author clruch */
public class ScenarioPreparer {

    public static void main(String[] args) throws MalformedURLException, Exception {
        run(args);
    }

    public static void run(String[] args) throws MalformedURLException, Exception {
        
        // run preparer in simulation working directory
        File workingDirectory = MultiFileTools.getWorkingDirectory();

        // load Settings from IDSC Options
        PrepSettings settings = new PrepSettings(workingDirectory);

        Scenario scenario = ScenarioUtils.loadScenario(settings.config);
        // create Reduced Scenario Folder if nesscesary
        settings.preparedScenarioDirectory.mkdir();

        // 1) cut network (and reduce population to new network)
        Network network = scenario.getNetwork();
        network = NetworkPreparer.run(network, settings);

        // 2) adapt the population to new network
        Population population = scenario.getPopulation();
        PopulationPreparer.run(network, population, settings);

        
        // FIXME LUKAS Add Facilities
        
        
        // 3) create virtual Network
        VirtualNetworkPreparer.run(network, population, settings);

        // 4) coppy and prepare other files
        // TODO NO Hardcoding!!!! 
        //COPY and Modify CONFIG Files (IDSCOptions, AV and CONFIG)
        new ConfigWriter(settings.config).writeFileV2(new File(settings.preparedScenarioDirectory, settings.preparedConfigName).toString());
        String IDSCOptions = ScenarioOptions.getOptionsFileName();
        Path src = new File(workingDirectory, IDSCOptions).toPath();
        Path dest = new File(settings.preparedScenarioDirectory, IDSCOptions).toPath();
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        Path avFile = new File(workingDirectory, "av.xml").toPath();
        if (Files.exists(avFile)) {
            Files.copy(avFile, new File(settings.preparedScenarioDirectory, "av.xml").toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        //TODO Check if other files are needed to copy into prepared Scenario Folder (e.g. Schedule, ...)

        // 5) TODO CREATE Report of the Preparing in a text (or other format) file which summarizes the preparation Steps

        System.out.println("-----> END OF SCENARIO PREPARER <-----");
    }
}
