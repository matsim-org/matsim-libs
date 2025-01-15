package org.matsim.contrib.ev.withinday.utils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.withinday.ChargingSlot;
import org.matsim.contrib.ev.withinday.ChargingSlotProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FirstLegSlotProvider implements ChargingSlotProvider {
	@Inject
	private ChargingInfrastructure infrastructure;

	private final static double DURATION = 3600.0;

	@Override
	public List<ChargingSlot> findSlots(Person person, Plan plan, ElectricVehicle vehicle) {
		List<Charger> chargers = new LinkedList<>();
		chargers.addAll(infrastructure.getChargers().values());

		Collections.sort(chargers, (a, b) -> {
			return String.CASE_INSENSITIVE_ORDER.compare(a.getId().toString(), b.getId().toString());
		});

		Charger charger = chargers.get(0);

		for (PlanElement element : plan.getPlanElements()) {
			if (element instanceof Leg leg) {
				return Collections.singletonList(new ChargingSlot(null, null, leg, DURATION, charger));
			}
		}

		return Collections.emptyList();
	}
}
