package org.matsim.contrib.ev.reservation;

import org.matsim.contrib.ev.charging.ChargingLogic.ChargingVehicle;
import org.matsim.contrib.ev.charging.ChargingPriority;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.reservation.ChargerReservationManager.Reservation;

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
    private final ChargerReservationManager manager;
    private final ChargerSpecification charger;

    public ReservationBasedChargingPriority(ChargerReservationManager manager, ChargerSpecification charger) {
        this.charger = charger;
        this.manager = manager;
    }

    @Override
    public boolean requestPlugNext(ChargingVehicle cv, double now) {
        Reservation reservation = manager.findReservation(charger, cv.ev(), now);

        if (reservation != null) {
            // vehicle has a reservation, can be plugged right away, consume reservation
            manager.removeReservation(reservation);
            return true;
        }

        double endTime = cv.strategy().calcRemainingTimeToCharge() + now;
        reservation = manager.addReservation(charger, cv.ev(), now, endTime);

        if (reservation != null) {
            // vehicle did not have a reservation, but managed to create one on the fly,
            // consume it directly
            manager.removeReservation(reservation);
            return true;
        }

        return false;
    }

    static public class Factory implements ChargingPriority.Factory {
        private final ChargerReservationManager reservationManager;

        public Factory(ChargerReservationManager reservationManager) {
            this.reservationManager = reservationManager;
        }

        @Override
        public ChargingPriority create(ChargerSpecification charger) {
            return new ReservationBasedChargingPriority(reservationManager, charger);
        }
    }
}