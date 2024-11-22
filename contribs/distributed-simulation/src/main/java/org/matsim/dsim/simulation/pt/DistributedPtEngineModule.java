package org.matsim.dsim.simulation.pt;

import com.google.inject.Provides;
import com.google.inject.multibindings.OptionalBinder;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.*;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.UmlaufBuilder;

import javax.inject.Singleton;

/**
 * This is copied from TransitEngineModule. Instead of the TransitQSimEngine, this binds the DistributedPtEngine.
 */
public class DistributedPtEngineModule extends AbstractQSimModule {

	public final static String TRANSIT_ENGINE_NAME = "DistributedTransitEngine";

	@Override
	protected void configureQSim() {
		bind(TransitQSimEngine.class).in(Singleton.class);
		bind(DistributedPtEngine.class).in(Singleton.class);
		addQSimComponentBinding(TRANSIT_ENGINE_NAME).to(DistributedPtEngine.class);
		if (this.getConfig().transit().isUseTransit() && this.getConfig().transit().isUsingTransitInMobsim()) {
			bind(TransitStopHandlerFactory.class).to(ComplexTransitStopHandlerFactory.class);
		} else {
			// Explicit bindings are required, so although it may not be used, we need provide something.
			bind(TransitStopHandlerFactory.class).to(SimpleTransitStopHandlerFactory.class);
		}

		bind(UmlaufBuilder.class).to(ReconstructingUmlaufBuilder.class);

		OptionalBinder.newOptionalBinder(binder(), TransitDriverAgentFactory.class)
			.setDefault().to(DefaultTransitDriverAgentFactory.class);
	}

	@Provides
	@com.google.inject.Singleton
	public TransitStopAgentTracker transitStopAgentTracker(Netsim qSim) {
		return new TransitStopAgentTracker(qSim.getEventsManager());
	}
}
