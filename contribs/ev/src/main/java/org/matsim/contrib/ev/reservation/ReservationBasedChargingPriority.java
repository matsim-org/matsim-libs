package org.matsim.contrib.ev.reservation;

import com.google.inject.Inject;
import org.matsim.contrib.ev.charging.ChargingLogic.ChargingVehicle;
import org.matsim.contrib.ev.charging.ChargingPriority;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;

/**
 * This is an implementation of a charging priority which is backed by the
 * ReservationManager: When a vehicle arrives, it is checked whether a
 * reservation for this vehicle exists for the respective charger and the
 * current time. In that case, the vehicle can be plugged if it is on top of the
 * queue. If not, the ChargingPriority will try to make a reservation and if it
 * is successful, the vehicle can start charging. In all other cases, the
 * vehicle will stay in the queue until it is removed manually or a successful
 * reservation can be made at a future time.
 *
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ReservationBasedChargingPriority implements ChargingPriority {

	private final DistributedChargerReservationManager manager;
	private final ChargerSpecification charger;

	public ReservationBasedChargingPriority(DistributedChargerReservationManager manager, ChargerSpecification charger) {
		this.charger = charger;
		this.manager = manager;
	}

	@Override
	public boolean requestPlugNext(ChargingVehicle cv, double now) {

		var reservation = manager.findReservation(charger.getId(), cv.ev().getId(), now);

		if (reservation.isPresent()) {
			// vehicle has a reservation, can be plugged right away, consume reservation
			manager.removeReservation(reservation.get().reservationId(), reservation.get().resource());
			return true;
		}

		double endTime = cv.strategy().calcRemainingTimeToCharge() + now;
		var onTheFlyReservation = manager.addLocalReservation(charger.getId(), cv.ev().getId(), now, endTime);

		if (onTheFlyReservation.isPresent()) {
			// vehicle did not have a reservation, but managed to create one on the fly,
			// consume it directly
			manager.removeReservation(onTheFlyReservation.get().reservationId(), onTheFlyReservation.get().resource());
			return true;
		}

		return false;
	}

	static public class Factory implements ChargingPriority.Factory {
		private final DistributedChargerReservationManager reservationManager;

		@Inject
		public Factory(DistributedChargerReservationManager reservationManager) {
			this.reservationManager = reservationManager;
		}

		@Override
		public ChargingPriority create(ChargerSpecification charger) {
			return new ReservationBasedChargingPriority(reservationManager, charger);
		}
	}
}
