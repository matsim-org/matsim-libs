package org.matsim.contrib.dvrp.passenger;

import javax.inject.Inject;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.name.Named;

public class PassengerEngineQSimModule extends AbstractQSimModule {
	private final String mode;

	public PassengerEngineQSimModule(String mode) {
		this.mode = mode;
	}

	@Override
	protected void configureQSim() {
		bindComponent(PassengerEngine.class, DvrpModes.mode(mode)).toProvider(
				new ModalProviders.AbstractProvider<PassengerEngine>(mode) {
					@Inject
					private EventsManager eventsManager;

					@Inject
					@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
					private Network network;

					@Override
					public PassengerEngine get() {
						return new PassengerEngine(mode, eventsManager, getModalInstance(PassengerRequestCreator.class),
								getModalInstance(VrpOptimizer.class), network);
					}
				}).asEagerSingleton();
	}
}
