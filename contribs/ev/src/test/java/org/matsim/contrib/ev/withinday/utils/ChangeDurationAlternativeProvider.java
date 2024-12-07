package org.matsim.contrib.ev.withinday.utils;

import java.util.List;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.withinday.ChargingAlternative;
import org.matsim.contrib.ev.withinday.ChargingAlternativeProvider;
import org.matsim.contrib.ev.withinday.ChargingSlot;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ChangeDurationAlternativeProvider implements ChargingAlternativeProvider {
    @Inject
    ChargingInfrastructure infrastructure;

    @SuppressWarnings("null")
    @Override
    public ChargingAlternative findEnrouteAlternative(double now, Person person, Plan plan,
            ElectricVehicle vehicle,
            @Nullable ChargingSlot initialSlot) {
        return new ChargingAlternative(initialSlot.charger(), 1800.0);
    }

    @Override
    public ChargingAlternative findAlternative(double now, Person person, Plan plan, ElectricVehicle vehicle,
            @Nullable ChargingSlot slot, List<ChargingAlternative> trace) {
        return null;
    }

}
