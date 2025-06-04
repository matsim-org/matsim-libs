package org.matsim.contrib.ev.withinday.utils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.withinday.ChargingSlot;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.ActivityBasedCandidate;
import org.matsim.contrib.ev.withinday.ChargingSlotProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class WorkActivitySlotProvider implements ChargingSlotProvider {
	@Inject
	ChargingInfrastructure infrastructure;

	@Inject
	Scenario scenario;

	@Override
	public List<ChargingSlot> findSlots(Person person, Plan plan, ElectricVehicle vehicle) {
		ChargingSlotFinder finder = new ChargingSlotFinder(scenario, "car");

		List<Charger> chargers = new LinkedList<>();
		chargers.addAll(infrastructure.getChargers().values());

		Collections.sort(chargers, (a, b) -> {
			return String.CASE_INSENSITIVE_ORDER.compare(a.getId().toString(), b.getId().toString());
		});

		Charger charger = chargers.get(0);

		List<ChargingSlot> slots = new LinkedList<>();

		for (ActivityBasedCandidate candidate : finder.findActivityBased(person, plan)) {
			if (candidate.startActivity().getType().startsWith("work")
					|| candidate.endActivity().getType().startsWith("work")) {
				slots.add(new ChargingSlot(candidate.startActivity(), candidate.endActivity(), null, 0.0, charger));
			}
		}

		return slots;
	}
}
