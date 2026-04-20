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
		return null;
	}

	@Override
	public void findEnrouteAlternativeAsync(double now, Person person, Plan plan, ElectricVehicle vehicle,
	                                        @Nullable ChargingSlot slot, Consumer<Optional<ChargingAlternative>> callback) {

		if (person.getId().toString().equals("person2")) {
			manager.addReservation(slot.charger().getId(), vehicle.getId(), now, Double.POSITIVE_INFINITY, optResrvation -> {
				if (optResrvation.isPresent()) {
					var result = new ChargingAlternative(slot.charger().getId(), slot.duration());
					callback.accept(Optional.of(result));
				} else {
					callback.accept(Optional.empty());
				}
			});
		}
		callback.accept(Optional.empty());
	}
}
