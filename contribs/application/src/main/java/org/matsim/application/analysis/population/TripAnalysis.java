package org.matsim.application.analysis.population;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.joining.DataFrameJoiner;
import tech.tablesaw.selection.Selection;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static tech.tablesaw.aggregate.AggregateFunctions.count;

@CommandLine.Command(name = "trips", description = "Calculates various trip related metrics.")
@CommandSpec(
	requires = {"trips.csv", "persons.csv"},
	produces = {"mode_share.csv", "mode_share_per_dist.csv", "mode_users.csv", "trip_stats.csv", "population_trip_stats.csv", "trip_purposes_by_hour.csv"}
)
public class TripAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(TripAnalysis.class);

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(TripAnalysis.class);
	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(TripAnalysis.class);

	@CommandLine.Option(names = "--match-id", description = "Pattern to filter agents by id")
	private String matchId;

	@CommandLine.Option(names = "--dist-groups", split = ",", description = "List of distances for binning", defaultValue = "0,1000,2000,5000,10000,20000")
	private List<Long> distGroups;

	@CommandLine.Option(names = "--modes", split = ",", description = "List of considered modes, if not set all will be used")
	private List<String> modeOrder;

	@CommandLine.Option(names = "--shp-filter", description = "Define how the shp file filtering should work", defaultValue = "home")
	private LocationFilter filter;

	@CommandLine.Mixin
	private ShpOptions shp;

	public static void main(String[] args) {
		new TripAnalysis().execute(args);
	}

	private static String cut(long dist, List<Long> distGroups, List<String> labels) {

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
			.sample(false)
			.separator(new CsvOptions().detectDelimiter(input.getPath("persons.csv"))).build());

		int total = persons.rowCount();

		if (matchId != null) {
			log.info("Using id filter {}", matchId);
			persons = persons.where(persons.textColumn("person").matchesRegex(matchId));
		}

		// Home filter by standard attribute
		if (shp.isDefined() && filter == LocationFilter.home) {
			Geometry geometry = shp.getGeometry();
			GeometryFactory f = new GeometryFactory();

			IntList idx = new IntArrayList();

			for (int i = 0; i < persons.rowCount(); i++) {
				Row row = persons.row(i);
				Point p = f.createPoint(new Coordinate(row.getDouble("home_x"), row.getDouble("home_y")));
				if (geometry.contains(p)) {
					idx.add(i);
				}
			}

			persons = persons.where(Selection.with(idx.toIntArray()));
		}

		log.info("Filtered {} out of {} persons", persons.rowCount(), total);

		Map<String, ColumnType> columnTypes = new HashMap<>(Map.of("person", ColumnType.TEXT,
			"trav_time", ColumnType.STRING, "wait_time", ColumnType.STRING, "dep_time", ColumnType.STRING,
			"longest_distance_mode", ColumnType.STRING, "main_mode", ColumnType.STRING,
			"start_activity_type", ColumnType.TEXT, "end_activity_type", ColumnType.TEXT,
			"first_pt_boarding_stop", ColumnType.TEXT, "last_pt_egress_stop", ColumnType.TEXT));

		// Map.of only has 10 argument max
		columnTypes.put("traveled_distance", ColumnType.LONG);

		Table trips = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(input.getPath("trips.csv")))
			.columnTypesPartial(columnTypes)
			.sample(false)
			.separator(CsvOptions.detectDelimiter(input.getPath("trips.csv"))).build());

		// Trip filter with start and end
		if (shp.isDefined() && filter == LocationFilter.trip_start_and_end) {
			Geometry geometry = shp.getGeometry();
			GeometryFactory f = new GeometryFactory();
			IntList idx = new IntArrayList();

			for (int i = 0; i < trips.rowCount(); i++) {
				Row row = trips.row(i);
				Point start = f.createPoint(new Coordinate(row.getDouble("start_x"), row.getDouble("start_y")));
				Point end = f.createPoint(new Coordinate(row.getDouble("end_x"), row.getDouble("end_y")));
				if (geometry.contains(start) && geometry.contains(end)) {
					idx.add(i);
				}
			}

			trips = trips.where(Selection.with(idx.toIntArray()));
		}

		// Use longest_distance_mode where main_mode is not present
		trips.stringColumn("main_mode")
			.set(trips.stringColumn("main_mode").isMissing(),
				trips.stringColumn("longest_distance_mode"));


		Table joined = new DataFrameJoiner(trips, "person").inner(persons);

		log.info("Filtered {} out of {} trips", joined.rowCount(), trips.rowCount());

		List<String> labels = new ArrayList<>();
		for (int i = 0; i < distGroups.size() - 1; i++) {
			labels.add(String.format("%d - %d", distGroups.get(i), distGroups.get(i + 1)));
		}
		labels.add(distGroups.get(distGroups.size() - 1) + "+");
		distGroups.add(Long.MAX_VALUE);

		StringColumn dist_group = joined.longColumn("traveled_distance")
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
		Object2LongMap<String> travelTime = new Object2LongOpenHashMap<>();
		Object2LongMap<String> travelDistance = new Object2LongOpenHashMap<>();

		for (Row trip : trips) {
			String mainMode = trip.getString("main_mode");

			n.mergeInt(mainMode, 1, Integer::sum);
			travelTime.mergeLong(mainMode, durationToSeconds(trip.getString("trav_time")), Long::sum);
			travelDistance.mergeLong(mainMode, trip.getLong("traveled_distance"), Long::sum);
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
				long seconds = travelTime.getLong(m);
				printer.print(new BigDecimal(seconds / (60d * 60d)).setScale(0, RoundingMode.HALF_UP));
			}
			printer.println();

			printer.print("Total distance traveled [km]");
			for (String m : modeOrder) {
				double meter = travelDistance.getLong(m);
				printer.print(new BigDecimal(meter / 1000d).setScale(0, RoundingMode.HALF_UP));
			}
			printer.println();

			printer.print("Avg. speed [km/h]");
			for (String m : modeOrder) {
				double speed = (travelDistance.getLong(m) / 1000d) / (travelTime.getLong(m) / (60d * 60d));
				printer.print(new BigDecimal(speed).setScale(2, RoundingMode.HALF_UP));

			}
			printer.println();

			printer.print("Avg. distance per trip [km]");
			for (String m : modeOrder) {
				double avg = (travelDistance.getLong(m) / 1000d) / (n.getInt(m));
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
		trip_start_and_end,
		home,
		none
	}
}
