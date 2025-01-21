package org.matsim.contrib.ev.withinday.utils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.withinday.ChargingAlternative;
import org.matsim.contrib.ev.withinday.ChargingAlternativeProvider;
import org.matsim.contrib.ev.withinday.ChargingSlot;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class OrderedAlternativeProvider implements ChargingAlternativeProvider {
    @Inject
    ChargingInfrastructure infrastructure;

    @Override
    public ChargingAlternative findEnrouteAlternative(double now, Person person, Plan plan,
            ElectricVehicle vehicle,
            @Nullable ChargingSlot initialSlot) {
        return null;
    }

    @SuppressWarnings("null")
    @Override
    @Nullable
    public ChargingAlternative findAlternative(double now, Person person, Plan plan, ElectricVehicle vehicle,
            @Nullable ChargingSlot slot, List<ChargingAlternative> trace) {
        List<Charger> chargers = new LinkedList<>();
        chargers.addAll(infrastructure.getChargers().values());

        chargers.remove(slot.charger());
        for (ChargingAlternative s : trace) {
            chargers.remove(s.charger());
        }

        Collections.sort(chargers, (a, b) -> {
            return String.CASE_INSENSITIVE_ORDER.compare(a.getId().toString(), b.getId().toString());
        });

        if (chargers.size() > 0) {
            if (!slot.isLegBased()) {
                return new ChargingAlternative(chargers.get(0));
            } else {
                return new ChargingAlternative(chargers.get(0), slot.duration());
            }
        }

        return null;
    }

}
