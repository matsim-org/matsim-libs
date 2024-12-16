package org.matsim.contrib.ev.withinday.utils;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.reservation.ChargerReservationManager;
import org.matsim.contrib.ev.withinday.ChargingAlternative;
import org.matsim.contrib.ev.withinday.ChargingSlot;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ReservationAlternativeProvider extends OrderedAlternativeProvider {
    @Inject
    ChargingInfrastructure infrastructure;

    @Inject
    ChargerReservationManager manager;

    @SuppressWarnings("null")
    @Override
    @Nullable
    public ChargingAlternative findEnrouteAlternative(double now, Person person, Plan plan,
            ElectricVehicle vehicle, @Nullable ChargingSlot initialSlot) {
        if (person.getId().toString().equals("person2")) {
            manager.addReservation(initialSlot.charger().getSpecification(), vehicle, now, Double.POSITIVE_INFINITY);
        }

        return null;
    }
}
