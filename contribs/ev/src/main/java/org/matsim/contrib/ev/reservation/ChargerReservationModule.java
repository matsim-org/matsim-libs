package org.matsim.contrib.ev.reservation;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.charging.ChargingPriority;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

/**
 * This module enables the reservation-based charging logic that requires
 * vehicles to have or make a reservation when attempting to charge at a
 * charger.
 *
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ChargerReservationModule extends AbstractModule {
	private final boolean bindPriority;

	public ChargerReservationModule() {
		this(true);
	}

	public ChargerReservationModule(boolean bindPriority) {
		this.bindPriority = bindPriority;
	}

	@Override
	public void install() {
		if (getConfig().controller().getMobsim().equals("dsim")) {
			installQSimModule(new AbstractQSimModule() {
				@Override
				protected void configureQSim() {
					bind(DistributedChargerReservationManager.class).in(Singleton.class);
					addQSimComponentBinding(EvModule.EV_COMPONENT).to(DistributedChargerReservationManager.class);
				}
			});
		} else {
			addControllerListenerBinding().to(ChargerReservationManager.class);
		}
		if (bindPriority) {
			bind(ChargingPriority.Factory.class).to(ReservationBasedChargingPriority.Factory.class);
		}
	}

	@Provides
	@Singleton
	ReservationBasedChargingPriority.Factory provideReservationBasedChargingPriorityFactory(
		ChargerReservationManager reservationManager) {
		return new ReservationBasedChargingPriority.Factory(reservationManager);
	}

	@Provides
	@Singleton
	ChargerReservationManager provideChargerReservationManager(ChargerReservability chargerReservability) {
		return new ChargerReservationManager(chargerReservability);
	}

	@Provides
	@Singleton
	ChargerReservability provideChargerReservability() {
		return new ChargerReservability();
	}
}
