package org.matsim.application.analysis.population;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.util.*;

import static tech.tablesaw.aggregate.AggregateFunctions.count;

@CommandLine.Command(name = "trips", description = "Calculates various trip related metrics.")
@CommandSpec(
	requires = {"trips.csv", "persons.csv"},
	produces = {"mode_share.csv", "mode_share_per_dist.csv", "mode_users.csv", "trip_stats.csv", "population_trip_stats.csv", "trip_purposes_by_hour.csv"}
)
public class TripAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(TripAnalysis.class);
	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(TripAnalysis.class);

	@CommandLine.Option(names = "--match-id", description = "Pattern to filter agents by id")
	private String matchId;

	@CommandLine.Option(names = "--dist-groups", split = ",", description = "List of distances for binning", defaultValue = "0,1000,2000,5000,10000,20000")
	private List<Integer> distGroups;

	@CommandLine.Option(names = "--modes", split = ",", description = "List of considered modes, if not set all will be used")
	private List<String> modeOrder;

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

	private static int durationToSeconds(String d) {
		String[] split = d.split(":");
		return (Integer.parseInt(split[0]) * 60 * 60) + (Integer.parseInt(split[1]) * 60) + Integer.parseInt(split[2]);
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

		writeModeShare(joined, labels);

		writePopulationStats(persons, joined);

		writeTripStats(joined);

		writeTripPurposes(joined);

		return 0;
	}

	private void writeModeShare(Table trips, List<String> labels) {

		Table aggr = trips.summarize("trip_id", count).by("dist_group", "main_mode");

		DoubleColumn share = aggr.numberColumn(2).divide(aggr.numberColumn(2).sum()).setName("share");
		aggr.replaceColumn(2, share);

		// Sort by dist_group and mode
		Comparator<Row> cmp = Comparator.comparingInt(row -> labels.indexOf(row.getString("dist_group")));
		aggr = aggr.sortOn(cmp.thenComparing(row -> row.getString("main_mode")));

		aggr.write().csv(output.getPath("mode_share.csv").toFile());

		// Norm each dist_group to 1
		for (String label : labels) {
			DoubleColumn dist_group = aggr.doubleColumn("share");
			Selection sel = aggr.stringColumn("dist_group").isEqualTo(label);

			double total = dist_group.where(sel).sum();
			if (total > 0)
				dist_group.set(sel, dist_group.divide(total));
		}

		aggr.write().csv(output.getPath("mode_share_per_dist.csv").toFile());

		// Derive mode order if not given
		if (modeOrder == null) {
			modeOrder = new ArrayList<>();
			for (Row row : aggr) {
				String mainMode = row.getString("main_mode");
				if (!modeOrder.contains(mainMode)) {
					modeOrder.add(mainMode);
				}
			}
		}
	}

	private void writeTripStats(Table trips) throws IOException {

		// Stats per mode
		Object2IntMap<String> n = new Object2IntLinkedOpenHashMap<>();
		Object2IntMap<String> travelTime = new Object2IntLinkedOpenHashMap<>();
		Object2IntMap<String> travelDistance = new Object2IntLinkedOpenHashMap<>();

		for (Row trip : trips) {
			String mainMode = trip.getString("main_mode");

			n.mergeInt(mainMode, 1, Integer::sum);
			travelTime.mergeInt(mainMode, durationToSeconds(trip.getString("trav_time")), Integer::sum);
			travelDistance.mergeInt(mainMode, trip.getInt("traveled_distance"), Integer::sum);
		}

		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("trip_stats.csv")), CSVFormat.DEFAULT)) {

			printer.print("Info");
			for (String m : modeOrder) {
				printer.print(m);
			}
			printer.println();

			printer.print("Number of trips");
			for (String m : modeOrder) {
				printer.print(n.getInt(m));
			}
			printer.println();

			printer.print("Total time traveled [h]");
			for (String m : modeOrder) {
				int seconds = travelTime.getInt(m);
				printer.print(new BigDecimal(seconds / (60d * 60d)).setScale(0, RoundingMode.HALF_UP));
			}
			printer.println();

			printer.print("Total distance traveled [km]");
			for (String m : modeOrder) {
				double meter = travelDistance.getInt(m);
				printer.print(new BigDecimal(meter / 1000d).setScale(0, RoundingMode.HALF_UP));
			}
			printer.println();

			printer.print("Avg. speed [km/h]");
			for (String m : modeOrder) {
				double speed = (travelDistance.getInt(m) / 1000d) / (travelTime.getInt(m) / (60d * 60d));
				printer.print(new BigDecimal(speed).setScale(2, RoundingMode.HALF_UP));

			}
			printer.println();

			printer.print("Avg. distance per trip [km]");
			for (String m : modeOrder) {
				double avg = (travelDistance.getInt(m) / 1000d) / (n.getInt(m));
				printer.print(new BigDecimal(avg).setScale(2, RoundingMode.HALF_UP));

			}
			printer.println();
		}
	}

	private void writePopulationStats(Table persons, Table trips) throws IOException {

		Object2IntMap<String> tripsPerPerson = new Object2IntLinkedOpenHashMap<>();
		Map<String, Set<String>> modesPerPerson = new LinkedHashMap<>();

		for (Row trip : trips) {
			String id = trip.getString("person");
			tripsPerPerson.mergeInt(id, 1, Integer::sum);
			String mode = trip.getString("main_mode");
			modesPerPerson.computeIfAbsent(id, s -> new LinkedHashSet<>()).add(mode);
		}

		Object2IntMap<String> usedModes = new Object2IntLinkedOpenHashMap<>();
		for (Map.Entry<String, Set<String>> e : modesPerPerson.entrySet()) {
			for (String mode : e.getValue()) {
				usedModes.mergeInt(mode, 1, Integer::sum);
			}
		}

		double totalMobile = tripsPerPerson.size();
		double avgTripsMobile = tripsPerPerson.values().intStream().average().orElse(0);

		for (Row person : persons) {
			String id = person.getString("person");
			if (!tripsPerPerson.containsKey(id))
				tripsPerPerson.put(id, 0);
		}

		double avgTrips = tripsPerPerson.values().intStream().average().orElse(0);

		Table table = Table.create(TextColumn.create("main_mode", usedModes.size()), DoubleColumn.create("user", usedModes.size()));

		int i = 0;
		for (String m : modeOrder) {
			int n = usedModes.getInt(m);
			table.textColumn(0).set(i, m);
			table.doubleColumn(1).set(i++, n / totalMobile);
		}

		table.write().csv(output.getPath("mode_users.csv").toFile());

		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("population_trip_stats.csv")), CSVFormat.DEFAULT)) {

			printer.printRecord("Info", "Value");
			printer.printRecord("Persons", tripsPerPerson.size());
			printer.printRecord("Mobile persons [%]", new BigDecimal(100 * totalMobile / tripsPerPerson.size()).setScale(2, RoundingMode.HALF_UP));
			printer.printRecord("Avg. trips", new BigDecimal(avgTrips).setScale(2, RoundingMode.HALF_UP));
			printer.printRecord("Avg. trip per mobile persons", new BigDecimal(avgTripsMobile).setScale(2, RoundingMode.HALF_UP));
		}
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

	/**
	 * How shape file filtering should be applied.
	 */
	enum LocationFilter {
		// TODO: shp file support and option
		trip_start_and_end,
		trip_start_or_end,
		home,
		none
	}
}