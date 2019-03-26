package org.matsim.contrib.dynagent.run;

import com.google.inject.Singleton;
import org.matsim.contrib.dvrp.passenger.ActivityEngineWithWakeup;
import org.matsim.contrib.dvrp.passenger.WakeupGenerator;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.ActivityEngineModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;

import com.google.inject.multibindings.Multibinder;

public class DynActivityEngineModule extends AbstractQSimModule {
	public final static String COMPONENT_NAME = "DynActivityEngine";

	@Override
	protected void configureQSim() {
		bind(DynActivityEngine.class).in( Singleton.class ) ;
		bind(ActivityEngineWithWakeup.class).in( Singleton.class ) ;
		Multibinder.newSetBinder(this.binder(), WakeupGenerator.class);
		addQSimComponentBinding( COMPONENT_NAME ).to( DynActivityEngine.class );
	}

	public static void configureComponents(QSimComponentsConfig components) {
		components.removeNamedComponent(ActivityEngineModule.COMPONENT_NAME);
		components.addNamedComponent(COMPONENT_NAME);
	}
}
