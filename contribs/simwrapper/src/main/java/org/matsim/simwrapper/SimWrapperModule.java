package org.matsim.simwrapper;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;

/**
 * Install the SimWrapper Extension into MATSim.
 */
public final class SimWrapperModule extends AbstractModule {

	private final SimWrapper simWrapper;

	/**
	 * Create module with existing simwrapper instance.
	 */
	public SimWrapperModule(SimWrapper simWrapper) {
		this.simWrapper = simWrapper;
	}

	/**
	 * Constructor with a newly initialized {@link SimWrapper} instance.
	 */
	public SimWrapperModule() {
		this.simWrapper = null;
	}

	@Override
	public void install() {
		addControlerListenerBinding().to(SimWrapperListener.class);
	}

	@Provides
	@Singleton
	public SimWrapper getSimWrapper(Config config) {
		if (simWrapper == null)
			return SimWrapper.create(ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class));

		return simWrapper;
	}
}
