package org.matsim.core.mobsim.qsim;

import java.lang.annotation.Annotation;
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
	
	protected LinkedBindingBuilder<MobsimEngine> bindNamedMobsimEngine(String name) {
		return bindMobsimEngine(Names.named(name));
	}
	
	protected LinkedBindingBuilder<MobsimEngine> bindMobsimEngine(Annotation annotation) {
		return binder().bind(Key.get(MobsimEngine.class, annotation));
	}
	
	protected LinkedBindingBuilder<MobsimEngine> bindMobsimEngine(Class<? extends Annotation> annotation) {
		return binder().bind(Key.get(MobsimEngine.class, annotation));
	}

	protected LinkedBindingBuilder<ActivityHandler> bindNamedActivityHandler(String name) {
		return bindActivityHandler(Names.named(name));
	}
	
	protected LinkedBindingBuilder<ActivityHandler> bindActivityHandler(Annotation annotation) {
		return binder().bind(Key.get(ActivityHandler.class, annotation));
	}
	
	protected LinkedBindingBuilder<ActivityHandler> bindActivityHandler(Class<? extends Annotation> annotation) {
		return binder().bind(Key.get(ActivityHandler.class, annotation));
	}

	protected LinkedBindingBuilder<DepartureHandler> bindNamedDepartureHandler(String name) {
		return bindDepartureHandler(Names.named(name));
	}
	
	protected LinkedBindingBuilder<DepartureHandler> bindDepartureHandler(Annotation annotation) {
		return binder().bind(Key.get(DepartureHandler.class, annotation));
	}
	
	protected LinkedBindingBuilder<DepartureHandler> bindDepartureHandler(Class<? extends Annotation> annotation) {
		return binder().bind(Key.get(DepartureHandler.class, annotation));
	}

	protected LinkedBindingBuilder<AgentSource> bindNamedAgentSource(String name) {
		return bindAgentSource(Names.named(name));
	}
	
	protected LinkedBindingBuilder<AgentSource> bindAgentSource(Annotation annotation) {
		return binder().bind(Key.get(AgentSource.class, annotation));
	}
	
	protected LinkedBindingBuilder<AgentSource> bindAgentSource(Class<? extends Annotation> annotation) {
		return binder().bind(Key.get(AgentSource.class, annotation));
	}

	protected LinkedBindingBuilder<MobsimListener> bindNamedMobsimListener(String name) {
		return bindMobsimListener(Names.named(name));
	}
	
	protected LinkedBindingBuilder<MobsimListener> bindMobsimListener(Annotation annotation) {
		return binder().bind(Key.get(MobsimListener.class, annotation));
	}
	
	protected LinkedBindingBuilder<MobsimListener> bindMobsimListener(Class<? extends Annotation> annotation) {
		return binder().bind(Key.get(MobsimListener.class, annotation));
	}

	protected abstract void configureQSim();
	
	protected void install(AbstractQSimModule module) {
		module.setParent(this);
		super.install(module);
	}

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
