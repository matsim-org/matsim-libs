package org.matsim.contrib.ev.withinday.utils;

import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
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
public class LastActivitySlotProvider implements ChargingSlotProvider {
	@Inject
	private ChargingInfrastructure infrastructure;

	@Override
	public List<ChargingSlot> findSlots(Person person, Plan plan, ElectricVehicle vehicle) {
		List<? extends PlanElement> elements = plan.getPlanElements();
		Activity activity = (Activity) elements.get(elements.size() - 1);
		Charger charger = infrastructure.getChargers().values().iterator().next();
		return Collections.singletonList(new ChargingSlot(activity, activity, null, 0.0, charger));
	}
}
