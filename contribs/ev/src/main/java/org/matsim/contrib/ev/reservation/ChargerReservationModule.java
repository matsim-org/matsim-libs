package org.matsim.contrib.ev.reservation;

import org.matsim.contrib.ev.charging.ChargingPriority;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

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
        addControlerListenerBinding().to(ChargerReservationManager.class);

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
    ChargerReservationManager provideChargerReservationManager() {
        return new ChargerReservationManager();
    }
}
