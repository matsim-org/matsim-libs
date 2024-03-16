package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.joining.DataFrameJoiner;
import tech.tablesaw.selection.Selection;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import static tech.tablesaw.aggregate.AggregateFunctions.count;

/**
 * Helper class to analyze trips by groups.
 * This class can not be used on its own, but will be called by {@link TripAnalysis}.
 */
final class TripByGroupAnalysis {

	private final static Logger log = LogManager.getLogger(TripByGroupAnalysis.class);

	/**
	 * Contains detected groups and their reference data.
	 */
	private final List<Group> groups;

	private final Map<String, Category> categories;

	TripByGroupAnalysis(String refData) throws IOException {

		try (BufferedReader reader = IOUtils.getBufferedReader(refData)) {
			Table ref = Table.read().csv(CsvReadOptions.builder(reader)
				.columnTypes((column) -> column.equals("share") ? ColumnType.DOUBLE : ColumnType.STRING)
				.sample(false)
				.build());

			List<String> columns = new ArrayList<>(ref.columnNames());
			// remove non group columns
			columns.removeAll(Set.of("dist_group", "main_mode", "share"));

			// Collect all contained groups
			Set<List<String>> groups = new HashSet<>();
			for (Row row : ref) {

				List<String> g = new ArrayList<>();
				for (String c : columns) {
					if (!row.getString(c).isEmpty())
						g.add(c);
				}
				if (!g.isEmpty())
					groups.add(g);
			}

			log.info("Detect groups: {}", groups);

			this.groups = new ArrayList<>();

			for (List<String> g : groups) {

				Selection sel = Selection.withRange(0, ref.rowCount());
				for (String c : g) {
					sel = sel.and(ref.stringColumn(c).isNotEqualTo(""));
				}

				Table gRef = ref.where(sel);
				this.groups.add(new Group(g, gRef));
			}

			this.categories = new HashMap<>();
			for (List<String> group : groups) {
				for (String g : group) {
					if (!this.categories.containsKey(g)) {
						this.categories.put(g, new Category(ref.column(g)));
					}
				}
			}

		}
	}

	void analyzeModeShare(Table trips, List<String> dists) {

		for (Group group : groups) {

			List<String> columns = new ArrayList<>(List.of("dist_group", "main_mode"));
			columns.addAll(group.columns);

			String[] join = columns.toArray(new String[0]);

			Table aggr = trips.summarize("trip_id", count).by(join);

			int idx = aggr.columnCount() - 1;
			DoubleColumn share = aggr.numberColumn(idx).divide(aggr.numberColumn(idx).sum()).setName("sim_share");
			aggr.replaceColumn(idx, share);

			// Sort by dist_group and mode
			Comparator<Row> cmp = Comparator.comparingInt(row -> dists.indexOf(row.getString("dist_group")));
			aggr = aggr.sortOn(cmp.thenComparing(row -> row.getString("main_mode")));

			// TODO: norm by category and dist_group
			// probably need two separate files as well (with and without dist)
			// not normed is more useful for now

			Table joined = new DataFrameJoiner(group.data, join).leftOuter(aggr);
			joined.column("share").setName("ref_share");

			joined.removeColumns(
				joined.columnNames().stream()
					.filter(c -> !columns.contains(c) && !c.equals("sim_share") && !c.equals("ref_share"))
					.toArray(String[]::new)
			);

			// TODO: write trip analysis, obtain output path from TripAnalysis
//			aggr.write().csv(output.getPath("mode_share_per_dist.csv").toFile());

		}
	}

	void groupPersons(Table persons) {

		for (Group g : groups) {
			for (String c : g.columns) {

				if (!persons.columnNames().contains(c)) {
					log.error("Column {} not found in persons table", c);
					persons.addColumns(StringColumn.create(c, persons.rowCount()));
					continue;
				}

				Column<?> column = persons.column(c);

				StringColumn copy = column.emptyCopy(column.size()).asStringColumn().setName(c);
				column.mapInto((Object value) -> categories.get(c).categorize(value), copy);
				persons.replaceColumn(c, copy);
			}
		}
	}

	private record Group(List<String> columns, Table data) {
	}

	private static final class Category {

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

		public Category(Column<?> data) {
			this.values = data.asStringColumn().unique()
				.removeMissing()
				.asSet();

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
		}

		/**
		 * Categorize a single value.
		 */
		public String categorize(Object value) {

			if (value == null)
				return null;

			// TODO: handle booleans

			if (value instanceof Number) {
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
			// TODO: int and float could be represented differently
			String v = value.toString();
			if (values.contains(v))
				return v;
			else if (grouped.containsKey(v))
				return grouped.get(v);

			return null;
		}

	}

	/**
	 * @param left  Left bound of the range.
	 * @param right Right bound of the range. (exclusive)
	 * @param label Label of this group.
	 */
	private record Range(double left, double right, String label) {


	}

}
