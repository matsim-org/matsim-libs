package org.matsim.contrib.dvrp.passenger;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.name.Named;

public class PassengerEngineQSimModule extends AbstractQSimModule {
	public final static String PASSENGER_ENGINE_NAME_PREFIX = "PassengerEngine_";

	private final String mode;

	public PassengerEngineQSimModule(String mode) {
		this.mode = mode;
	}

	@Override
	protected void configureQSim() {
		bind(PassengerEngine.class).toProvider(new PassengerEngineProvider(mode)).asEagerSingleton();

		bindDepartureHandler(PASSENGER_ENGINE_NAME_PREFIX + mode).to(PassengerEngine.class);
		bindMobsimEngine(PASSENGER_ENGINE_NAME_PREFIX + mode).to(PassengerEngine.class);
	}

	public static class PassengerEngineProvider implements Provider<PassengerEngine> {
		private final String mode;

		@Inject
		private EventsManager eventsManager;
		@Inject
		private PassengerRequestCreator requestCreator;
		@Inject
		private VrpOptimizer optimizer;
		@Inject
		@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
		private Network network;

		public PassengerEngineProvider(String mode) {
			this.mode = mode;
		}

		@Override
		public PassengerEngine get() {
			return new PassengerEngine(mode, eventsManager, requestCreator, optimizer, network);
		}
	}
}
