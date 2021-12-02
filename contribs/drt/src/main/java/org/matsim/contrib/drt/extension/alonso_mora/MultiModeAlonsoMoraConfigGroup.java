package org.matsim.contrib.drt.extension.alonso_mora;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

public class MultiModeAlonsoMoraConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "mode";

	public MultiModeAlonsoMoraConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		if (type.equals(AlonsoMoraConfigGroup.GROUP_NAME)) {
			return new AlonsoMoraConfigGroup();
		}

		throw new IllegalStateException("Cannot create parameter set of type " + type);
	}

	public Map<String, AlonsoMoraConfigGroup> getModes() {
		Map<String, AlonsoMoraConfigGroup> modes = new HashMap<>();

		for (ConfigGroup modeConfig : getParameterSets(AlonsoMoraConfigGroup.GROUP_NAME)) {
			AlonsoMoraConfigGroup amConfig = (AlonsoMoraConfigGroup) modeConfig;
			modes.put(amConfig.getMode(), amConfig);
		}

		return modes;
	}

	static public MultiModeAlonsoMoraConfigGroup get(Config config) {
		return (MultiModeAlonsoMoraConfigGroup) config.getModules().get(GROUP_NAME);
	}
}
