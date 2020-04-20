package org.matsim.contrib.dvrp.passenger;

import javax.inject.Inject;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.PreplanningEngine;

public class PassengerEngineQSimModule extends AbstractDvrpModeQSimModule {
	public PassengerEngineQSimModule(String mode) {
		super(mode);
	}

	@Override
	protected void configureQSim() {
		addModalComponent(PassengerEngine.class, new ModalProviders.AbstractProvider<PassengerEngine>(getMode()) {
			@Inject
			private EventsManager eventsManager;

			@Inject
			private MobsimTimer mobsimTimer;

			@Inject
			private PreplanningEngine preplanningEngine;

			@Inject
			private PassengerRequestEventToPassengerEngineForwarder passengerRequestEventForwarder;

			@Override
			public PassengerEngine get() {
				return new PassengerEngine(getMode(), eventsManager, mobsimTimer, preplanningEngine,
						getModalInstance(PassengerRequestCreator.class), getModalInstance(VrpOptimizer.class),
						getModalInstance(Network.class), getModalInstance(PassengerRequestValidator.class),
						passengerRequestEventForwarder);
			}
		});
	}
}
