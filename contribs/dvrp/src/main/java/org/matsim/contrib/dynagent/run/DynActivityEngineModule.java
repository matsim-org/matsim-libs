package org.matsim.contrib.dynagent.run;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;

public class DynActivityEngineModule extends AbstractQSimModule {
	public final static String COMPONENT_NAME = "DynActivityEngine";

	@Override
	protected void configureQSim() {
		bind(DynActivityEngine.class).asEagerSingleton();
		addQSimComponentBinding( COMPONENT_NAME ).to( DynActivityEngine.class );
	}

	public static void configureComponents(QSimComponentsConfig components) {
//		components.removeNamedComponent(ActivityEngineModule.COMPONENT_NAME);
		// do not remove this one any more since multiple activity handlers are allowed. kai, apr'19

		components.addNamedComponent(COMPONENT_NAME);
	}
}
