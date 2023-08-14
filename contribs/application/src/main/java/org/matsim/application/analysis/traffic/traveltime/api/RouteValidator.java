package org.matsim.application.analysis.traffic.traveltime.api;

import org.matsim.api.core.v01.Coord;

import java.time.*;
import java.time.temporal.TemporalAdjusters;

/**
 * Interface to calculate route information.
 */
public interface RouteValidator extends AutoCloseable {

	/**
	 * Create departure time for a week day in the future.
	 */
	static ZonedDateTime createDateTime(int hour) {
		LocalDate date = LocalDate.now(ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
		return date.atStartOfDay(ZoneId.systemDefault()).plusHours(hour).withZoneSameInstant(ZoneOffset.UTC);
	}

	/**
	 * Create departure time for a week day in the future.
	 */
	static LocalDateTime createLocalDateTime(int hour) {
		LocalDate date = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
		return date.atStartOfDay().plusHours(hour);
	}

	/**
	 * Return the name of the validator.
	 */
	String name();

	/**
	 * Calculate route information between two coordinates. Coordinates are always in WGS84.
	 */
	Result retrieve(Coord from, Coord to, int hour);

	/**
	 * Result for one query.
	 * @param travelTime travel time in seconds
	 * @param dist distance of route in meter
	 */
	record Result(int hour, int travelTime, int dist) {
	}

	/**
	 * Exception that can be thrown, if an API key is not valid and processing should stop immediately.
	 */
	final class Forbidden extends RuntimeException {}

}
