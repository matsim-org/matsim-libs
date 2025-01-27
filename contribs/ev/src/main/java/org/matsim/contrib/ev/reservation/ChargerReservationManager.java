package org.matsim.contrib.ev.reservation;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.IdMap;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

/**
 * This class is a singleton service that keeps a list of reservations for
 * chargers. It can be used in combination with the
 * ReservationBasedChargingPriority to let vehicle pass on to charging only if
 * they have a proper reservation.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ChargerReservationManager implements IterationStartsListener {
	private final IdMap<Charger, List<Reservation>> reservations = new IdMap<>(Charger.class);

	public boolean isAvailable(ChargerSpecification charger, ElectricVehicle vehicle, double startTile,
			double endTime) {
		if (charger.getPlugCount() == 0) {
			return false;
		}

		if (!reservations.containsKey(charger.getId())) {
			return true;
		}

		int remaining = charger.getPlugCount();
		for (Reservation reservation : reservations.get(charger.getId())) {
			if (reservation.vehicle != vehicle && isOverlapping(reservation, startTile, endTime)) {
				remaining--;
			}
		}

		return remaining > 0;
	}

	private boolean isOverlapping(Reservation reservation, double startTime, double endTime) {
		if (startTime >= reservation.startTime && startTime <= reservation.endTime) {
			return true; // start time within existing range
		} else if (endTime >= reservation.startTime && endTime <= reservation.endTime) {
			return true; // end time within existing range
		} else if (startTime <= reservation.startTime && endTime >= reservation.endTime) {
			return true; // new range covers existing range
		} else {
			return false;
		}
	}

	public Reservation addReservation(ChargerSpecification charger, ElectricVehicle vehicle, double startTime,
			double endTime) {
		if (isAvailable(charger, vehicle, startTime, endTime)) {
			List<Reservation> chargerReservations = reservations.get(charger.getId());

			if (chargerReservations == null) {
				chargerReservations = new LinkedList<>();
				reservations.put(charger.getId(), chargerReservations);
			}

			Reservation reservation = new Reservation(charger, vehicle, startTime, endTime);
			chargerReservations.add(reservation);

			return reservation;
		}

		return null;
	}

	public boolean removeReservation(Reservation reservation) {
		List<Reservation> chargerReservations = reservations.get(reservation.charger.getId());

		if (chargerReservations != null) {
			return chargerReservations.remove(reservation);
		}

		return false;
	}

	public Reservation findReservation(ChargerSpecification charger, ElectricVehicle vehicle, double now) {
		List<Reservation> chargerReservations = reservations.get(charger.getId());

		if (chargerReservations != null) {
			for (Reservation reservation : chargerReservations) {
				if (reservation.vehicle == vehicle && now >= reservation.startTime && now <= reservation.endTime) {
					return reservation;
				}
			}
		}

		return null;
	}

	public record Reservation(ChargerSpecification charger, ElectricVehicle vehicle, double startTime, double endTime) {
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		reservations.clear();
	}
}