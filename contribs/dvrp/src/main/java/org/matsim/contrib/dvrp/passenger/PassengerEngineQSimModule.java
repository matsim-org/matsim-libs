package org.matsim.contrib.dvrp.passenger;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponents;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class PassengerEngineQSimModule extends AbstractQSimModule {
	public final static String PASSENGER_ENGINE_NAME_PREFIX = "PassengerEngine_";

	private final String mode;

	public PassengerEngineQSimModule(String mode) {
		this.mode = mode;
	}

	@Override
	protected void configureQSim() {
		bind(PassengerEngine.class).annotatedWith(Names.named(mode))
				.toProvider(new PassengerEngineProvider(mode))
				.asEagerSingleton();
		Named modeNamed = Names.named(mode);
		bindNamedDepartureHandler(PASSENGER_ENGINE_NAME_PREFIX + mode).to(Key.get(PassengerEngine.class, modeNamed));
		bindNamedMobsimEngine(PASSENGER_ENGINE_NAME_PREFIX + mode).to(Key.get(PassengerEngine.class, modeNamed));
	}

	public static void configureComponents(QSimComponents components, String mode) {
		components.addNamedComponent(PassengerEngineQSimModule.PASSENGER_ENGINE_NAME_PREFIX + mode);
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
