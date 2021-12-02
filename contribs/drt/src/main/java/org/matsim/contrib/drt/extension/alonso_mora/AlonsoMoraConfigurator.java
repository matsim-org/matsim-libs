package org.matsim.contrib.drt.extension.alonso_mora;

import java.util.Set;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

public class AlonsoMoraConfigurator {
	static public void configure(Controler controller, Set<String> modes) {
		for (String mode : modes) {
			configure(controller, mode);
		}
	}

	static public void configure(Controler controller, String mode) {
		DrtConfigGroup drtConfig = getDrtConfig(controller.getConfig(), mode);
		AlonsoMoraConfigGroup amConfig = getAlonsoMoraConfig(controller.getConfig(), mode);

		if (drtConfig == null) {
			throw new IllegalStateException("Cannot find mode in DRT config: " + mode);
		}

		if (amConfig == null) {
			throw new IllegalStateException("Cannot find mode in Alonso-Mora config: " + mode);
		}

		controller.addOverridingModule(new AlonsoMoraModeModule(drtConfig));
		controller.addOverridingQSimModule(new AlonsoMoraModeQSimModule(drtConfig, amConfig));
	}

	static private AlonsoMoraConfigGroup getAlonsoMoraConfig(Config config, String mode) {
		MultiModeAlonsoMoraConfigGroup multiModeConfig = MultiModeAlonsoMoraConfigGroup.get(config);

		for (AlonsoMoraConfigGroup modeConfig : multiModeConfig.getModes().values()) {
			if (modeConfig.getMode().equals(mode)) {
				return modeConfig;
			}
		}

		return null;
	}

	static private DrtConfigGroup getDrtConfig(Config config, String mode) {
		MultiModeDrtConfigGroup multiModeConfig = MultiModeDrtConfigGroup.get(config);

		for (DrtConfigGroup modeConfig : multiModeConfig.getModalElements()) {
			if (modeConfig.getMode().equals(mode)) {
				return modeConfig;
			}
		}

		return null;
	}
}
