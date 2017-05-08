package playground.clruch.prep;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.clruch.netdata.KMEANSVirtualNetworkCreator;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkIO;

public class VirtualNetworkCreator {

    public static void main(String[] args) throws IOException {
        File configFile = new File(args[0]);
        
        // parameters
        int numberOfNodes = 40;
        boolean completeGraph = true;
                
        // load Network file
        Config config = ConfigUtils.loadConfig(configFile.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();
        
        // create KMEANS-Computed VirtualNetwork
        KMEANSVirtualNetworkCreator kmeansVirtualNetworkCreator = new KMEANSVirtualNetworkCreator();
        VirtualNetwork virtualNetwork = kmeansVirtualNetworkCreator.createVirtualNetwork(population,network,numberOfNodes,completeGraph);
        
        
        // save in virtualNetwork Folder    
        // TODO /virtualNetwork/ is magic constant ... put to central location
        VirtualNetworkIO.toXML(configFile.getParent()+"/virtualNetwork/virtualNetwork.xml",virtualNetwork);        
        

    }

}
