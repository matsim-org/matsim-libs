package org.matsim.application.analysis.population;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.strings.AbstractStringColumn;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.joining.DataFrameJoiner;
import tech.tablesaw.selection.Selection;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static tech.tablesaw.aggregate.AggregateFunctions.count;

@CommandLine.Command(name = "trips", description = "Calculates various trip related metrics.")
@CommandSpec(
	requires = {"trips.csv", "persons.csv"},
	produces = {
		"mode_share.csv", "mode_share_per_dist.csv", "mode_users.csv", "trip_stats.csv",
		"mode_share_per_purpose.csv", "mode_share_per_%s.csv",
		"population_trip_stats.csv", "trip_purposes_by_hour.csv",
		"mode_share_distance_distribution.csv", "mode_shift.csv", "mode_chains.csv",
		"mode_choices.csv", "mode_choice_evaluation.csv", "mode_choice_evaluation_per_mode.csv",
		"mode_confusion_matrix.csv", "mode_prediction_error.csv"
	}
)
public class TripAnalysis implements MATSimAppCommand {

	/**
	 * Attributes which relates this person to a reference person.
	 */
	public static final String ATTR_REF_ID = "ref_id";
	/**
	 * Person attribute that contains the reference modes of a person. Multiple modes are delimited by "-".
	 */
	public static final String ATTR_REF_MODES = "ref_modes";
	/**
	 * Person attribute containing its weight for analysis purposes.
	 */
	public static final String ATTR_REF_WEIGHT = "ref_weight";
	private static final Logger log = LogManager.getLogger(TripAnalysis.class);
	@CommandLine.Option(names = "--person-filter", description = "Define which persons should be included into trip analysis. Map like: Attribute name (key), attribute value (value). " +
		"The attribute needs to be contained by output_persons.csv. Persons who do not match all filters are filtered out.", split = ",")
	private final Map<String, String> personFilters = new HashMap<>();
	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(TripAnalysis.class);
	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(TripAnalysis.class);
	@CommandLine.Option(names = "--input-ref-data", description = "Optional path to reference data", required = false)
	private String refData;
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

	private static double[] calcHistogram(double[] data, double[] bins) {

		double[] hist = new double[bins.length - 1];

		for (int i = 0; i < bins.length - 1; i++) {

			double binStart = bins[i];
			double binEnd = bins[i + 1];

			// The last right bin edge is inclusive, which is consistent with the numpy implementation
			if (i == bins.length - 2)
				hist[i] = Arrays.stream(data).filter(d -> d >= binStart && d <= binEnd).count();
			else
				hist[i] = Arrays.stream(data).filter(d -> d >= binStart && d < binEnd).count();
		}

		return hist;
	}

	private static Map<String, ColumnType> getColumnTypes() {
		Map<String, ColumnType> columnTypes = new HashMap<>(Map.of("person", ColumnType.TEXT,
			"trav_time", ColumnType.STRING, "wait_time", ColumnType.STRING, "dep_time", ColumnType.STRING,
			"longest_distance_mode", ColumnType.STRING, "main_mode", ColumnType.STRING,
			"start_activity_type", ColumnType.TEXT, "end_activity_type", ColumnType.TEXT,
			"first_pt_boarding_stop", ColumnType.TEXT, "last_pt_egress_stop", ColumnType.TEXT));

		// Map.of only has 10 argument max
		columnTypes.put("traveled_distance", ColumnType.LONG);
		columnTypes.put("euclidean_distance", ColumnType.LONG);

		return columnTypes;
	}

	@Override
	public Integer call() throws Exception {

		Table persons = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(input.getPath("persons.csv")))
			.columnTypesPartial(Map.of("person", ColumnType.TEXT))
			.sample(false)
			.separator(CsvOptions.detectDelimiter(input.getPath("persons.csv"))).build());

		int total = persons.rowCount();

		if (matchId != null) {
			log.info("Using id filter {}", matchId);
			persons = persons.where(persons.textColumn("person").matchesRegex(matchId));
		}

