package org.matsim.contrib.common.util.reservation;

import org.matsim.api.core.v01.Identifiable;

/**
 * A generic interface for managing reservations of limited-capacity resources.
 *
 * @param <R> Resource type (must be identifiable, e.g. ChargerSpecification)
 * @param <Id> Id type of the resource (e.g. Charger in case of ChargerSpecification)
 * @param <C> Consumer type (the entity making the reservation, e.g. Vehicle)
 */
public interface ReservationManager<
        R extends Identifiable<Id>, Id, C> {

    /**
     * A reservation associates a consumer with a resource for a time interval.
     */
    record Reservation<R extends Identifiable<Id>, Id, C>(
            R resource,
            C consumer,
            double startTime,
            double endTime
    ) {}

    /**
     * Check if a reservation is possible for the given consumer and resource
     * in the specified time window.
     */
    boolean isAvailable(R resource, C consumer, double startTime, double endTime);

    /**
     * Add a reservation for the given consumer and resource in the specified time window.
     * Returns the created reservation if successful, otherwise null.
     */
    Reservation<R, Id, C> addReservation(R resource, C consumer, double startTime, double endTime);

    /**
     * Remove a previously added reservation.
     */
    boolean removeReservation(Reservation<R, Id, C> reservation);

    /**
     * Find a reservation for the given consumer at the given resource at a specific time.
     */
    Reservation<R, Id, C> findReservation(R resource, C consumer, double now);
}
