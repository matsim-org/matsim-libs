package org.matsim.application.analysis.population;

import java.util.*;

/**
 * Categorize values into groups.
 */
public final class Category {

	private static final Set<String> TRUE = Set.of("true", "yes", "1", "on", "y", "j", "ja");
	private static final Set<String> FALSE = Set.of("false", "no", "0", "off", "n", "nein");

	/**
	 * Unique values of the category.
	 */
	private final Set<String> values;

	/**
	 * Groups of values that have been subsumed under a single category.
	 * These are values separated by ,
	 */
	private final Map<String, String> grouped;

	/**
	 * Range categories.
	 */
	private final List<Range> ranges;

	public Category(Set<String> values) {
		this.values = values;
		this.grouped = new HashMap<>();
		for (String v : values) {
			if (v.contains(",")) {
				String[] grouped = v.split(",");
				for (String g : grouped) {
					this.grouped.put(g, v);
				}
			}
		}

		boolean range = this.values.stream().allMatch(v -> v.contains("-") || v.contains("+"));
		if (range) {
			ranges = new ArrayList<>();
			for (String value : this.values) {
				if (value.contains("-")) {
					String[] parts = value.split("-");
					ranges.add(new Range(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), value));
				} else if (value.contains("+")) {
					ranges.add(new Range(Double.parseDouble(value.replace("+", "")), Double.POSITIVE_INFINITY, value));
				}
			}

			ranges.sort(Comparator.comparingDouble(r -> r.left));
		} else
			ranges = null;


		// Check if all values are boolean
		if (values.stream().allMatch(v -> TRUE.contains(v.toLowerCase()) || FALSE.contains(v.toLowerCase()))) {
			for (String value : values) {
				Set<String> group = TRUE.contains(value.toLowerCase()) ? TRUE : FALSE;
				for (String g : group) {
					this.grouped.put(g, value);
				}
			}
		}
	}

	/**
	 * Categorize a single value.
	 */
	public String categorize(Object value) {

		if (value == null)
			return null;

		if (value instanceof Boolean) {
			// Booleans and synonyms are in the group map
			return categorize(((Boolean) value).toString().toLowerCase());
		} else if (value instanceof Number) {
			return categorizeNumber((Number) value);
		} else {
			String v = value.toString();
			if (values.contains(v))
				return v;
			else if (grouped.containsKey(v))
				return grouped.get(v);

			try {
				double d = Double.parseDouble(v);
				return categorizeNumber(d);
			} catch (NumberFormatException e) {
				return null;
			}
		}
	}

	private String categorizeNumber(Number value) {

		if (ranges != null) {
			for (Range r : ranges) {
				if (value.doubleValue() >= r.left && value.doubleValue() < r.right)
					return r.label;
			}
		}

		// Match string representation
		String v = value.toString();
		if (values.contains(v))
			return v;
		else if (grouped.containsKey(v))
			return grouped.get(v);


		// Convert the number to a whole number, which will have a different string representation
		if (value instanceof Float || value instanceof Double) {
			return categorizeNumber(value.longValue());
		}

		return null;
	}

	/**
	 * @param left  Left bound of the range.
	 * @param right Right bound of the range. (exclusive)
	 * @param label Label of this group.
	 */
	private record Range(double left, double right, String label) {


	}

}
