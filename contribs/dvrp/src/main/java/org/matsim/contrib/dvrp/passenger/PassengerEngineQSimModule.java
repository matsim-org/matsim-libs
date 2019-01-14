package org.matsim.contrib.dvrp.passenger;

import javax.inject.Inject;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.core.api.experimental.events.EventsManager;

import com.google.inject.name.Named;

public class PassengerEngineQSimModule extends AbstractDvrpModeQSimModule {
	public PassengerEngineQSimModule(String mode) {
		super(mode);
	}

	@Override
	protected void configureQSim() {
		addModalComponent(PassengerEngine.class,
				new ModalProviders.AbstractProvider<PassengerEngine>(getMode()) {
					@Inject
					private EventsManager eventsManager;

					@Inject
					@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
					private Network network;

					@Override
					public PassengerEngine get() {
						return new PassengerEngine(getMode(), eventsManager,
								getModalInstance(PassengerRequestCreator.class), getModalInstance(VrpOptimizer.class),
								network, getModalInstance(PassengerRequestValidator.class));
					}
				});
	}
}
