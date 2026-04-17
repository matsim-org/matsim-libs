package org.matsim.contrib.ev.withinday.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.annotation.Nullable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.reservation.DistributedChargerReservationManager;
import org.matsim.contrib.ev.withinday.ChargingAlternative;
import org.matsim.contrib.ev.withinday.ChargingSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class ReservationAlternativeProvider extends OrderedAlternativeProvider {
	@Inject
	ChargingInfrastructure infrastructure;

	//	@Inject
//	ChargerReservationManager manager;
	@Inject
	DistributedChargerReservationManager manager;

	private final AtomicInteger counter = new AtomicInteger();
	private final Map<Id<ChargingAlternative>, PendingAlternative> pendingAlternatives = new HashMap<>();

	@SuppressWarnings("null")
	@Override
	@Nullable
	public ChargingAlternative findEnrouteAlternative(double now, Person person, Plan plan,
	                                                  ElectricVehicle vehicle, @Nullable ChargingSlot initialSlot) {
		if (person.getId().toString().equals("person2")) {
			//manager.addReservation(initialSlot.charger().getSpecification(), vehicle, now, Double.POSITIVE_INFINITY);

			var id = Id.create(counter.incrementAndGet(), ChargingAlternative.class);
			var reservation = manager.addReservation(initialSlot.charger(), vehicle, now, Double.POSITIVE_INFINITY);
			if (reservation.isPresent() && reservation.get().status() == ReservationManager.ReservationStatus.PENDING) {
				pendingAlternatives.put(id, new PendingAlternative(reservation.get().reservationId(), initialSlot.charger().getId(), initialSlot.duration()));
				return new ChargingAlternative(id, initialSlot.charger().getId(), initialSlot.duration(), ChargingAlternative.RequestStatus.PENDING);
			} else {
				return new ChargingAlternative(id, initialSlot.charger().getId(), initialSlot.duration(), ChargingAlternative.RequestStatus.ACCEPTED);
			}
		}

		return null;
	}

	@Override
	@Nullable
	public ChargingAlternative queryPendingAlternative(Id<ChargingAlternative> id) {
		var pendingAlternative = pendingAlternatives.get(id);
		var reservation = manager.queryPendingReservation(pendingAlternative.reservationId());
		if (reservation.isPresent()) {
			var info = reservation.get();
			var state = info.status() == ReservationManager.ReservationStatus.PENDING ? ChargingAlternative.RequestStatus.PENDING : ChargingAlternative.RequestStatus.ACCEPTED;
			pendingAlternatives.remove(id);
			return new ChargingAlternative(id, pendingAlternative.chargerId(), pendingAlternative.duration(), state);
		} else {
			return null;
		}
	}

	record PendingAlternative(Id<ReservationManager.Reservation> reservationId, Id<Charger> chargerId, double duration) {}
}
