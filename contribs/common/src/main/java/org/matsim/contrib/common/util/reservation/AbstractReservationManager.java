package org.matsim.contrib.common.util.reservation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.common.util.TimeInterval;

import java.util.*;

/**
 *
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 * @author nkuehnel refactored to abstract generic version
 */
public abstract class AbstractReservationManager<
	R extends Identifiable<?>,               // Resource type (must be identifiable, e.g. ChargerSpecification)
	C extends Identifiable<?>                                      // Consumer type (vehicle, person, etc.),
	> implements ReservationManager<R, C> {

	private final Map<Id<?>, IdMap<Reservation, ReservationInfo<R, C>>> reservations;
	private final double bufferTime;
	private int counter = 0;


	public AbstractReservationManager() {
		this(0.);
	}

	public AbstractReservationManager(double bufferTime) {
		this.reservations = new HashMap<>();
		this.bufferTime = bufferTime;
	}

	protected abstract int getCapacity(R resource);

	@Override
	public boolean isAvailable(R resource, C consumer, double startTime, double endTime) {
		int capacity = getCapacity(resource);
		if (capacity == 0) {
			return false;
		}

		IdMap<Reservation, ReservationInfo<R, C>> resourceReservations = reservations.get(resource.getId());
		if (resourceReservations == null) {
			return true;
		}

		TimeInterval requested = new TimeInterval(startTime, endTime).expandBy(bufferTime);
		return getPeakOccupancy(resourceReservations.values(), consumer, requested) < capacity;
	}

	/**
	 * A consumer starts or stops occupying the resource at the given time. The kind also defines the processing order
	 * at equal times.
	 */
	private record OccupancyChange(double time, int kind, Id<?> consumerId) {
	}

	private static final int DEPARTURE = 0;
	private static final int ARRIVAL = 1;
	// departure of a zero-length occupancy: processed after the arrivals at the same time, so that it counts at its
	// instant
	private static final int DEPARTURE_OF_INSTANT = 2;

	/**
	 * Returns the highest number of other consumers that hold a reservation at the same time within the requested
	 * interval. Reservations of the requesting consumer are ignored, and several reservations of one consumer count
	 * once. The count is obtained by sweeping over the start and end points of the overlapping reservations, clipped to
	 * the requested interval. At equal times, departures are processed before arrivals, so reservations that only touch
	 * are not counted as simultaneous. A zero-length occupancy counts at its instant (see {@link TimeInterval}).
	 */
	private int getPeakOccupancy(Collection<ReservationInfo<R, C>> reservationInfos, C consumer,
								 TimeInterval requested) {
		List<OccupancyChange> changes = new ArrayList<>();
		for (ReservationInfo<R, C> reservationInfo : reservationInfos) {
			if (reservationInfo.consumer().getId().equals(consumer.getId())) {
				continue;
			}
			TimeInterval reserved = new TimeInterval(reservationInfo.startTime(), reservationInfo.endTime());
			if (requested.overlaps(reserved)) {
				double start = Math.max(reserved.start(), requested.start());
				double end = Math.min(reserved.end(), requested.end());
				Id<?> consumerId = reservationInfo.consumer().getId();
				changes.add(new OccupancyChange(start, ARRIVAL, consumerId));
				changes.add(new OccupancyChange(end, end > start ? DEPARTURE : DEPARTURE_OF_INSTANT, consumerId));
			}
		}
		changes.sort(Comparator.comparingDouble(OccupancyChange::time).thenComparingInt(OccupancyChange::kind));

		Map<Id<?>, Integer> activeReservations = new HashMap<>();
		int peak = 0;
		for (OccupancyChange change : changes) {
			if (change.kind() == ARRIVAL) {
				activeReservations.merge(change.consumerId(), 1, Integer::sum);
				peak = Math.max(peak, activeReservations.size());
			} else {
				activeReservations.computeIfPresent(change.consumerId(), (id, count) -> count == 1 ? null : count - 1);
			}
		}
		return peak;
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
			ReservationInfo<R, C> reservationInfo = new ReservationInfo<>(reservationId, resource, consumer, startTime, endTime, ReservationStatus.CONFIRMED);
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
				if (reservationInfo.consumer().getId().equals(consumer.getId()) && now >= reservationInfo.startTime() && now <= reservationInfo.endTime()) {
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
					newStartTime, newEndTime, ReservationStatus.CONFIRMED);
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
		if (reservationInfos == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(reservationInfos.getOrDefault(reservationId, null));
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
