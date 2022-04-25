package org.matsim.contrib.shared_mobility.run;

import org.matsim.contrib.shared_mobility.analysis.SharingAnalysisModule;
import org.matsim.core.controler.AbstractModule;

public class SharingModule extends AbstractModule {
	@Override
	public void install() {
		SharingConfigGroup micromobilityConfig = (SharingConfigGroup) getConfig().getModules()
				.get(SharingConfigGroup.GROUP_NAME);

		for (SharingServiceConfigGroup modeConfig : micromobilityConfig.getServices()) {
			install(new SharingServiceModule(modeConfig));
		}

		install(new SharingAnalysisModule());
	}
}
