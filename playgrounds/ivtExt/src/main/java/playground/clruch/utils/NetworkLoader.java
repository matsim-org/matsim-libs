package playground.clruch.utils;

import java.io.File;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.scenario.ScenarioUtils;

/** @author Claudio Ruch */
public enum NetworkLoader {
    ;
    public static Network loadNetwork(File file) {
        Config config = ConfigUtils.loadConfig(file.toString());
        Set<Entry<String, ConfigGroup>> toDelete = new HashSet<>();
        for (Entry<String, ConfigGroup> entry : config.getModules().entrySet()) {
            if (!entry.getKey().equals(NetworkConfigGroup.GROUP_NAME)) {
                toDelete.add(entry);
            }
        }

        toDelete.stream().forEach(e -> config.removeModule(e.getKey()));

        Scenario scenario = ScenarioUtils.loadScenario(config);
        return scenario.getNetwork();
    }
} 