package org.matsim.core.mobsim.qsim;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import org.matsim.core.mobsim.framework.AbstractMobsimModule;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import org.matsim.core.router.util.TravelTime;

public abstract class AbstractQSimModule extends AbstractMobsimModule {
	@Override
	protected final void configureMobsim() {
		configureQSim();
	}
	
//	protected <T extends QSimComponent> AnnotatedBindingBuilder<T> bindComponent(Class<T> componentClass) {
//		return binder().bind(componentClass);
//	}
//
//	protected <T extends QSimComponent> LinkedBindingBuilder<T> bindComponent(Class<T> componentClass, Annotation annotation) {
//		return bindComponent(componentClass).annotatedWith(annotation);
//	}
//
//	protected <T extends QSimComponent> LinkedBindingBuilder<T> bindComponent(Class<T> componentClass, Class<? extends Annotation> annotationClass) {
//		return bindComponent(componentClass).annotatedWith(annotationClass);
//	}
//
//	protected <T extends QSimComponent> LinkedBindingBuilder<T> bindNamedComponent(Class<T> componentClass, String name) {
//		return bindComponent(componentClass).annotatedWith(Names.named(name));
//	}
//
//	protected <T extends QSimComponent> void addComponent(Class<T> componentClass, Annotation annotation) {
//		bindComponent(componentClass, annotation).to(componentClass);
//	}
//
//	protected <T extends QSimComponent> void addComponent(Class<T> componentClass, Class<? extends Annotation> annotationClass) {
//		bindComponent(componentClass, annotationClass).to(componentClass);
//	}
//
//	protected <T extends QSimComponent> void addNamedComponent(Class<T> componentClass, String name) {
//		bindNamedComponent(componentClass, name).to(componentClass);
//	}
	// the above would need to be inlined to the below form before they could be removed

	protected final com.google.inject.binder.LinkedBindingBuilder<QSimComponent> addQSimComponentBinding( String label ) {
		return binder().bind(QSimComponent.class).annotatedWith(Names.named(label));
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
