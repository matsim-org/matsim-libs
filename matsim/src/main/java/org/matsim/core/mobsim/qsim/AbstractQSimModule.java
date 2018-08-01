package org.matsim.core.mobsim.qsim;

import org.matsim.core.mobsim.framework.AbstractMobsimModule;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;

public abstract class AbstractQSimModule extends AbstractMobsimModule {
	private MapBinder<String, MobsimEngine> engineBinder;
	private MapBinder<String, ActivityHandler> activityHandlerBinder;
	private MapBinder<String, DepartureHandler> departureHandlerBinder;
	private MapBinder<String, AgentSource> agentSourceBinder;

	@Override
	protected final void configureMobsim() {
		this.engineBinder = MapBinder.newMapBinder(binder(), String.class, MobsimEngine.class);
		this.activityHandlerBinder = MapBinder.newMapBinder(binder(), String.class, ActivityHandler.class);
		this.departureHandlerBinder = MapBinder.newMapBinder(binder(), String.class, DepartureHandler.class);
		this.agentSourceBinder = MapBinder.newMapBinder(binder(), String.class, AgentSource.class);

		configureQSim();
	}

	protected LinkedBindingBuilder<MobsimEngine> addMobsimEngine(String name) {
		return engineBinder.addBinding(name);
	}

	protected LinkedBindingBuilder<ActivityHandler> addActivityHandler(String name) {
		return activityHandlerBinder.addBinding(name);
	}

	protected LinkedBindingBuilder<DepartureHandler> addDepartureHandler(String name) {
		return departureHandlerBinder.addBinding(name);
	}

	protected LinkedBindingBuilder<AgentSource> addAgentSource(String name) {
		return agentSourceBinder.addBinding(name);
	}

	protected abstract void configureQSim();
}
