package playground.clruch;

import java.io.File;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.matsim.av.framework.AVConfigGroup;
import playground.clruch.options.ScenarioOptions;
import playground.lsieber.scenario.preparer.NetworkPreparer;
import playground.lsieber.scenario.preparer.PopulationPreparer;
import playground.lsieber.scenario.preparer.VirtualNetworkPreparer;

/** Class to prepare a given scenario for MATSim, includes preparation of network, population, creation of virtualNetwork
 * and travelData objects.
 * 
 * @author clruch */
public class ScenarioPreparer {

    public static void main(String[] args) throws MalformedURLException, Exception {
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        run(workingDirectory);
    }

    public static void run(File workingDirectory) throws MalformedURLException, Exception {

        // run preparer in simulation working directory
        ScenarioOptions scenarioOptions = ScenarioOptions.load(workingDirectory);

        // load Settings from IDSC Options
        Config config = ConfigUtils.loadConfig(scenarioOptions.getPreparerConfigName());
        Scenario scenario = ScenarioUtils.loadScenario(config);     

        // 1) cut network (and reduce population to new network)
        Network network = scenario.getNetwork();
        network = NetworkPreparer.run(network, scenarioOptions);

        // 2) adapt the population to new network
        Population population = scenario.getPopulation();
        System.out.println("population size: " + population.getPersons().size());
        PopulationPreparer.run(network, population, scenarioOptions);

        // 3) create virtual Network
        VirtualNetworkPreparer.run(network, population, scenarioOptions);

        // 4) save a simulation config file
        createSimulationConfigFile(config, scenarioOptions);

    }

    public static void createSimulationConfigFile(Config fullConfig, ScenarioOptions scenOptions) {

        // change population and network such that converted is loaded
        fullConfig.network().setInputFile(scenOptions.getPreparedNetworkName() + ".xml.gz");
        fullConfig.plans().setInputFile(scenOptions.getPreparedPopulationName() + ".xml.gz");
        AVConfigGroup avConfigGroup = new AVConfigGroup();
        fullConfig.addModule(avConfigGroup);
        
        // save under correct name
        new ConfigWriter(fullConfig).writeFileV2(scenOptions.getSimulationConfigName());
    }
}