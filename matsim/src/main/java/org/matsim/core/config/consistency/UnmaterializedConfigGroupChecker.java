package org.matsim.core.config.consistency;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

public class UnmaterializedConfigGroupChecker implements ConfigConsistencyChecker {
	@Override
	public void checkConsistency(Config config) {
		for (ConfigGroup configGroup : config.getModules().values()) {
			if (configGroup.getClass().equals(ConfigGroup.class)) {
				throw new RuntimeException("Unmaterialized config group: "+configGroup.getName());
			}
		}
	}
}
