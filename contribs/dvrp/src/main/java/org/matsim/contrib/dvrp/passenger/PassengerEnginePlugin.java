package org.matsim.contrib.dvrp.passenger;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Named;

public class PassengerEnginePlugin extends AbstractQSimPlugin {
	public final static String PASSENGER_ENGINE_NAME = "PassengerEngine";
	
	private final String mode;

	public PassengerEnginePlugin(Config config, String mode) {
		super(config);
		this.mode = mode;
	}

	@Override
	public Collection<? extends Module> modules() {
		return Collections.singletonList(new AbstractModule() {
			@Override
			public void configure() {
				bind(PassengerEngine.class).toProvider(new PassengerEngineProvider(mode)).asEagerSingleton();
			}
		});
	}

	@Override
	public Map<String, Class<? extends DepartureHandler>> departureHandlers() {
		return Collections.singletonMap(PASSENGER_ENGINE_NAME, PassengerEngine.class);
	}

	@Override
	public Map<String, Class<? extends MobsimEngine>> engines() {
		return Collections.singletonMap(PASSENGER_ENGINE_NAME, PassengerEngine.class);
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
