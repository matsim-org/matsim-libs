package org.matsim.contrib.common.util.reservation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.controler.listener.IterationStartsListener;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 * @author nkuehnel refactored to abstract generic version
 */
public abstract class AbstractReservationManager<
        R extends Identifiable<?>,               // Resource type (must be identifiable, e.g. ChargerSpecification)
        C                                       // Consumer type (vehicle, person, etc.),
        > implements ReservationManager<R, C> {

    private final Map<Id<?>, IdMap<Reservation, ReservationInfo<R, C>>> reservations;
    private int counter = 0;


    public AbstractReservationManager() {
        this.reservations = new HashMap<>();
    }

    protected abstract int getCapacity(R resource);

    @Override
    public final boolean isAvailable(R resource, C consumer, double startTime, double endTime) {
        int capacity = getCapacity(resource);
        if (capacity == 0) {
            return false;
        }

        if (!reservations.containsKey(resource.getId())) {
            return true;
        }

        // Use a Set to track unique consumers with overlapping reservations
        Set<C> consumersWithOverlappingReservations = new HashSet<>();

        for (ReservationInfo<R, C> reservationInfo : reservations.get(resource.getId()).values()) {
            if (!reservationInfo.consumer().equals(consumer) && 
                isOverlapping(reservationInfo, startTime, endTime)) {
                
                consumersWithOverlappingReservations.add(reservationInfo.consumer());
            }
        }

        // Return true if the number of unique consumers with overlapping reservations is less than capacity
        return consumersWithOverlappingReservations.size() < capacity;
    }

    @Override
    public final Optional<ReservationInfo<R, C>> addReservation(R resource, C consumer, double startTime, double endTime) {
        if (isAvailable(resource, consumer, startTime, endTime)) {
            IdMap<Reservation, ReservationInfo<R, C>> resourceReservations = reservations.get(resource.getId());

            if (resourceReservations == null) {
                resourceReservations = new IdMap<>(Reservation.class);
                reservations.put(resource.getId(), resourceReservations);
            }

            Id<Reservation> reservationId = Id.create(resource.getId() + "_" + counter++, Reservation.class);
            ReservationInfo<R, C> reservationInfo = new ReservationInfo<>(reservationId, resource, consumer, startTime, endTime);
            resourceReservations.put(reservationId, reservationInfo);

            return Optional.of(reservationInfo);
        }

        return Optional.empty();
    }

    @Override
    public final boolean removeReservation(Id<?> resourceId, Id<Reservation> reservationId) {
        IdMap<Reservation, ReservationInfo<R, C>> resourceReservations = reservations.get(resourceId);

        if (resourceReservations != null) {
            return resourceReservations.remove(reservationId) != null;
        }

        return false;
    }

    @Override
    public final Optional<ReservationInfo<R, C>> findReservation(R resource, C consumer, double now) {
        IdMap<Reservation, ReservationInfo<R, C>> resourceReservations = reservations.get(resource.getId());

        if (resourceReservations != null) {
            for (ReservationInfo<R, C> reservationInfo : resourceReservations.values()) {
                if (reservationInfo.consumer().equals(consumer) && now >= reservationInfo.startTime() && now <= reservationInfo.endTime()) {
                    return Optional.of(reservationInfo);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Try to update a reservation. Returns true if successful, false otherwise.
     */
    @Override
    public boolean updateReservation(Id<?> resourceId, Id<Reservation> reservationId, double newStartTime, double newEndTime) {
        IdMap<Reservation, ReservationInfo<R, C>> resourceReservations = reservations.get(resourceId);

        if (resourceReservations == null) {
            return false; // nothing to update
        }

        // remove old reservation
        ReservationInfo<R, C> removed = resourceReservations.remove(reservationId);
        if (removed == null) {
            return false; // reservation not found
        }

        // check availability for new interval
        if (isAvailable(removed.resource(), removed.consumer(), newStartTime, newEndTime)) {
            ReservationInfo<R, C> updated =
                    new ReservationInfo<>(removed.reservationId(), removed.resource(), removed.consumer(),
                            newStartTime, newEndTime);
            resourceReservations.put(removed.reservationId(), updated);
            return true;
        } else {
            // put the old reservation back since update failed
            resourceReservations.put(reservationId, removed);
            return false;
        }
    }

    /**
     * Find a reservation by id.
     */
    @Override
    public Optional<ReservationInfo<R, C>> findReservation(Id<?> resourceId, Id<Reservation> reservationId) {
        IdMap<Reservation, ReservationInfo<R, C>> reservationInfos = reservations.get(resourceId);
        if(reservationInfos == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(reservationInfos.getOrDefault(reservationId, null));
    }


    private boolean isOverlapping(ReservationInfo<R, C> reservationInfo, double startTime, double endTime) {
        if (startTime >= reservationInfo.startTime() && startTime < reservationInfo.endTime()) {
            return true; // start time within existing range
        } else if (endTime > reservationInfo.startTime() && endTime < reservationInfo.endTime()) {
            return true; // end time within existing range
        } else if (startTime <= reservationInfo.startTime() && endTime > reservationInfo.endTime()) {
            return true; // new range covers existing range
        } else {
            return false;
        }
    }

    protected void cleanReservations() {
        reservations.clear();
        counter = 0;
    }
    
    /**
     * Returns all reservations for a given resource.
     * This method is primarily for testing and debugging purposes.
     */
    protected Collection<ReservationInfo<R, C>> getReservations(R resource) {
        IdMap<Reservation, ReservationInfo<R, C>> resourceReservations = reservations.get(resource.getId());
        if (resourceReservations == null) {
            return Collections.emptyList();
        }
        return resourceReservations.values();
    }
}
