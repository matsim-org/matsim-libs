package org.matsim.core.mobsim.framework;

import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;

public abstract class AbstractMobsimModule extends AbstractModule {
	protected LinkedBindingBuilder<MobsimListener> bindMobsimListener(String name) {
		return binder().bind(Key.get(MobsimListener.class, Names.named(name)));
	}

	protected final void configure() {
		configureMobsim();
	}

	protected abstract void configureMobsim();
}
