package org.matsim.contrib.ev.withinday.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.annotation.Nullable;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.withinday.ChargingAlternative;
import org.matsim.contrib.ev.withinday.ChargingAlternativeProvider;
import org.matsim.contrib.ev.withinday.ChargingSlot;

import java.util.LinkedList;
import java.util.List;

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

		List<Charger> chargers = new LinkedList<>(infrastructure.getChargers().values());
		chargers.removeIf(c -> c.getId().equals(slot.charger().getId()));
		for (ChargingAlternative s : trace) {
			chargers.removeIf(c -> c.getId().equals(s.charger()));
		}

		chargers.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getId().toString(), b.getId().toString()));

		if (!chargers.isEmpty()) {
			if (!slot.isLegBased()) {
				return new ChargingAlternative(chargers.getFirst().getId());
			} else {
				return new ChargingAlternative(chargers.getFirst().getId(), slot.duration());
			}
		}

		return null;
	}

}
