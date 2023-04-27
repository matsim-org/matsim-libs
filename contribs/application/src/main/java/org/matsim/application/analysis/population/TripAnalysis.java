package org.matsim.application.analysis.population;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.joining.DataFrameJoiner;
import tech.tablesaw.selection.Selection;

import java.util.*;

import static tech.tablesaw.aggregate.AggregateFunctions.count;

@CommandLine.Command(name = "trips", description = "Calculates various trip related metrics.")
@CommandSpec(requires = {"trips.csv", "persons.csv"}, produces = {"mode_share.csv", "population_stats.csv", "trip_purposes_by_hour.csv"})
public class TripAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(TripAnalysis.class);
	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(TripAnalysis.class);

	@CommandLine.Option(names = "--match-id", description = "Pattern to filter agents by id")
	private String matchId;

	@CommandLine.Option(names = "--dist-groups", split = ",", description = "List of distances for binning", defaultValue = "0,1000,2000,5000,10000,20000")
	private List<Integer> distGroups;

	@CommandLine.Mixin
	private ShpOptions shp;

	public static void main(String[] args) {
		new TripAnalysis().execute(args);
	}

	private static String cut(int dist, List<Integer> distGroups, List<String> labels) {

		int idx = Collections.binarySearch(distGroups, dist);

		if (idx >= 0)
			return labels.get(idx);

		int ins = -(idx + 1);
		return labels.get(ins - 1);
	}

	private static int[] durationToHour(String d) {
		return Arrays.stream(d.split(":")).mapToInt(Integer::valueOf).toArray();
	}

	@Override
	public Integer call() throws Exception {

		Table persons = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(input.getPath("persons.csv")))
				.columnTypesPartial(Map.of("person", ColumnType.TEXT))
				.separator(';').build());


		if (matchId != null) {
			persons = persons.where(persons.textColumn("person").matchesRegex(matchId));
		}

		if (shp.isDefined()) {
			throw new UnsupportedOperationException("Shp filtering not implemented yet.");
		}

		Table trips = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(input.getPath("trips.csv")))
				.columnTypesPartial(Map.of("person", ColumnType.TEXT, "trav_time", ColumnType.STRING,
						"longest_distance_mode", ColumnType.STRING, "main_mode", ColumnType.STRING, "end_activity_type", ColumnType.TEXT))
				.separator(';').build());

		// Use longest_distance_mode where main_mode is not present
		trips.stringColumn("main_mode")
				.set(trips.stringColumn("main_mode").isMissing(),
						trips.stringColumn("longest_distance_mode"));


		Table joined = new DataFrameJoiner(trips, "person").inner(persons);

		List<String> labels = new ArrayList<>();
		for (int i = 0; i < distGroups.size() - 1; i++) {
			labels.add(String.format("%d - %d", distGroups.get(i), distGroups.get(i + 1)));
		}
		labels.add(distGroups.get(distGroups.size() - 1) + "+");
		distGroups.add(Integer.MAX_VALUE);

		StringColumn dist_group = joined.intColumn("traveled_distance")
				.map(dist -> cut(dist, distGroups, labels), ColumnType.STRING::create).setName("dist_group");

		joined.addColumns(dist_group);

		writeModeShare(joined);

		writePopulationStats(persons, joined);

		writeTripPurposes(joined);

		return 0;
	}

	private void writeModeShare(Table trips) {

		Table aggr = trips.summarize("trip_id", count).by("dist_group", "main_mode");

		DoubleColumn share = aggr.numberColumn(2).divide(aggr.numberColumn(2).sum()).setName("share");
		aggr.replaceColumn(2, share);

		aggr = aggr.sortOn(0, 1);
		aggr.write().csv(output.getPath("mode_share.csv").toFile());
	}

	private void writePopulationStats(Table persons, Table trips) {

		Object2IntMap<String> numTrips = new Object2IntOpenHashMap<>();
		Map<String, Set<String>> modesPerPerson = new HashMap<>();

		for (Row trip : trips) {
			String id = trip.getString("person");
			numTrips.mergeInt(id, 1, Integer::sum);
			String mode = trip.getString("main_mode");
			modesPerPerson.computeIfAbsent(id, s -> new HashSet<>()).add(mode);
		}

		Object2IntMap<String> usedModes = new Object2IntOpenHashMap<>();
		for (Map.Entry<String, Set<String>> e : modesPerPerson.entrySet()) {
			for (String mode : e.getValue()) {
				usedModes.mergeInt(mode, 1, Integer::sum);
			}
		}

		double totalMobile = numTrips.size();
		double avgTripsMobile = numTrips.values().intStream().average().orElse(0);

		for (Row person : persons) {
			String id = person.getString("person");
			if (!numTrips.containsKey(id))
				numTrips.put(id, 0);
		}

		double avgTrips = numTrips.values().intStream().average().orElse(0);

		Table table = Table.create();

		for (Object2IntMap.Entry<String> e : usedModes.object2IntEntrySet()) {
			table.addColumns(DoubleColumn.create(e.getKey() + "_user", e.getIntValue() / totalMobile));
		}

		table.addColumns(
				DoubleColumn.create("n", (double) numTrips.size()),
				DoubleColumn.create("mobile", totalMobile / numTrips.size()),
				DoubleColumn.create("avg_trips", avgTrips),
				DoubleColumn.create("avg_trips_mobile", avgTripsMobile)
		);

		table.write().csv(output.getPath("population_stats.csv").toFile());

	}

	private void writeTripPurposes(Table trips) {

		IntList departure = new IntArrayList(trips.rowCount());
		IntList arrival = new IntArrayList(trips.rowCount());

		for (Row t : trips) {
			int[] depTime = durationToHour(t.getString("dep_time"));
			departure.add(depTime[0]);

			int[] travTimes = durationToHour(t.getString("trav_time"));

			depTime[2] += travTimes[2];
			if (depTime[2] >= 60)
				depTime[1]++;

			depTime[1] += travTimes[1];
			if (depTime[1] >= 60)
				depTime[0]++;

			depTime[0] += travTimes[0];

			arrival.add(depTime[0]);

		}

		trips.addColumns(
				IntColumn.create("departure_h", departure.intStream().toArray()),
				IntColumn.create("arrival_h", arrival.intStream().toArray())
		);

		TextColumn purpose = trips.textColumn("end_activity_type");

		// Remove suffix durations like _345
		Selection withDuration = purpose.matchesRegex("^.+_[0-9]+$");
		purpose.set(withDuration, purpose.where(withDuration).replaceAll("_[0-9]+$", ""));

		Table tArrival = trips.summarize("trip_id", count).by("end_activity_type", "arrival_h");

		tArrival.column(0).setName("purpose");
		tArrival.column(1).setName("h");

		DoubleColumn share = tArrival.numberColumn(2).divide(tArrival.numberColumn(2).sum()).setName("arrival");
		tArrival.replaceColumn(2, share);

		Table tDeparture = trips.summarize("trip_id", count).by("end_activity_type", "departure_h");

		tDeparture.column(0).setName("purpose");
		tDeparture.column(1).setName("h");

		share = tDeparture.numberColumn(2).divide(tDeparture.numberColumn(2).sum()).setName("departure");
		tDeparture.replaceColumn(2, share);


		Table table = new DataFrameJoiner(tArrival, "purpose", "h").fullOuter(tDeparture).sortOn(0, 1);

		table.doubleColumn("departure").setMissingTo(0.0);
		table.doubleColumn("arrival").setMissingTo(0.0);

		table.write().csv(output.getPath("trip_purposes_by_hour.csv").toFile());

	}
}
