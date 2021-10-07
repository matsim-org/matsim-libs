package org.matsim.contrib.sharing.run;

import org.matsim.core.controler.AbstractModule;

public class SharingModule extends AbstractModule {
	@Override
	public void install() {
		SharingConfigGroup micromobilityConfig = (SharingConfigGroup) getConfig().getModules()
				.get(SharingConfigGroup.GROUP_NAME);

		for (SharingServiceConfigGroup modeConfig : micromobilityConfig.getServices()) {
			install(new SharingServiceModule(modeConfig));
		}
	}
}
