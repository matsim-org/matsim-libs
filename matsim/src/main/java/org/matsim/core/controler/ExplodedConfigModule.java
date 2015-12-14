package org.matsim.core.controler;

import com.google.inject.Binder;
import com.google.inject.Module;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

public class ExplodedConfigModule implements Module {
	private final Config config;

	public ExplodedConfigModule(Config config) {
		this.config = config;
	}

	@Override
	public void configure(Binder binder) {
		binder.bind(Config.class).toInstance(config);
		for (ConfigGroup configGroup : config.getModules().values()) {
			Class materializedConfigGroupSubclass = configGroup.getClass();
			if (materializedConfigGroupSubclass != ConfigGroup.class) {
				binder.bind(materializedConfigGroupSubclass).toInstance(configGroup);
			}
		}
	}
}