//		filter persons according to person (attribute) filter
		if (!personFilters.isEmpty()) {
			IntSet generalFilteredRowIds = null;
			for (Map.Entry<String, String> entry : personFilters.entrySet()) {
				if (!persons.containsColumn(entry.getKey())) {
					log.warn("Persons table does not contain column for filter attribute {}. Filter on {} will not be applied.", entry.getKey(), entry.getValue());
					continue;
				}
				log.info("Using person filter for attribute {} and value {}", entry.getKey(), entry.getValue());

				IntSet filteredRowIds = new IntOpenHashSet();

				for (int i = 0; i < persons.rowCount(); i++) {
					Row row = persons.row(i);
					String value = row.getString(entry.getKey());
//					only add value once
					if (value.equals(entry.getValue())) {
						filteredRowIds.add(i);
					}
				}

				if (generalFilteredRowIds == null) {
					// If generalFilteredRowIds is empty, add all elements from filteredRowIds to generalFilteredRowIds
					generalFilteredRowIds = filteredRowIds;
				} else {
					// If generalFilteredRowIds is not empty, retain only the elements that are also in filteredRowIds
					generalFilteredRowIds.retainAll(filteredRowIds);
				}
			}

			if (generalFilteredRowIds != null) {
				persons = persons.where(Selection.with(generalFilteredRowIds.intStream().toArray()));
			}
		}

		log.info("Filtered {} out of {} persons", persons.rowCount(), total);

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

		Table trips = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(input.getPath("trips.csv")))
			.columnTypesPartial(getColumnTypes())
			.sample(false)
			.separator(CsvOptions.detectDelimiter(input.getPath("trips.csv"))).build());

		// Trip filter with start AND end
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
//		trip filter with start OR end
		} else if (shp.isDefined() && filter == LocationFilter.trip_start_or_end) {
			Geometry geometry = shp.getGeometry();
			GeometryFactory f = new GeometryFactory();
			IntList idx = new IntArrayList();

			for (int i = 0; i < trips.rowCount(); i++) {
				Row row = trips.row(i);
				Point start = f.createPoint(new Coordinate(row.getDouble("start_x"), row.getDouble("start_y")));
				Point end = f.createPoint(new Coordinate(row.getDouble("end_x"), row.getDouble("end_y")));
				if (geometry.contains(start) || geometry.contains(end)) {
					idx.add(i);
				}
			}

			trips = trips.where(Selection.with(idx.toIntArray()));
		}

		TripByGroupAnalysis groups = null;
		if (refData != null) {
			groups = new TripByGroupAnalysis(refData);
			groups.groupPersons(persons);
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

		TextColumn purpose = joined.textColumn("end_activity_type");

		// Remove suffix durations like _345
		purpose.set(Selection.withRange(0, purpose.size()), purpose.replaceAll("_[0-9]{2,}$", ""));

		writeModeShare(joined, labels);

		if (groups != null) {
			groups.writeModeShare(joined, labels, modeOrder, (g) -> output.getPath("mode_share_per_%s.csv", g));
		}

		if (persons.containsColumn(ATTR_REF_MODES)) {
			try {
				TripChoiceAnalysis choices = new TripChoiceAnalysis(persons, trips, modeOrder);

				choices.writeChoices(output.getPath("mode_choices.csv"));
				choices.writeChoiceEvaluation(output.getPath("mode_choice_evaluation.csv"));
				choices.writeChoiceEvaluationPerMode(output.getPath("mode_choice_evaluation_per_mode.csv"));
				choices.writeConfusionMatrix(output.getPath("mode_confusion_matrix.csv"));
				choices.writeModePredictionError(output.getPath("mode_prediction_error.csv"));
			} catch (RuntimeException e) {
				log.error("Error while analyzing mode choices", e);
			}
		}

		writePopulationStats(persons, joined);

		tryRun(this::writeTripStats, joined);
		tryRun(this::writeTripPurposes, joined);
		tryRun(this::writeTripDistribution, joined);
		tryRun(this::writeModeShift, joined);
		tryRun(this::writeModeChains, joined);
		tryRun(this::writeModeStatsPerPurpose, joined);

		return 0;
	}

	private void tryRun(ThrowingConsumer<Table> f, Table df) {
		try {
			f.accept(df);
		} catch (IOException e) {
			log.error("Error while running method", e);
		}
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
		Object2LongMap<String> beelineDistance = new Object2LongOpenHashMap<>();

		for (Row trip : trips) {
			String mainMode = trip.getString("main_mode");

			n.mergeInt(mainMode, 1, Integer::sum);
			travelTime.mergeLong(mainMode, durationToSeconds(trip.getString("trav_time")), Long::sum);
			travelDistance.mergeLong(mainMode, trip.getLong("traveled_distance"), Long::sum);
			beelineDistance.mergeLong(mainMode, trip.getLong("euclidean_distance"), Long::sum);
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

			printer.print("Avg. beeline speed [km/h]");
			for (String m : modeOrder) {
				double speed = (beelineDistance.getLong(m) / 1000d) / (travelTime.getLong(m) / (60d * 60d));
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

	private void writeTripDistribution(Table trips) throws IOException {

		Map<String, double[]> dists = new LinkedHashMap<>();

		// Note that the results of this interpolator are consistent with the one performed in matsim-python-tools
		// This makes the results comparable with reference data, changes here will also require changes in the python package
		LoessInterpolator inp = new LoessInterpolator(0.05, 0);

		long max = distGroups.get(distGroups.size() - 3) + distGroups.get(distGroups.size() - 2);

		double[] bins = IntStream.range(0, (int) (max / 100)).mapToDouble(i -> i * 100).toArray();
		double[] x = Arrays.copyOf(bins, bins.length - 1);

		for (String mode : modeOrder) {
			double[] distances = trips.where(
					trips.stringColumn("main_mode").equalsIgnoreCase(mode))
				.numberColumn("traveled_distance").asDoubleArray();

			double[] hist = calcHistogram(distances, bins);

			double[] y = inp.smooth(x, hist);
			dists.put(mode, y);
		}

		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("mode_share_distance_distribution.csv")), CSVFormat.DEFAULT)) {

			printer.print("dist");
			for (String s : modeOrder) {
				printer.print(s);
			}
			printer.println();

			for (int i = 0; i < x.length; i++) {

				double sum = 0;
				for (String s : modeOrder) {
					sum += Math.max(0, dists.get(s)[i]);
				}

				printer.print(x[i]);
				for (String s : modeOrder) {
					double value = Math.max(0, dists.get(s)[i]) / sum;
					printer.print(value);
				}
				printer.println();
			}
		}
	}

	private void writeModeShift(Table trips) throws IOException {
		Path path;
		try {
			Path dir = Path.of(input.getPath("trips.csv")).getParent().resolve("ITERS").resolve("it.0");
			path = ApplicationUtils.matchInput("trips.csv", dir);
		} catch (Exception e) {
			log.error("Could not find trips from 0th iteration.", e);
			return;
		}

		Table originalTrips = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(path.toString()))
			.columnTypesPartial(getColumnTypes())
			.sample(false)
			.separator(CsvOptions.detectDelimiter(path.toString())).build());

		// Use longest_distance_mode where main_mode is not present
		originalTrips.stringColumn("main_mode")
			.set(originalTrips.stringColumn("main_mode").isMissing(),
				originalTrips.stringColumn("longest_distance_mode"));

		originalTrips.column("main_mode").setName("original_mode");

		Table joined = new DataFrameJoiner(trips, "trip_id").inner(true, originalTrips);
		Table aggr = joined.summarize("trip_id", count).by("original_mode", "main_mode");

		aggr.write().csv(output.getPath("mode_shift.csv").toFile());
	}

	/**
	 * Collects information about all modes used during one day.
	 */
	private void writeModeChains(Table trips) throws IOException {

		Map<String, List<String>> modesPerPerson = new LinkedHashMap<>();

		for (Row trip : trips) {
			String id = trip.getString("person");
			String mode = trip.getString("main_mode");
			modesPerPerson.computeIfAbsent(id, s -> new LinkedList<>()).add(mode);
		}

		// Store other values explicitly
		ObjectDoubleMutablePair<String> other = ObjectDoubleMutablePair.of("other", 0);
		Object2DoubleMap<String> chains = new Object2DoubleOpenHashMap<>();
		for (List<String> modes : modesPerPerson.values()) {
			String key;
			if (modes.size() == 1)
				key = modes.getFirst();
			else if (modes.size() > 6) {
				other.right(other.rightDouble() + 1);
				continue;
			} else
				key = String.join("-", modes);

			chains.mergeDouble(key, 1, Double::sum);
		}


		List<ObjectDoubleMutablePair<String>> counts = chains.object2DoubleEntrySet().stream()
			.map(e -> ObjectDoubleMutablePair.of(e.getKey(), (int) e.getDoubleValue()))
			.sorted(Comparator.comparingDouble(p -> -p.rightDouble()))
			.collect(Collectors.toList());

		// Aggregate entries to prevent file from getting too large
		for (int i = 250; i < counts.size(); i++) {
			other.right(other.rightDouble() + counts.get(i).rightDouble());
		}
		counts = counts.subList(0, Math.min(counts.size(), 250));
		counts.add(other);

		counts.sort(Comparator.comparingDouble(p -> -p.rightDouble()));


		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("mode_chains.csv")), CSVFormat.DEFAULT)) {

			printer.printRecord("modes", "count", "share");

			double total = counts.stream().mapToDouble(ObjectDoubleMutablePair::rightDouble).sum();
			for (ObjectDoubleMutablePair<String> p : counts) {
				printer.printRecord(p.left(), (int) p.rightDouble(), p.rightDouble() / total);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void writeModeStatsPerPurpose(Table trips) {

		Table aggr = trips.summarize("trip_id", count).by("end_activity_type", "main_mode");

		Comparator<Row> cmp = Comparator.comparing(row -> row.getString("end_activity_type"));
		aggr = aggr.sortOn(cmp.thenComparing(row -> row.getString("main_mode")));

		aggr.doubleColumn(aggr.columnCount() - 1).setName("share");
		aggr.column("end_activity_type").setName("purpose");

		Set<String> purposes = (Set<String>) aggr.column("purpose").asSet();

		// Norm each purpose to 1
		// It was not clear if the purpose is a string or text colum, therefor this code uses the abstract version
		for (String label : purposes) {
			DoubleColumn all = aggr.doubleColumn("share");
			Selection sel = ((AbstractStringColumn<?>) aggr.column("purpose")).isEqualTo(label);

			double total = all.where(sel).sum();
			if (total > 0)
				all.set(sel, all.divide(total));
		}

		aggr.write().csv(output.getPath("mode_share_per_purpose.csv").toFile());
	}

	/**
	 * How shape file filtering should be applied.
	 */
	enum LocationFilter {
		trip_start_and_end,
		trip_start_or_end,
		home,
		none
	}

	@FunctionalInterface
	private interface ThrowingConsumer<T> {
		void accept(T t) throws IOException;
	}
}
