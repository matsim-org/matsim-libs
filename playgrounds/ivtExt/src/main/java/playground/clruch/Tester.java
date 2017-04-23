package playground.clruch;

import java.io.File;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.clruch.netdata.KMEANSVirtualNetworkCreator;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkIO;

/**
 * main entry point
 * 
 * only one ScenarioServer can run at one time, since a fixed network port is reserved to serve the
 * simulation status
 * 
 * if you wish to run multiple simulations at the same time use for instance {@link RunAVScenario}
 */
public class Tester {

    public static void main(String[] args) throws MalformedURLException, Exception {

        File configFile = new File("/home/clruch/Simulations/2017_04_06_Sioux_LPFeedback/av_config.xml");
        
        // load Network file
        Config config = ConfigUtils.loadConfig(configFile.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();
        
        KMEANSVirtualNetworkCreator kmeansVirtualNetworkCreator = new KMEANSVirtualNetworkCreator();
        
        VirtualNetwork virtualNetwork = kmeansVirtualNetworkCreator.createVirtualNetwork(population,network,4);
        VirtualNetworkIO.toXML("/home/clruch/Simulations/2017_04_21_Zurich_LP/virtualNetwork/virtualNetwork.xml",virtualNetwork);
        
        
        
        //VirtualNetwork virtualNetwork = VirtualNetworkIO.fromXML(network, new File("/home/clruch/Simulations/2017_04_06_Sioux_LPFeedback/vN_debug_v0/virtualNetwork.xml"));
      

    }
}


