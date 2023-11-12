package org.matsim.contrib.drt.prebooking;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.prebooking.logic.helpers.PopulationIterator.PopulationIteratorFactory;
import org.matsim.contrib.drt.prebooking.logic.helpers.PrebookingQueue;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.QSim;

import com.google.inject.Singleton;

public class PrebookingModeQSimModule extends AbstractDvrpModeQSimModule {
	public PrebookingModeQSimModule(String mode) {
		super(mode);
	}

	@Override
	protected void configureQSim() {
		bindModal(PrebookingActionCreator.class).toProvider(modalProvider(getter -> {
			PassengerHandler passengerHandler = (PassengerEngine) getter.getModal(PassengerHandler.class);
			DrtActionCreator delegate = getter.getModal(DrtActionCreator.class);
			PassengerStopDurationProvider stopDurationProvider = getter.getModal(PassengerStopDurationProvider.class);

			return new PrebookingActionCreator(passengerHandler, delegate, stopDurationProvider);
		})).in(Singleton.class);

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

		bindModal(PrebookingQueue.class).toProvider(modalProvider(getter -> {
			return new PrebookingQueue(getter.getModal(PrebookingManager.class));
		})).in(Singleton.class);
		addModalQSimComponentBinding().to(modalKey(PrebookingQueue.class));

		bindModal(PopulationIteratorFactory.class).toProvider(modalProvider(getter -> {
			return new PopulationIteratorFactory(getter.get(Population.class), getter.get(QSim.class));
		}));
	}
}
