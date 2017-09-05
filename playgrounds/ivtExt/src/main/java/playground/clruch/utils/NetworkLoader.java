package playground.clruch.utils;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

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

