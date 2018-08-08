package org.matsim.core.mobsim.qsim;

import java.util.Collection;
import java.util.List;

import org.matsim.core.mobsim.framework.AbstractMobsimModule;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;

public abstract class AbstractQSimModule extends AbstractMobsimModule {
	@Override
	protected final void configureMobsim() {
		configureQSim();
	}

	protected LinkedBindingBuilder<MobsimEngine> bindMobsimEngine(String name) {
		return binder().bind(Key.get(MobsimEngine.class, Names.named(name)));
	}

	protected LinkedBindingBuilder<ActivityHandler> bindActivityHandler(String name) {
		return binder().bind(Key.get(ActivityHandler.class, Names.named(name)));
	}

	protected LinkedBindingBuilder<DepartureHandler> bindDepartureHandler(String name) {
		return binder().bind(Key.get(DepartureHandler.class, Names.named(name)));
	}

	protected LinkedBindingBuilder<AgentSource> bindAgentSource(String name) {
		return binder().bind(Key.get(AgentSource.class, Names.named(name)));
	}

	protected LinkedBindingBuilder<MobsimListener> bindMobsimListener(String name) {
		return binder().bind(Key.get(MobsimListener.class, Names.named(name)));
	}

	protected abstract void configureQSim();

	public static AbstractQSimModule overrideQSimModules(Collection<AbstractQSimModule> base,
			List<AbstractQSimModule> overrides) {
		Module composite = Modules.override(base).with(overrides);

		AbstractQSimModule wrapper = new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				install(composite);
			}
		};

		base.forEach(m -> m.setParent(wrapper));
		overrides.forEach(m -> m.setParent(wrapper));

		return wrapper;
	}
}
