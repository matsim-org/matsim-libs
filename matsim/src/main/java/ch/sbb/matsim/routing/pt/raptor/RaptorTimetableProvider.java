package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.population.Person;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * Chooses the timetable that {@link SwissRailRaptor} routes on, once per request. The default
 * ({@link DefaultRaptorTimetableProvider}) returns the times of the transit schedule. With
 * {@code useTimetableUpdates}, an implementation may return a {@link VersionedRaptorTimetable} instead, e.g. the
 * version that the routing attributes name.
 * <p>
 * It is called from many threads at the same time, so implementations must be thread-safe. They should not block:
 * a caller that has to wait for a version waits before it routes.
 */
@FunctionalInterface
public interface RaptorTimetableProvider {

	/**
	 * @param person            may be null
	 * @param routingAttributes may be null
	 * @return a timetable that belongs to {@code data} (see {@link RaptorTimetable#belongsTo}); otherwise the search
	 * throws an {@link IllegalArgumentException}
	 */
	RaptorTimetable getTimetable(SwissRailRaptorData data, double departureTime, Person person, Attributes routingAttributes);
}
