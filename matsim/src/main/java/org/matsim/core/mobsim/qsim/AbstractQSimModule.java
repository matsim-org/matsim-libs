package org.matsim.core.mobsim.qsim;

import com.google.inject.Module;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import org.matsim.core.mobsim.framework.AbstractMobsimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponent;

import java.util.Collection;
import java.util.List;

public abstract class AbstractQSimModule extends AbstractMobsimModule {
	@Override
	protected final void configureMobsim() {
		configureQSim();
	}

	protected final LinkedBindingBuilder<QSimComponent> addQSimComponentBinding( String label ) {
//		return binder().bind(QSimComponent.class).annotatedWith(Names.named(label));
		MapBinder<String,QSimComponent> mapBinder = MapBinder.newMapBinder( binder(), String.class, QSimComponent.class ) ;
		mapBinder.permitDuplicates() ;
		return mapBinder.addBinding( label ) ;
	}

	// if we really want support for non-String annotations, it could look as follows.  kai, dec'18
//	protected final LinkedBindingBuilder<QSimComponent> addQSimComponentBinding( Annotation annotation ) {
//		return binder().bind(QSimComponent.class).annotatedWith( annotation );
//	}
//	protected final LinkedBindingBuilder<QSimComponent> addQSimComponentBinding( Class<? extends Annotation> annotationClass ) {
//		return binder().bind(QSimComponent.class).annotatedWith( annotationClass );
//	}


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
