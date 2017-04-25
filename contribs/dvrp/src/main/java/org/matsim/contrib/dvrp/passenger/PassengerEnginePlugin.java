package org.matsim.contrib.dvrp.passenger;

import java.util.*;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.interfaces.*;

import com.google.inject.*;
import com.google.inject.name.Named;

public class PassengerEnginePlugin extends AbstractQSimPlugin {
	private final String mode;

	public PassengerEnginePlugin(Config config, String mode) {
		super(config);
		this.mode = mode;
	}

	@Override
	public Collection<? extends Module> modules() {
		Collection<Module> result = new ArrayList<>();
		result.add(new AbstractModule() {
			@Override
			public void configure() {
				bind(PassengerEngine.class).toProvider(new PassengerEngineProvider(mode)).asEagerSingleton();
			}
		});
		return result;
	}

	@Override
	public Collection<Class<? extends DepartureHandler>> departureHandlers() {
		Collection<Class<? extends DepartureHandler>> result = new ArrayList<>();
		result.add(PassengerEngine.class);
		return result;
	}

	@Override
	public Collection<Class<? extends MobsimEngine>> engines() {
		Collection<Class<? extends MobsimEngine>> result = new ArrayList<>();
		result.add(PassengerEngine.class);
		return result;
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
		@Named(DvrpModule.DVRP_ROUTING)
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
