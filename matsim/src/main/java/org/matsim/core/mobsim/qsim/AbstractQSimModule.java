package org.matsim.core.mobsim.qsim;

import java.util.Optional;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AbstractMobsimModule;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import com.google.inject.Key;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;

public abstract class AbstractQSimModule extends AbstractMobsimModule {
	private Optional<Config> config;

	public void setConfig(Config config) {
		this.config = Optional.of(config);
	}

	protected Config getConfig() {
		if (!config.isPresent()) {
			throw new IllegalStateException(
					"No config set. Did you try to use the module outside of the QSim initialization process?");
		}

		return config.get();
	}

	@Override
	protected final void configureMobsim() {
		configureQSim();
	}

	protected LinkedBindingBuilder<MobsimEngine> addMobsimEngineBinding(String name) {
		return binder().bind(Key.get(MobsimEngine.class, Names.named(name)));
	}

	protected LinkedBindingBuilder<ActivityHandler> addActivityHandlerBinding(String name) {
		return binder().bind(Key.get(ActivityHandler.class, Names.named(name)));
	}

	protected LinkedBindingBuilder<DepartureHandler> addDepartureHandlerBinding(String name) {
		return binder().bind(Key.get(DepartureHandler.class, Names.named(name)));
	}

	protected LinkedBindingBuilder<AgentSource> addAgentSourceBinding(String name) {
		return binder().bind(Key.get(AgentSource.class, Names.named(name)));
	}

	protected abstract void configureQSim();
}
