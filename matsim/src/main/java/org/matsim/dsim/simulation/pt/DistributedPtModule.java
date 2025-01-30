package org.matsim.dsim.simulation.pt;

import com.google.inject.Singleton;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.pt.TransitEngineModule;

public class DistributedPtModule extends AbstractQSimModule {

	@Override
	protected void configureQSim() {
		bind(DistributedPtEngine.class).in(Singleton.class);
		addQSimComponentBinding(TransitEngineModule.TRANSIT_ENGINE_NAME).to(DistributedPtEngine.class);
	}
}
