package playground.clruch.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

import java.io.File;

/**
 * Created by Claudio on 3/29/2017.
 */
public class NetworkLoader {
    public static Network loadNetwork(String[] args) {
        // TODO can this be made more nice?
        // TODO potentially use MatsimNetworkReader to only read network?!
        File configFile = new File(args[0]);

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup);
        //Scenario scenario = ScenarioUtils.loadScenario(config);

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(config.network().getInputFile());

        return network;
    }
}
