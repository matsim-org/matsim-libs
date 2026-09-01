package org.matsim.application.prepare.counts;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * One aggregation of hourly count data: the days of week its values are taken from, and the date ranges those days
 * have to fall into. An empty range list accepts every date the input covers.
 */
public record Aggregation(String name, Set<DayOfWeek> weekdays, List<DateRange> ranges) {

	/**
	 * An aggregation name is used as an output directory name, so it must not contain a path separator.
	 */
	private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]+");

	/**
	 * Parses aggregation definitions of the form {@code <name>=<DAY>[,<DAY>...][@<from>:<to>[+<from>:<to>...]]}, e.g.
	 * {@code saturday=SATURDAY@2022-03-01:2022-05-31+2022-09-01:2022-10-31}. Dates are inclusive and given as
	 * yyyy-MM-dd, a day is used if it falls into any of the ranges. Without ranges the whole input period is used.
	 */
	public static List<Aggregation> parse(List<String> specs) {

		List<Aggregation> aggregations = new ArrayList<>();
		Set<String> names = new HashSet<>();

		for (String spec : specs) {

			int equals = spec.indexOf('=');
			if (equals < 0)
				throw new IllegalArgumentException("Aggregation '" + spec + "' has no '=', expected <name>=<DAY>[,<DAY>...][@<from>:<to>[+<from>:<to>...]]");

			String name = spec.substring(0, equals).trim();
			if (name.isEmpty())
				throw new IllegalArgumentException("Aggregation '" + spec + "' has an empty name.");

			//The name becomes a directory name, so it must not carry a path
			if (!NAME.matcher(name).matches())
				throw new IllegalArgumentException("Aggregation name '" + name + "' is not a plain name of letters, digits, '-' and '_', but it is used as an output directory name.");

			if (!names.add(name))
				throw new IllegalArgumentException("Aggregation name '" + name + "' is used more than once, but every aggregation needs its own output directory.");

			//Everything up to the '@' are the days of week, everything behind it the date ranges
			String definition = spec.substring(equals + 1);
			int at = definition.indexOf('@');

			Set<DayOfWeek> weekdays = parseWeekdays(at < 0 ? definition : definition.substring(0, at), spec);
			List<DateRange> ranges = at < 0 ? List.of() : parseDateRanges(definition.substring(at + 1), spec);

			aggregations.add(new Aggregation(name, weekdays, ranges));
		}

		return aggregations;
	}

	private static Set<DayOfWeek> parseWeekdays(String weekdays, String spec) {

		Set<DayOfWeek> parsed = EnumSet.noneOf(DayOfWeek.class);

		for (String day : weekdays.split(",")) {
			day = day.trim();
			if (day.isEmpty())
				continue;

			try {
				parsed.add(DayOfWeek.valueOf(day.toUpperCase(Locale.ROOT)));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Aggregation '" + spec + "' names the unknown day of week '" + day
					+ "', expected one of " + Arrays.toString(DayOfWeek.values()));
			}
		}

		if (parsed.isEmpty())
			throw new IllegalArgumentException("Aggregation '" + spec + "' does not name any day of week.");

		return parsed;
	}

	private static List<DateRange> parseDateRanges(String ranges, String spec) {

		List<DateRange> parsed = new ArrayList<>();

		for (String range : ranges.split("\\+")) {
			String[] bounds = range.trim().split(":");
			if (bounds.length != 2)
				throw new IllegalArgumentException("Aggregation '" + spec + "' has the malformed date range '" + range + "', expected <from>:<to> as yyyy-MM-dd:yyyy-MM-dd");

			LocalDate from;
			LocalDate to;
			try {
				from = LocalDate.parse(bounds[0].trim());
				to = LocalDate.parse(bounds[1].trim());
			} catch (DateTimeParseException e) {
				throw new IllegalArgumentException("Aggregation '" + spec + "' has the unparsable date range '" + range + "', expected yyyy-MM-dd:yyyy-MM-dd", e);
			}

			if (from.isAfter(to))
				throw new IllegalArgumentException("Aggregation '" + spec + "' has the date range '" + range + "', which starts after it ends.");

			parsed.add(new DateRange(from, to));
		}

		if (parsed.isEmpty())
			throw new IllegalArgumentException("Aggregation '" + spec + "' has a '@' but no date range behind it.");

		return parsed;
	}

	/**
	 * Whether the counts of that date are part of this aggregation.
	 */
	public boolean covers(LocalDate date) {

		if (!weekdays.contains(date.getDayOfWeek()))
			return false;

		if (ranges.isEmpty())
			return true;

		return ranges.stream().anyMatch(range -> range.contains(date));
	}

	/**
	 * Human readable definition of the aggregation, used in the log and in the counts description.
	 */
	public String describe() {

		String days = weekdays.stream().sorted().map(Enum::name).collect(Collectors.joining(", "));

		if (ranges.isEmpty())
			return days;

		return days + " within " + ranges.stream().map(DateRange::toString).collect(Collectors.joining(", "));
	}

	/**
	 * Date range, inclusive on both ends.
	 */
	public record DateRange(LocalDate from, LocalDate to) {

		public boolean contains(LocalDate date) {
			return !date.isBefore(from) && !date.isAfter(to);
		}

		@Override
		public String toString() {
			return from + ":" + to;
		}
	}
}
