package org.matsim.contrib.ev.strategic.infrastructure;

import org.matsim.core.controler.AbstractModule;

import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;

/**
 * This class is used to bind new charger sources for startegic charging.
 */
public abstract class AbstractChargerProviderModule extends AbstractModule {
	private Multibinder<ChargerProvider> chargerProviderBinder;

	@Override
	public void install() {
		this.chargerProviderBinder = Multibinder.newSetBinder(binder(), ChargerProvider.class);
		configureChargingStrategies();
	}

	protected final LinkedBindingBuilder<ChargerProvider> bindChargerProvider() {
		return chargerProviderBinder.addBinding();
	}

	abstract protected void configureChargingStrategies();
}
