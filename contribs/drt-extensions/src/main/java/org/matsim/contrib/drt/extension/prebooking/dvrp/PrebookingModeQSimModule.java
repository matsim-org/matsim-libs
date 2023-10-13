package org.matsim.contrib.drt.extension.prebooking.dvrp;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.edrt.EDrtActionCreator;
import org.matsim.contrib.drt.extension.prebooking.electric.ElectricPrebookingActionCreator;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;

import com.google.inject.Singleton;

class PrebookingModeQSimModule extends AbstractDvrpModeQSimModule {
	private final boolean isElectric;

	PrebookingModeQSimModule(String mode, boolean isElectric) {
		super(mode);

		this.isElectric = isElectric;
	}

	@Override
	protected void configureQSim() {
		// bind the custom PrebookingActionCreator
		if (!isElectric) {
			bindModal(PrebookingActionCreator.class).toProvider(modalProvider(getter -> {
				PrebookingPassengerEngine passengerEngine = (PrebookingPassengerEngine) getter
						.getModal(PassengerHandler.class);
				DrtActionCreator delegate = getter.getModal(DrtActionCreator.class);
				PassengerStopDurationProvider stopDurationProvider = getter
						.getModal(PassengerStopDurationProvider.class);
				PassengerEnteringVehicleHandler enteringHandler = getter.getModal(PassengerEnteringVehicleHandler.class);

				return new PrebookingActionCreator(passengerEngine, delegate, stopDurationProvider, enteringHandler);
			})).in(Singleton.class);
			bindModal(VrpAgentLogic.DynActionCreator.class).to(modalKey(PrebookingActionCreator.class));
		} else {
			bindModal(ElectricPrebookingActionCreator.class).toProvider(modalProvider(getter -> {
				// delegate to edrt instead of drt
				EDrtActionCreator delegate = getter.getModal(EDrtActionCreator.class);

				PrebookingPassengerEngine passengerEngine = (PrebookingPassengerEngine) getter
						.getModal(PassengerHandler.class);
				PassengerStopDurationProvider stopDurationProvider = getter
						.getModal(PassengerStopDurationProvider.class);
				MobsimTimer timer = getter.get(MobsimTimer.class);
				PassengerEnteringVehicleHandler enteringHandler = getter.getModal(PassengerEnteringVehicleHandler.class);

				return new ElectricPrebookingActionCreator(passengerEngine, delegate, stopDurationProvider, timer,
						enteringHandler);
			})).in(Singleton.class);
			bindModal(VrpAgentLogic.DynActionCreator.class).to(modalKey(ElectricPrebookingActionCreator.class));
		}

		bindModal(PassengerEnteringVehicleHandler.class).toProvider(modalProvider(getter -> {
			EventsManager eventsManager = getter.get(EventsManager.class);
			return new PassengerEnteringVehicleHandler(eventsManager, getMode());
		})).in(Singleton.class);

		// override the PassengerEngine
		bindModal(PrebookingPassengerEngine.class).toProvider(modalProvider(getter -> {
			EventsManager eventsManager = getter.get(EventsManager.class);
			MobsimTimer mobsimTimer = getter.get(MobsimTimer.class);

			PassengerRequestCreator requestCreator = getter.getModal(PassengerRequestCreator.class);
			VrpOptimizer optimizer = getter.getModal(VrpOptimizer.class);
			Network network = getter.getModal(Network.class);
			PassengerRequestValidator requestValidator = getter.getModal(PassengerRequestValidator.class);
			PrebookingManager prebookingManager = getter.getModal(PrebookingManager.class);

			return new PrebookingPassengerEngine(getMode(), eventsManager, mobsimTimer, requestCreator, optimizer,
					network, requestValidator, prebookingManager);
		})).in(Singleton.class);
		bindModal(PassengerEngine.class).to(modalKey(PrebookingPassengerEngine.class));

		// bind the prebooking manager
		bindModal(PrebookingManager.class).toProvider(modalProvider(getter -> {
			Network network = getter.getModal(Network.class);
			PassengerRequestCreator requestCreator = getter.getModal(PassengerRequestCreator.class);
			VrpOptimizer optimizer = getter.getModal(VrpOptimizer.class);
			PassengerRequestValidator requestValidator = getter.getModal(PassengerRequestValidator.class);
			EventsManager eventsManager = getter.get(EventsManager.class);

			return new PrebookingManager(getMode(), network, requestCreator, optimizer, requestValidator,
					eventsManager);
		})).in(Singleton.class);
		addModalQSimComponentBinding().to(modalKey(PrebookingManager.class));
	}
}
