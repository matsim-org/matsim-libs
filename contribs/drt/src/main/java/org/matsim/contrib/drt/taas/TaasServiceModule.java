package org.matsim.contrib.drt.taas;

import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.routing.DrtRouteConstraintsCalculator;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;

import com.google.inject.Singleton;

public class TaasServiceModule extends AbstractDvrpModeModule {
	public double maximumPassengerWaitTime = 300.0;
	public double maximumPassengerTravelDelay = 600.0;
	public double passengerInteractionDuration = 30.0;

	public double parcelLatestDeliveryTime = 20.0 * 3600.0;
	public double parcelPickupDuration = 30.0;
	public double parcelDeliveryDuration = 4.0 * 60.0;

	public TaasServiceModule(String mode) {
		super(mode);
	}

	@Override
	public void install() {
		bindModal(TaasServiceConfigurator.class).toProvider(modalProvider(getter -> {
			Population population = getter.get(Population.class);
			return new TaasServiceConfigurator(population, maximumPassengerWaitTime, maximumPassengerTravelDelay,
					passengerInteractionDuration, parcelLatestDeliveryTime, parcelPickupDuration,
					parcelDeliveryDuration);
		})).in(Singleton.class);

		bindModal(PassengerStopDurationProvider.class).to(modalKey(TaasServiceConfigurator.class));
		bindModal(DrtRouteConstraintsCalculator.class).to(modalKey(TaasServiceConfigurator.class));
	}
}
