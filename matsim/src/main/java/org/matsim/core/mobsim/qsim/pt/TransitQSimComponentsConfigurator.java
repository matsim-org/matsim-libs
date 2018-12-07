package org.matsim.core.mobsim.qsim.pt;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.components.QSimComponentAnnotationsRegistry;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;

public class TransitQSimComponentsConfigurator implements QSimComponentsConfigurator {
	final private Config config;

	public TransitQSimComponentsConfigurator(Config config) {
		this.config = config;
	}

	@Override
	public void configure( QSimComponentAnnotationsRegistry components ) {
		if (config.transit().isUseTransit() && config.transit().isUsingTransitInMobsim()) {
			components.addNamedAnnotation(TransitEngineModule.TRANSIT_ENGINE_NAME );
		}
	}
}
