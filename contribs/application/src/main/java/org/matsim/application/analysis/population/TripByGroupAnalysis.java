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
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

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

			log.info("Detected groups: {}", groups);

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
						this.categories.put(g, new Category(ref.column(g).asStringColumn().removeMissing().asSet()));
					}
				}
			}

			// Norm shares per instance of each group to sum of 1
			for (Group group : this.groups) {

				String norm = group.columns.get(0);
				if (group.columns.size() > 1)
					throw new UnsupportedOperationException("Multiple columns not supported yet");

				Table df = group.data;
				for (String label : df.stringColumn(norm).asSet()) {
					DoubleColumn dist_group = df.doubleColumn("share");
					Selection sel = df.stringColumn(norm).isEqualTo(label);
					double total = dist_group.where(sel).sum();
					if (total > 0)
						dist_group.set(sel, dist_group.divide(total));
				}
			}
		}
	}

	void writeModeShare(Table trips, List<String> dists, List<String> modeOrder, Function<String, Path> output) {

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
			aggr = aggr.sortOn(cmp.thenComparingInt(row -> modeOrder.indexOf(row.getString("main_mode"))));

			// Norm each group to 1
			String norm = group.columns.get(0);
			if (group.columns.size() > 1)
				throw new UnsupportedOperationException("Multiple columns not supported yet");

			for (String label : aggr.stringColumn(norm).asSet()) {
				DoubleColumn dist_group = aggr.doubleColumn("sim_share");
				Selection sel = aggr.stringColumn(norm).isEqualTo(label);

				double total = dist_group.where(sel).sum();
				if (total > 0)
					dist_group.set(sel, dist_group.divide(total));
			}

			Table joined = new DataFrameJoiner(group.data, join).leftOuter(aggr);
			joined.column("share").setName("ref_share");

			joined.removeColumns(
				joined.columnNames().stream()
					.filter(c -> !columns.contains(c) && !c.equals("sim_share") && !c.equals("ref_share"))
					.toArray(String[]::new)
			);

			String name = String.join("_", group.columns);
			joined.write().csv(output.apply(name).toFile());
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

}
