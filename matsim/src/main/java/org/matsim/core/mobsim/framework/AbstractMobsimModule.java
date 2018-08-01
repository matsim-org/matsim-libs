package org.matsim.core.mobsim.framework;

import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import com.google.inject.AbstractModule;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;

public abstract class AbstractMobsimModule extends AbstractModule {
	private MapBinder<String, MobsimListener> listenerBinder;

	protected final void configure() {
		this.listenerBinder = MapBinder.newMapBinder(binder(), String.class, MobsimListener.class);
		configureMobsim();
	}

	protected LinkedBindingBuilder<MobsimListener> bindMobsimListener(String name) {
		return listenerBinder.addBinding(name);
	}

	protected abstract void configureMobsim();
}
