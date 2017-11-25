package playground.clruch;

import java.net.MalformedURLException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.lsieber.networkshapecutter.PrepSettings;
import playground.lsieber.scenario.preparer.NetworkPreparer;
import playground.lsieber.scenario.preparer.PopulationPreparer;
import playground.lsieber.scenario.preparer.VirtualNetworkPreparer;

/** Class to prepare a given scenario for MATSim, includes preparation of netowrk, population, creation of virtualNetwork
 * and travelData objects.
 * 
 * @author clruch */
// TODO clean up thoroughly this file
public class ScenarioPreparer {

    public static void main(String[] args) throws MalformedURLException, Exception {
        run(args);
    }

    public static void run(String[] args) throws MalformedURLException, Exception {

        // load Settings from IDSC Options
        PrepSettings settings = new PrepSettings();

        // 0) load files
        Config config = ConfigUtils.loadConfig(settings.configFile.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        // create Reduced Scenario Folder if nesscesary
        settings.preparedScenarioDirectory.mkdir();

        // 1) cut network (and reduce population to new network)
        Network network = scenario.getNetwork();
        NetworkPreparer.run(network, settings);

        // 2) adapt the population to new network
        Population population = scenario.getPopulation();
        PopulationPreparer.run(network, population, settings);

        // 3) create virtual Network
        VirtualNetworkPreparer.run(network, population, settings);
        
        
        // TODO COPY and Modify CONFIG Files (IDSCOptions, AV and CONFIG)
        
        // TODO CREATE Report of the Preparing in a text (or other format) file which summarizes the preparation Steps
        
        

    }
}
