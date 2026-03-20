package org.matsim.contrib.common.util.reservation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

import java.util.Optional;

/**
 * A generic interface for managing reservations of limited-capacity resources.
 *
 * @param <R> Resource type (must be identifiable, e.g. ChargerSpecification)
 * @param <C> Consumer type (the entity making the reservation, e.g. Vehicle)
 */
public interface ReservationManager<R extends Identifiable<?>, C> {

    // marker type just to namespace reservation IDs
    interface Reservation {}

    /**
     * A reservation associates a consumer with a resource for a time interval.
     */
    record ReservationInfo<R extends Identifiable<?>, C>(
            Id<Reservation> reservationId,
            R resource,
            C consumer,
            double startTime,
            double endTime
    ){}

    /**
     * Check if a reservation is possible for the given consumer and resource
     * in the specified time window.
     */
    boolean isAvailable(R resource, C consumer, double startTime, double endTime);

    /**
     * Add a reservation for the given consumer and resource in the specified time window.
     * Returns the created reservation if successful, otherwise null.
     */
    Optional<ReservationInfo<R, C>> addReservation(R resource, C consumer, double startTime, double endTime);

    /**
     * Remove a previously added reservation.
     */
    boolean removeReservation(Id<?> resourceId, Id<Reservation> reservationId);

    /**
     * Try to update a reservation. Returns true if successful, false otherwise. If the reservation existed it will
     * remain unchanged if the update failed.
     */
    boolean updateReservation(Id<?> resourceId, Id<Reservation> reservationId, double newStartTime, double newEndTime);

    /**
     * Find a reservation for the given consumer at the given resource at a specific time.
     */
    Optional<ReservationInfo<R, C>> findReservation(R resource, C consumer, double now);

    /**
     * Find a reservation by id.
     */
    Optional<ReservationInfo<R, C>> findReservation(Id<?> resourceId, Id<Reservation> reservationId);
}
