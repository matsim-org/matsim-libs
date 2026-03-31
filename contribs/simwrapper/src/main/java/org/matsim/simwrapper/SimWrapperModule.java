package org.matsim.simwrapper;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Install the SimWrapper Extension into MATSim.
 */
public final class SimWrapperModule extends AbstractModule {
	private static final Logger log = LoggerFactory.getLogger(SimWrapperModule.class);

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

		addControllerListenerBinding().to(SimWrapperListener.class);

		// Construct the binder one time, even through nothing is added
		// otherwise the injection will not work
		Multibinder.newSetBinder(binder(), Dashboard.class);
		Multibinder.newSetBinder(binder(), DashboardProvider.class);
	}

	@Provides
	@Singleton
	public SimWrapper getSimWrapper(Config config) {
		if (simWrapper == null)
			return SimWrapper.create(config);

		return simWrapper;
	}
}
