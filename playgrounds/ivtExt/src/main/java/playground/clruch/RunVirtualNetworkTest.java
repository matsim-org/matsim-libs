package playground.clruch;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import playground.clruch.netdata.VirtualNetwork;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

import java.io.File;
import java.net.MalformedURLException;

/**
 * Created by Claudio on 2/9/2017.
 */
public class RunVirtualNetworkTest {
    public static void main(String[] args) throws MalformedURLException {
        File configFile = new File(args[0]);
        final File dir = configFile.getParentFile();
        File virtualnetworkXML = new File(dir + "/virtualNetwork.xml");
        System.out.println("Looking for virtualNetwork.xml file at " + virtualnetworkXML.getAbsoluteFile());

        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

        if (virtualnetworkXML.isFile()) {
            System.out.println("now creating VirtualNetwork based on XML file.");
            VirtualNetwork virtualNetwork = new VirtualNetwork();
            virtualNetwork.loadFromXML(network, virtualnetworkXML);
        } else {
            System.out.println("no virutalNetwork.xml file");
        }
    }

}
