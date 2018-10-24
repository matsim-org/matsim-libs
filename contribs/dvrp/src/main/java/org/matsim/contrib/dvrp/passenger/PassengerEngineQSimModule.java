package org.matsim.contrib.dvrp.passenger;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class PassengerEngineQSimModule extends AbstractQSimModule {
	private final String mode;

	public PassengerEngineQSimModule(String mode) {
		this.mode = mode;
	}

	@Override
	protected void configureQSim() {
		bindNamedComponent(PassengerEngine.class, mode).toProvider(new PassengerEngineProvider(mode))
				.asEagerSingleton();
	}

	public static class PassengerEngineProvider implements Provider<PassengerEngine> {
		private final String mode;

		@Inject
		private Injector injector;
		@Inject
		private EventsManager eventsManager;
		@Inject
		@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
		private Network network;

		public PassengerEngineProvider(String mode) {
			this.mode = mode;
		}

		@Override
		public PassengerEngine get() {
			Named modeNamed = Names.named(mode);
			PassengerRequestCreator requestCreator = injector.getInstance(
					Key.get(PassengerRequestCreator.class, modeNamed));
			VrpOptimizer optimizer = injector.getInstance(Key.get(VrpOptimizer.class, modeNamed));
			return new PassengerEngine(mode, eventsManager, requestCreator, optimizer, network);
		}
	}
}
