package org.matsim.contrib.ev.withinday.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.annotation.Nullable;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.reservation.DistributedChargerReservationManager;
import org.matsim.contrib.ev.withinday.ChargingAlternative;
import org.matsim.contrib.ev.withinday.ChargingSlot;
import org.matsim.core.mobsim.framework.PlanAgent;

import java.util.Optional;
import java.util.function.Consumer;

@Singleton
public class ReservationAlternativeProvider extends OrderedAlternativeProvider {
	@Inject
	ChargingInfrastructure infrastructure;

	@Inject
	DistributedChargerReservationManager manager;

	@SuppressWarnings("null")
	@Override
	@Nullable
	public ChargingAlternative findEnrouteAlternative(double now, Person person, Plan plan,
	                                                  ElectricVehicle vehicle, @Nullable ChargingSlot initialSlot) {
		if (person.getId().toString().equals("person2")) {
			// ignore the results. We just want the reservation
			manager.addLocalReservation(initialSlot.charger().getId(), vehicle.getId(), now, Double.POSITIVE_INFINITY);
		}
		return null;
	}

	@Override
	public void findEnrouteAlternativeAsync(double now, PlanAgent agent, ElectricVehicle vehicle,
	                                        @Nullable ChargingSlot slot, Consumer<Optional<ChargingAlternative>> callback) {

		if (agent.getCurrentPlan().getPerson().getId().toString().equals("person2")) {
			manager.addReservation(slot.charger().getId(), vehicle.getId(), now, Double.POSITIVE_INFINITY, optResrvation -> {
				// ignore the results. We just want the reservation
				callback.accept(Optional.empty());
			});
		}
		callback.accept(Optional.empty());
	}
}
