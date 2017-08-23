package playground.clruch.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

import java.io.File;

/**
 * 
 * @author Claudio Ruch
 *
 */
public enum NetworkLoader {
    ;
    public static Network loadNetwork(File file) {
        Config config = ConfigUtils.loadConfig(file.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        return scenario.getNetwork();
    }
}

