package org.matsim.core.mobsim.qsim;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import com.google.inject.multibindings.Multibinder;
import org.matsim.core.mobsim.framework.AbstractMobsimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponent;

import com.google.inject.Module;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;

public abstract class AbstractQSimModule extends AbstractMobsimModule {
	@Override
	protected final void configureMobsim() {
		configureQSim();
	}

	protected LinkedBindingBuilder<QSimComponent> addComponentBindingAnnotatedWith(Annotation annotation) {
		Multibinder<QSimComponent> multibinder = Multibinder.newSetBinder(binder(), QSimComponent.class, annotation);
		multibinder.permitDuplicates();
		return multibinder.addBinding();
	}

	protected LinkedBindingBuilder<QSimComponent> addComponentBindingAnnotatedWith(Class<? extends Annotation> annotationClass) {
		Multibinder<QSimComponent> multibinder = Multibinder.newSetBinder(binder(), QSimComponent.class, annotationClass);
		multibinder.permitDuplicates();
		return multibinder.addBinding();
	}
	
	protected LinkedBindingBuilder<QSimComponent> addComponentBindingNamed(String name) {
		return addComponentBindingAnnotatedWith(Names.named(name));
	}

	//TODO: Inline
	protected <T extends QSimComponent> void addNamedComponent(Class<T> componentClass, String name) {
		addComponentBindingNamed(name).to(componentClass);
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
