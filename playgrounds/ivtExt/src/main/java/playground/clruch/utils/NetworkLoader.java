package playground.clruch.utils;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.util.GlobalAssert;

/** @author Claudio Ruch */
// TODO Lukas rewrite this in a way such that only the network file is loaded and the other files (population.xml etc. ) are not needed.
public enum NetworkLoader {
    ;
    public static Network loadNetwork(File file) {
        GlobalAssert.that(file.exists());
        Config config = ConfigUtils.loadConfig(file.toString());
        
        // TODO Lukas: see if you can change using the function below such that we can load a network without
        // loading all the rest. 
//         return NetworkUtils.createNetwork(config);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        return scenario.getNetwork();
    }
}
