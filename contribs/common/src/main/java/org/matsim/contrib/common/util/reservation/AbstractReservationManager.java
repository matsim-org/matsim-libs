package org.matsim.contrib.common.util.reservation;

import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.controler.listener.IterationStartsListener;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 * @author nkuehnel refactored to abstract generic version
 */
public abstract class AbstractReservationManager<
        R extends Identifiable<Id>,               // Resource type (must be identifiable, e.g. ChargerSpecification)
        Id,                                     // Id type of the resource
        C                                       // Consumer type (vehicle, person, etc.),
        > implements ReservationManager<R, Id, C>, IterationStartsListener {

    private final IdMap<Id, List<Reservation<R, Id, C>>> reservations;

    public AbstractReservationManager(Class<Id> idClass) {
        this.reservations = new IdMap<>(idClass);
    }

    public abstract int getCapacity(R resource);

    @Override
    public boolean isAvailable(R resource, C consumer, double startTime, double endTime) {
        int capacity = getCapacity(resource);
        if (capacity == 0) {
            return false;
        }

        if (!reservations.containsKey(resource.getId())) {
            return true;
        }

        int remaining = capacity;
        for (Reservation<R, Id, C> reservation : reservations.get(resource.getId())) {
            if (reservation.consumer() != consumer && isOverlapping(reservation, startTime, endTime)) {
                remaining--;
            }
        }

        return remaining > 0;
    }

    @Override
    public final Reservation<R, Id, C> addReservation(R resource, C consumer, double startTime, double endTime) {
        if (isAvailable(resource, consumer, startTime, endTime)) {
            List<Reservation<R, Id, C>> resourceReservations = reservations.get(resource.getId());

            if (resourceReservations == null) {
                resourceReservations = new LinkedList<>();
                reservations.put(resource.getId(), resourceReservations);
            }

            Reservation<R, Id, C> reservation = new Reservation<>(resource, consumer, startTime, endTime);
            resourceReservations.add(reservation);

            return reservation;
        }

        return null;
    }

    @Override
    public final boolean removeReservation(Reservation<R, Id, C> reservation) {
        List<Reservation<R, Id, C>> resourceReservation = reservations.get(reservation.resource().getId());

        if (resourceReservation != null) {
            return resourceReservation.remove(reservation);
        }

        return false;
    }

    @Override
    public final Reservation<R, Id, C> findReservation(R resource, C consumer, double now) {
        List<Reservation<R, Id, C>> resourceReservations = reservations.get(resource.getId());

        if (resourceReservations != null) {
            for (Reservation<R, Id, C> reservation : resourceReservations) {
                if (reservation.consumer() == consumer && now >= reservation.startTime() && now <= reservation.endTime()) {
                    return reservation;
                }
            }
        }

        return null;
    }

    private boolean isOverlapping(Reservation<R, Id, C> reservation, double startTime, double endTime) {
        if (startTime >= reservation.startTime() && startTime <= reservation.endTime()) {
            return true; // start time within existing range
        } else if (endTime >= reservation.startTime() && endTime <= reservation.endTime()) {
            return true; // end time within existing range
        } else if (startTime <= reservation.startTime() && endTime >= reservation.endTime()) {
            return true; // new range covers existing range
        } else {
            return false;
        }
    }

    protected void cleanReservations() {
        reservations.clear();
    }
}
