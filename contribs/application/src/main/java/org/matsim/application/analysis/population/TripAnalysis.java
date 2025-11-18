package org.matsim.application.analysis.population;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.*;
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
import java.util.stream.Stream;

import static tech.tablesaw.aggregate.AggregateFunctions.count;
import static tech.tablesaw.aggregate.AggregateFunctions.sum;

@CommandLine.Command(name = "trips", description = "Calculates various trip related metrics.")
@CommandSpec(
	requires = {"trips.csv", "persons.csv"},
	produces = {
		"mode_share_%s.csv", "mode_share_per_dist_%s.csv", "mode_users_%s.csv", "trip_stats_%s.csv",
		"mode_share_per_purpose.csv", "mode_share_per_%s.csv",
		"population_trip_stats.csv", "trip_purposes_by_hour_%s.csv",
		"mode_share_distance_distribution_%s.csv", "mode_shift.csv", "mode_chains.csv",
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

	private static Map<String, List<String>> groupsOfSubpopulationsForPersonAnalysis = new HashMap<>();
	private static Map<String, List<String>> groupsOfSubpopulationsForCommercialAnalysis = new HashMap<>();

	public enum ModelType {
		PERSON_TRAFFIC("personTraffic"),
		COMMERCIAL_TRAFFIC("commercialTraffic"),
		UNASSIGNED("undefinedGroup");

		private final String id;
		ModelType(String id) { this.id = id; }
		@Override public String toString() { return id; }
	}

	private static final Logger log = LogManager.getLogger(TripAnalysis.class);
	@CommandLine.Option(names = "--person-filter", description = "Define which persons should be included into trip analysis. Map like: Attribute name (key), attribute value (value). " +
		"The attribute needs to be contained by output_persons.csv. Persons who do not match all filters are filtered out.", split = ",")
	private final Map<String, String> personFilters = new HashMap<>();
	@CommandLine.Option(names = "--groups-of-subpopulations-personAnalysis", description = "Define the subpopulations for the analysis of the person traffic and defines if the have different groups. If a group is defined by several subpopulations," +
		"split them by ','. and different groups are seperated by ';'. The analysis output will be for the given groups.", split = ";")
	private final Map<String, String> groupsOfSubpopulationsForPersonAnalysisRaw = new HashMap<>();
	@CommandLine.Option(names = "--groups-of-subpopulations-commercialAnalysis", description = "Define the subpopulations for the analysis of the commercial traffic and defines if the have different groups. If a group is defined by several subpopulations," +
		"split them by ','. and different groups are seperated by ';'. The analysis output will be for the given groups.", split = ";")
	private final Map<String, String> groupsOfSubpopulationsForCommercialAnalysisRaw = new HashMap<>();
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
		Map<String, ColumnType> columnTypes = new HashMap<>(Map.of("person", ColumnType.STRING,
			"trav_time", ColumnType.STRING, "wait_time", ColumnType.STRING, "dep_time", ColumnType.STRING,
			"longest_distance_mode", ColumnType.STRING, "main_mode", ColumnType.STRING,
			"start_activity_type", ColumnType.STRING, "end_activity_type", ColumnType.STRING,
			"first_pt_boarding_stop", ColumnType.STRING, "last_pt_egress_stop", ColumnType.STRING));

		// Map.of only has 10 argument max
		columnTypes.put("traveled_distance", ColumnType.LONG);
		columnTypes.put("euclidean_distance", ColumnType.LONG);

		return columnTypes;
	}

	/**
	 * Converts the raw input map into a map of lists for easier processing.
	 */
	private Map<String, List<String>> getGroupsOfSubpopulations(Map<String, String> groupsOfSubpopulationsRaw) {
		Map<String, List<String>> groupsOfSubpopulations = new HashMap<>();
		for (Map.Entry<String, String> entry : groupsOfSubpopulationsRaw.entrySet()) {
			List<String> subpops = Arrays.asList(entry.getValue().split(","));
			groupsOfSubpopulations.put(entry.getKey(), subpops);
		}
		return groupsOfSubpopulations;
	}

	/**
	 * Gets the group name of a subpopulation. If the subpopulation is not assigned to any group, NO_GROUP_ASSIGNED is returned.
	 *
	 * @return the group name of the subpopulation or NO_GROUP_ASSIGNED if the subpopulation is not assigned to any group.
	 */
	private String getGroupOfSubpopulation(String subpopulation) {
		String group = null;
		for (Map.Entry<String, List<String>> entry : groupsOfSubpopulationsForPersonAnalysis.entrySet()) {
			if (entry.getValue().contains(subpopulation)) {
				if (group != null)
					log.warn("Subpopulation {} is assigned to multiple groups. Returning the first group {}. Other group is {}", subpopulation, group, entry.getKey());
				else
					group = entry.getKey();
			}
		}
		for (Map.Entry<String, List<String>> entry : groupsOfSubpopulationsForCommercialAnalysis.entrySet()) {
			if (entry.getValue().contains(subpopulation)) {
				if (group != null)
					log.warn("Subpopulation {} is assigned to multiple groups. Returning the first group {}. Other group is {}", subpopulation, group, entry.getKey());
				else
					group = entry.getKey();
			}
		}
		if (group != null)
			return group;
		else
			return ModelType.UNASSIGNED.toString();
	}

	@Override
	public Integer call() throws Exception {

		if (!groupsOfSubpopulationsForPersonAnalysisRaw.isEmpty())
			groupsOfSubpopulationsForPersonAnalysis = getGroupsOfSubpopulations(groupsOfSubpopulationsForPersonAnalysisRaw);
		if (!groupsOfSubpopulationsForCommercialAnalysisRaw.isEmpty())
			groupsOfSubpopulationsForCommercialAnalysis = getGroupsOfSubpopulations(groupsOfSubpopulationsForCommercialAnalysisRaw);

		Table persons = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(input.getPath("persons.csv")))
			.columnTypesPartial(Map.of("person", ColumnType.STRING, "home_x", ColumnType.DOUBLE, "home_y", ColumnType.DOUBLE))
			.sample(false)
			.separator(CsvOptions.detectDelimiter(input.getPath("persons.csv"))).build());
		StringColumn subpop = persons.stringColumn("subpopulation");

		StringColumn modelType = StringColumn.create("modelType");

		for (String subpopulation : subpop) {
			String foundModelType = getModelType(subpopulation).toString();
			modelType.append(foundModelType);
		}
		persons.addColumns(modelType);

		int total = persons.rowCount();

		if (matchId != null) {
			log.info("Using id filter {}", matchId);
			persons = persons.where(persons.stringColumn("person").matchesRegex(matchId));
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
			log.info("Using home filter for TripAnalysis. Persons outside of the shape file will be removed from the analysis. Agents without home coordinates will be kept, because they are probably commercial agents.");
			Geometry geometry = shp.getGeometry();
			GeometryFactory f = new GeometryFactory();

			IntList idx = new IntArrayList();

			for (int i = 0; i < persons.rowCount(); i++) {
				Row row = persons.row(i);
				Point p = f.createPoint(new Coordinate(row.getDouble("home_x"), row.getDouble("home_y")));
				if (geometry.contains(p) || Double.isNaN(p.getX()) && Double.isNaN(p.getY())) {//TODO discuss what we should do with the commercial agents without home coordinates
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
		if (modeOrder == null)
			modeOrder = new ArrayList<>(trips.stringColumn("main_mode").unique().asList());
		Collections.sort(modeOrder);
		Table joined = new DataFrameJoiner(trips, "person").inner(persons);

		log.info("Filtered {} out of {} trips", joined.rowCount(), trips.rowCount());

		List<String> labels = new ArrayList<>();
		for (int i = 0; i < distGroups.size() - 1; i++) {
			labels.add(String.format("%d - %d", distGroups.get(i), distGroups.get(i + 1)));
		}
		labels.add(distGroups.getLast() + "+");
		distGroups.add(Long.MAX_VALUE);

		StringColumn dist_group = joined.longColumn("traveled_distance")
			.map(dist -> cut(dist, distGroups, labels), ColumnType.STRING::create).setName("dist_group");

		joined.addColumns(dist_group);

		StringColumn purpose = joined.stringColumn("end_activity_type");

		// Remove suffix durations like _345
		purpose.set(Selection.withRange(0, purpose.size()), purpose.replaceAll("_[0-9]{2,}$", ""));

		writeModeShare(joined, labels);

		if (groups != null) {
			// filters for all subpopulations that are used for person analysis
			Table filteredForPersons = joined.where(joined.stringColumn("subpopulation").isIn(groupsOfSubpopulationsForPersonAnalysis.values().stream()
					.flatMap(Collection::stream)
					.collect(Collectors.toSet())));
			groups.writeModeShare(filteredForPersons, labels, modeOrder, (g) -> output.getPath("mode_share_per_%s.csv", g));
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

	private static ModelType getModelType(String subpopulation) {
	    if (subpopulation == null || subpopulation.isEmpty()) return ModelType.UNASSIGNED;
	    for (List<String> list : groupsOfSubpopulationsForPersonAnalysis.values()) {
	        if (list.contains(subpopulation)) return ModelType.PERSON_TRAFFIC;
	    }
	    for (List<String> list : groupsOfSubpopulationsForCommercialAnalysis.values()) {
	        if (list.contains(subpopulation)) return ModelType.COMMERCIAL_TRAFFIC;
	    }
	    return ModelType.UNASSIGNED;
	}

	private void tryRun(ThrowingConsumer<Table> f, Table df) {
		try {
			f.accept(df);
		} catch (IOException e) {
			log.error("Error while running method", e);
		}
	}

	private void writeModeShare(Table trips, List<String> labels) {

		Table aggr = trips.summarize("trip_id", count).by("dist_group", "main_mode", "subpopulation", "modelType");

		aggr = setEmptyCombinationsToZero(aggr);

		// Sort by dist_group and mode
		Comparator<Row> cmp = Comparator.comparingInt(row -> labels.indexOf(row.getString("dist_group")));
		aggr = aggr.sortOn(cmp.thenComparing(row -> row.getString("main_mode")).thenComparing(row -> row.getString("subpopulation")));

		for (String group : groupsOfSubpopulationsForPersonAnalysis.keySet()) {
			subSetAnalysisForModeShares(labels, group, aggr, cmp, groupsOfSubpopulationsForPersonAnalysis);
		}
		for (String group : groupsOfSubpopulationsForCommercialAnalysis.keySet()) {
			subSetAnalysisForModeShares(labels, group, aggr, cmp, groupsOfSubpopulationsForCommercialAnalysis);
		}

		DoubleColumn shareSum = aggr.numberColumn("Count [trip_id]").divide(aggr.numberColumn("Count [trip_id]").sum()).setName("share");
		aggr.replaceColumn("Count [trip_id]", shareSum);

		aggr = addModeSharesPerModelType(aggr);
		aggr.write().csv(output.getPath("mode_share_%s.csv", "total").toFile());

		// Norm each dist_group to 1
		normDistanceGroups(labels, aggr);

		aggr.write().csv(output.getPath("mode_share_per_dist_%s.csv", "total").toFile());
	}

	private static Table addModeSharesPerModelType(Table aggr) {
		List<String> modelTypes = aggr.stringColumn("modelType").unique().asList();

		String[] keys = new String[] {"dist_group", "main_mode", "subpopulation"};

		for (String mt : modelTypes) {
			Table countsMt = aggr.where(aggr.stringColumn("modelType").isEqualTo(mt))
				.selectColumns("dist_group", "main_mode", "subpopulation", "Count [trip_id]")
				.copy();
			String countCol = "count_" + mt;
			countsMt.column("Count [trip_id]").setName(countCol);

			aggr = aggr.joinOn(keys).leftOuter(countsMt);

			DoubleColumn c = aggr.numberColumn(countCol).asDoubleColumn();
			for (int row : c.isMissing().toArray()) c.set(row, 0.0);
			aggr.replaceColumn(countCol, c);

			double denom = c.sum();
			String shareCol = "share_" + mt;

			DoubleColumn share;
			if (denom == 0.0) {
				share = DoubleColumn.create(shareCol, aggr.rowCount());
				share.fillWith(0.0);
			} else {
				share = c.divide(denom).setName(shareCol);
			}
			aggr.addColumns(share);

			aggr.removeColumns(countCol);
		}
		return aggr;
	}

	private void subSetAnalysisForModeShares(List<String> labels, String group, Table aggr, Comparator<Row> cmp,
											 Map<String, List<String>> groupsOfSubpopulationsForAnalysis) {
		Table subset;
		if (groupsOfSubpopulationsForAnalysis.isEmpty())
			subset = aggr;
		else
			subset = aggr.where(
			aggr.stringColumn("subpopulation").isIn(groupsOfSubpopulationsForAnalysis.get(group)));
		Table sumsByMode = subset.summarize("Count [trip_id]", sum).by("main_mode");

		String sumColName = sumsByMode.columnNames().stream()
			.filter(n -> n.toLowerCase().contains("sum") && n.contains("Count [trip_id]"))
			.findFirst().orElseThrow();

		Table usedModesOnly = sumsByMode.where(sumsByMode.numberColumn(sumColName).isGreaterThan(0));
		Set<String> usedModes = new HashSet<>(usedModesOnly.stringColumn("main_mode").asList());

		subset = subset.where(subset.stringColumn("main_mode").isIn(usedModes));

		DoubleColumn share = subset.numberColumn("Count [trip_id]")
			.divide(subset.numberColumn("Count [trip_id]").sum())
			.setName("share");

		subset = subset.replaceColumn("Count [trip_id]", share)
			.sortOn(cmp);
		subset.write().csv(output.getPath("mode_share_%s.csv", group).toFile());

		normDistanceGroups(labels, subset);

		subset.write().csv(output.getPath("mode_share_per_dist_%s.csv", group).toFile());
	}

	private void normDistanceGroups(List<String> labels, Table aggr) {
		for (String label : labels) {
			DoubleColumn dist_group = aggr.doubleColumn("share");
			Selection sel = aggr.stringColumn("dist_group").isEqualTo(label);

			double total = dist_group.where(sel).sum();
			if (total > 0)
				dist_group.set(sel, dist_group.divide(total));
		}
	}

	/**
	 * If a combination of dist_group, main_mode, subpopulation doesn't exist, it will be added with Count [trip_id] = 0
	 *
	 * @param aggr table with columns dist_group, main_mode, subpopulation, Count [trip_id], modelType
	 */
	private static Table setEmptyCombinationsToZero(Table aggr) {
		List<String> distVals  = aggr.stringColumn("dist_group").unique().asList();
		List<String> modeVals  = aggr.stringColumn("main_mode").unique().asList();
		List<String> subpVals  = aggr.stringColumn("subpopulation").unique().asList();

		Table combos = Table.create("all_combos",
			StringColumn.create("dist_group"),
			StringColumn.create("main_mode"),
			StringColumn.create("subpopulation")
		);

		for (String d : distVals) {
			for (String m : modeVals) {
				for (String s : subpVals) {
					Row r = combos.appendRow();
					r.setString("dist_group", d);
					r.setString("main_mode", m);
					r.setString("subpopulation", s);
				}
			}
		}

		aggr = combos.joinOn("dist_group", "main_mode", "subpopulation")
			.leftOuter(aggr);

		DoubleColumn n = aggr.numberColumn("Count [trip_id]").asDoubleColumn();
		for (int row : n.isMissing().toArray()) {
			n.set(row, 0.0);
		}
		aggr.replaceColumn("Count [trip_id]", n);

		StringColumn subpop = aggr.stringColumn("subpopulation");
		StringColumn modelType = StringColumn.create("modelType");

		for (String subpopulation : subpop) {
			String foundModelType = getModelType(subpopulation).toString();
			modelType.append(foundModelType);
		}
		aggr.replaceColumn("modelType", modelType);

		return aggr;
	}

	private void writeTripStats(Table trips) throws IOException {

		Map<String, Object2IntMap<String>> nBySubpopulationGroup = new HashMap<>();
		Map<String, Object2LongMap<String>> travelTimeBySubpopulationGroup = new HashMap<>();
		Map<String, Object2LongMap<String>> travelDistanceBySubpopulationGroup = new HashMap<>();
		Map<String, Object2LongMap<String>> beelineDistanceBySubpopulationGroup = new HashMap<>();
		Map<String, Map<String, DoubleList>> speedsBySubpopulationGroup = new HashMap<>();

		for (Row trip : trips) {
			String group = getGroupOfSubpopulation(trip.getString("subpopulation"));
			String mainMode = trip.getString("main_mode");

			Object2IntMap<String> n =
				nBySubpopulationGroup.computeIfAbsent(group, k -> new Object2IntLinkedOpenHashMap<>());
			Object2LongMap<String> travelTime =
				travelTimeBySubpopulationGroup.computeIfAbsent(group, k -> new Object2LongOpenHashMap<>());
			Object2LongMap<String> travelDistance =
				travelDistanceBySubpopulationGroup.computeIfAbsent(group, k -> new Object2LongOpenHashMap<>());
			Object2LongMap<String> beelineDistance =
				beelineDistanceBySubpopulationGroup.computeIfAbsent(group, k -> new Object2LongOpenHashMap<>());
			Map<String, DoubleList> speeds =
				speedsBySubpopulationGroup.computeIfAbsent(group, k -> new HashMap<>());

			int travTime = durationToSeconds(trip.getString("trav_time"));
			long traveledDistance = trip.getLong("traveled_distance");
			long euclid = trip.getLong("euclidean_distance");

			n.mergeInt(mainMode, 1, Integer::sum);
			travelTime.mergeLong(mainMode, travTime, Long::sum);
			travelDistance.mergeLong(mainMode, traveledDistance, Long::sum);
			beelineDistance.mergeLong(mainMode, euclid, Long::sum);

			double speed = 3.6d * traveledDistance / (double) travTime;
			if (Double.isFinite(speed) && !Double.isNaN(speed))
				speeds.computeIfAbsent(mainMode, s -> new DoubleArrayList()).add(speed);
		}

		for (String group : groupsOfSubpopulationsForPersonAnalysis.keySet()) {
			analyseAndWriteTripStatsPerGroup(group, nBySubpopulationGroup, travelTimeBySubpopulationGroup, travelDistanceBySubpopulationGroup,
				beelineDistanceBySubpopulationGroup,
				speedsBySubpopulationGroup);
		}
		for (String group : groupsOfSubpopulationsForCommercialAnalysis.keySet()) {
			analyseAndWriteTripStatsPerGroup(group, nBySubpopulationGroup, travelTimeBySubpopulationGroup, travelDistanceBySubpopulationGroup,
				beelineDistanceBySubpopulationGroup,
				speedsBySubpopulationGroup);
		}
		analyseAndWriteTripStatsPerGroup("total", nBySubpopulationGroup, travelTimeBySubpopulationGroup, travelDistanceBySubpopulationGroup,
			beelineDistanceBySubpopulationGroup,
			speedsBySubpopulationGroup);
	}

	private void analyseAndWriteTripStatsPerGroup(String group, Map<String, Object2IntMap<String>> nBySubpopulationGroup,
												  Map<String, Object2LongMap<String>> travelTimeBySubpopulationGroup,
												  Map<String, Object2LongMap<String>> travelDistanceBySubpopulationGroup,
												  Map<String, Object2LongMap<String>> beelineDistanceBySubpopulationGroup,
												  Map<String, Map<String, DoubleList>> speedsBySubpopulationGroup) throws IOException {
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("trip_stats_%s.csv", group)), CSVFormat.DEFAULT)) {

			printer.print("Info");
			for (String m : modeOrder) {
				if (Objects.equals(group, "total") || nBySubpopulationGroup.get(group).getInt(m) > 0)
					printer.print(m);
			}
			printer.println();

			printer.print("Number of trips");
			for (String m : modeOrder) {
				if (group.equals("total")) {
					int sum = 0;
					for (Object2IntMap<String> n : nBySubpopulationGroup.values()) {
						sum += n.getInt(m);
					}
					printer.print(sum);
				} else {
					Object2IntMap<String> n = nBySubpopulationGroup.get(group);
					if (n.containsKey(m))
						printer.print(n.getInt(m));
				}
			}
			printer.println();

			printer.print("Total time traveled [h]");
			for (String m : modeOrder) {
				long seconds;
				if (group.equals("total")) {
					long sum = 0L;
					for (Object2LongMap<String> tt : travelTimeBySubpopulationGroup.values()) {
						sum += tt.getLong(m);
					}
					seconds = sum;
				} else {
					Object2LongMap<String> tt = travelTimeBySubpopulationGroup.get(group);
					if (!tt.containsKey(m))
						continue;
					else
						seconds = tt.getLong(m);
				}
				printer.print(new BigDecimal(seconds / 3600d).setScale(0, RoundingMode.HALF_UP));
			}
			printer.println();

			printer.print("Total distance traveled [km]");
			for (String m : modeOrder) {
				long meters;
				if (group.equals("total")) {
					long sum = 0L;
					for (Object2LongMap<String> td : travelDistanceBySubpopulationGroup.values()) {
						sum += td.getLong(m);
					}
					meters = sum;
				} else {
					Object2LongMap<String> td = travelDistanceBySubpopulationGroup.get(group);
					if (!td.containsKey(m))
						continue;
					else
						meters = td.getLong(m);
				}
				printer.print(new BigDecimal(meters / 1000d).setScale(0, RoundingMode.HALF_UP));
			}
			printer.println();

			printer.print("Avg. speed [km/h]");
			for (String m : modeOrder) {
				long seconds;
				long meters;
				if (group.equals("total")) {
					long secSum = 0L, mSum = 0L;
					for (String sub : nBySubpopulationGroup.keySet()) {
						Object2LongMap<String> tt = travelTimeBySubpopulationGroup.get(sub);
						Object2LongMap<String> td = travelDistanceBySubpopulationGroup.get(sub);
						if (tt != null) secSum += tt.getLong(m);
						if (td != null) mSum += td.getLong(m);
					}
					seconds = secSum;
					meters = mSum;
				} else {
					Object2LongMap<String> tt = travelTimeBySubpopulationGroup.get(group);
					Object2LongMap<String> td = travelDistanceBySubpopulationGroup.get(group);
					if (!td.containsKey(m))
						continue;
					seconds = (tt == null) ? 0L : tt.getLong(m);
					meters = td.getLong(m);
				}
				double timeH = seconds / 3600d;
				double distKm = meters / 1000d;
				double speed = timeH > 0 ? distKm / timeH : 0d;
				printer.print(new BigDecimal(speed).setScale(2, RoundingMode.HALF_UP));
			}
			printer.println();

			printer.print("Avg. beeline speed [km/h]");
			for (String m : modeOrder) {
				long seconds;
				long metersBee;
				if (group.equals("total")) {
					long secSum = 0L, beeSum = 0L;
					for (String sub : nBySubpopulationGroup.keySet()) {
						Object2LongMap<String> tt = travelTimeBySubpopulationGroup.get(sub);
						Object2LongMap<String> bee = beelineDistanceBySubpopulationGroup.get(sub);
						if (tt != null) secSum += tt.getLong(m);
						if (bee != null) beeSum += bee.getLong(m);
					}
					seconds = secSum;
					metersBee = beeSum;
				} else {
					Object2LongMap<String> tt = travelTimeBySubpopulationGroup.get(group);
					Object2LongMap<String> bee = beelineDistanceBySubpopulationGroup.get(group);
					if (!tt.containsKey(m))
						continue;
					seconds = tt.getLong(m);
					metersBee = (bee == null) ? 0L : bee.getLong(m);
				}
				double timeH = seconds / 3600d;
				double beeKm = metersBee / 1000d;
				double speed = timeH > 0 ? beeKm / timeH : 0d;
				printer.print(new BigDecimal(speed).setScale(2, RoundingMode.HALF_UP));
			}
			printer.println();

			printer.print("Avg. distance per trip [km]");
			for (String m : modeOrder) {
				long meters;
				int nTrips;
				if (group.equals("total")) {
					long mSum = 0L;
					int nSum = 0;
					for (String sub : nBySubpopulationGroup.keySet()) {
						Object2LongMap<String> td = travelDistanceBySubpopulationGroup.get(sub);
						Object2IntMap<String> n = nBySubpopulationGroup.get(sub);
						if (td != null) mSum += td.getLong(m);
						if (n != null) nSum += n.getInt(m);
					}
					meters = mSum;
					nTrips = nSum;
				} else {
					Object2LongMap<String> td = travelDistanceBySubpopulationGroup.get(group);
					Object2IntMap<String> n = nBySubpopulationGroup.get(group);
					if (!n.containsKey(m))
						continue;
					meters = (td == null) ? 0L : td.getLong(m);
					nTrips = n.getInt(m);
				}
				double avg = nTrips > 0 ? (meters / 1000d) / nTrips : 0d;
				printer.print(new BigDecimal(avg).setScale(2, RoundingMode.HALF_UP));
			}
			printer.println();

			printer.print("Avg. speed per trip [km]");
			for (String m : modeOrder) {
				double avg;
				if (group.equals("total")) {
					double sum = 0d;
					long cnt = 0L;
					for (Map.Entry<String, Map<String, DoubleList>> e : speedsBySubpopulationGroup.entrySet()) {
						DoubleList dl = e.getValue() == null ? null : e.getValue().get(m);
						if (dl != null && !dl.isEmpty()) {
							// Summe und Anzahl der Samples sammeln (gewichtetes Mittel)
							for (int i = 0; i < dl.size(); i++) sum += dl.getDouble(i);
							cnt += dl.size();
						}
					}
					avg = cnt > 0 ? (sum / cnt) : 0d;
				} else {
					Map<String, DoubleList> map = speedsBySubpopulationGroup.get(group);
					if (!map.containsKey(m))
						continue;
					DoubleList dl = map.get(m);
					avg = (dl == null || dl.isEmpty())
						? 0d
						: dl.doubleStream().average().orElse(0d);
				}
				printer.print(new BigDecimal(avg).setScale(2, RoundingMode.HALF_UP));
			}
			printer.println();
		}
	}

	private void writePopulationStats(Table persons, Table trips) throws IOException {

		HashMap<String, Object2IntMap<String>> tripsPerPerson = new HashMap<>();
		HashMap<String, Map<String, Set<String>>> modesPerPerson = new HashMap<>();
		List<String> subpVals = trips.stringColumn("subpopulation").unique().asList();
		subpVals.forEach(s -> {
		    String group = getGroupOfSubpopulation(s);
		    tripsPerPerson.computeIfAbsent(group, k -> new Object2IntLinkedOpenHashMap<>());
		    modesPerPerson.computeIfAbsent(group, k -> new LinkedHashMap<>());
		});

		for (Row trip : trips) {
			String id = trip.getString("person");
			String subpopulation = trip.getString("subpopulation");
			String group = getGroupOfSubpopulation(subpopulation);
			tripsPerPerson.get(group).mergeInt(id, 1, Integer::sum);
			String mode = trip.getString("main_mode");
			modesPerPerson.get(group).computeIfAbsent(id, s -> new LinkedHashSet<>()).add(mode);
		}

		HashMap<String, Object2IntMap<String>> usedModes = new HashMap<>();
		for (String group : modesPerPerson.keySet()) {
			for (Set<String> modes : modesPerPerson.get(group).values()) {
				for (String mode : modes) {
					usedModes.computeIfAbsent(group, s -> new Object2IntLinkedOpenHashMap<>()).mergeInt(mode, 1, Integer::sum);
				}
			}
		}
		HashMap<String, Double> totalMobilePerSubpopulation = new HashMap<>();
		HashMap<String, Double> totalAvgTripsMobilePerSubpopulation = new HashMap<>();
		HashMap<String, Double> totalAvgTripsPerSubpopulation = new HashMap<>();


		for (String group : tripsPerPerson.keySet()) {
			totalMobilePerSubpopulation.put(group, (double) tripsPerPerson.get(group).size());
			totalAvgTripsMobilePerSubpopulation.put(group, tripsPerPerson.get(group).values().intStream().average().orElse(0d));
		}
		for (Row person : persons) {
			String id = person.getString("person");
			String groupOfPerson = getGroupOfSubpopulation(person.getString("subpopulation"));
			if (!tripsPerPerson.get(groupOfPerson).containsKey(id))
				tripsPerPerson.get(groupOfPerson).put(id, 0);
		}
		for (String group : tripsPerPerson.keySet()) {
			totalAvgTripsPerSubpopulation.put(group, tripsPerPerson.get(group).values().intStream().average().orElse(0d));

			Table table = Table.create("modal_share",	StringColumn.create("main_mode"),DoubleColumn.create("user"), StringColumn.create("group"));
			for (String m : modeOrder) {
				int n = usedModes.get(group).getInt(m);
				if (n == 0) continue;

				double share = new BigDecimal(n / totalMobilePerSubpopulation.get(group)).setScale(2, RoundingMode.HALF_UP).doubleValue();

				table.stringColumn("main_mode").append(m);
				table.doubleColumn("user").append(share);
				table.stringColumn("group").append(group);
			}
			table.write().csv(output.getPath("mode_users_%s.csv", group).toFile());
		}

		Table table = Table.create("modal_share",	StringColumn.create("main_mode"),DoubleColumn.create("user"), StringColumn.create("group"));
		for (String m : modeOrder) {
			int n = usedModes.values().stream().mapToInt(map -> map.getInt(m)).sum();
			if (n == 0) continue;

			double share = new BigDecimal(n / (double)totalMobilePerSubpopulation.values().stream().mapToInt(Double::intValue).sum()).setScale(2, RoundingMode.HALF_UP).doubleValue();

			table.stringColumn("main_mode").append(m);
			table.doubleColumn("user").append(share);
			table.stringColumn("group").append("total");
		}
		table.write().csv(output.getPath("mode_users_%s.csv", "total").toFile());


		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("population_trip_stats.csv")), CSVFormat.DEFAULT)) {
			printer.printRecord(
				Stream.concat(
					Stream.concat(Stream.of("Group"), tripsPerPerson.keySet().stream()),
					Stream.of("total")
				).toList()
			);

			List<String> recordPersons = new ArrayList<>();
			List<String> recordMobilePersons = new ArrayList<>();
			List<String> recordAvgTrips = new ArrayList<>();
			List<String> recordAvgTripsMobile = new ArrayList<>();
			recordPersons.add("Persons");
			recordMobilePersons.add("Mobile persons [%]");
			recordAvgTrips.add("Avg. trips");
			recordAvgTripsMobile.add("Avg. trips per mobile persons");
			for (String group : tripsPerPerson.keySet()) {
				recordPersons.add(String.valueOf(tripsPerPerson.get(group).size()));
				recordMobilePersons.add(new BigDecimal(100 * totalMobilePerSubpopulation.get(group) / tripsPerPerson.get(group).size()).setScale(2, RoundingMode.HALF_UP).toString());
				recordAvgTrips.add(new BigDecimal(totalAvgTripsPerSubpopulation.get(group)).setScale(2, RoundingMode.HALF_UP).toString());
				recordAvgTripsMobile.add(new BigDecimal(totalAvgTripsMobilePerSubpopulation.get(group)).setScale(2, RoundingMode.HALF_UP).toString());
			}
			recordPersons.add(String.valueOf(tripsPerPerson.values().stream().mapToInt(Object2IntMap::size).sum()));
			recordMobilePersons.add(new BigDecimal(100 * totalMobilePerSubpopulation.values().stream().mapToInt(Double::intValue).sum() / (double) tripsPerPerson.values().stream().mapToInt(Object2IntMap::size).sum()).setScale(2, RoundingMode.HALF_UP).toString());
			recordAvgTrips.add(new BigDecimal(totalAvgTripsPerSubpopulation.values().stream().mapToDouble(Double::doubleValue).average().orElse(0d)).setScale(2, RoundingMode.HALF_UP).toString());
			recordAvgTripsMobile.add(new BigDecimal(totalAvgTripsMobilePerSubpopulation.values().stream().mapToDouble(Double::doubleValue).average().orElse(0d)).setScale(2, RoundingMode.HALF_UP).toString());
			printer.printRecord(recordPersons);
			printer.printRecord(recordMobilePersons);
			printer.printRecord(recordAvgTrips);
			printer.printRecord(recordAvgTripsMobile);
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

		for (String group : groupsOfSubpopulationsForPersonAnalysis.keySet()) {
			Table filtered = trips.where(
				trips.stringColumn("subpopulation").isIn(groupsOfSubpopulationsForPersonAnalysis.get(group))
			);
			calculateArrivalAndDepartures(group, filtered);
		}
		for (String group : groupsOfSubpopulationsForCommercialAnalysis.keySet()) {
			Table filtered = trips.where(
				trips.stringColumn("subpopulation").isIn(groupsOfSubpopulationsForCommercialAnalysis.get(group))
			);
			calculateArrivalAndDepartures(group, filtered);
		}
		calculateArrivalAndDepartures("total", trips);
	}

	private void calculateArrivalAndDepartures(String group, Table filtered) {
		Table tArrival = filtered.summarize("trip_id", count).by("end_activity_type", "arrival_h");

		tArrival.column(0).setName("purpose");
		tArrival.column(1).setName("h");

		DoubleColumn share = tArrival.numberColumn(2).divide(tArrival.numberColumn(2).sum()).setName("arrival");
		tArrival.replaceColumn(2, share);

		Table tDeparture = filtered.summarize("trip_id", count).by("end_activity_type", "departure_h");

		tDeparture.column(0).setName("purpose");
		tDeparture.column(1).setName("h");

		share = tDeparture.numberColumn(2).divide(tDeparture.numberColumn(2).sum()).setName("departure");
		tDeparture.replaceColumn(2, share);


		Table table = new DataFrameJoiner(tArrival, "purpose", "h").fullOuter(tDeparture).sortOn(0, 1);

		table.doubleColumn("departure").setMissingTo(0.0);
		table.doubleColumn("arrival").setMissingTo(0.0);

		table.write().csv(output.getPath("trip_purposes_by_hour_%s.csv", group).toFile());
	}

	private void writeTripDistribution(Table trips) throws IOException {


		// Note that the results of this interpolator are consistent with the one performed in matsim-python-tools
		// This makes the results comparable with reference data, changes here will also require changes in the python package
		LoessInterpolator inp = new LoessInterpolator(0.05, 0);

		long max = distGroups.get(distGroups.size() - 3) + distGroups.get(distGroups.size() - 2);

		double[] bins = IntStream.range(0, (int) (max / 100)).mapToDouble(i -> i * 100).toArray();
		double[] x = Arrays.copyOf(bins, bins.length - 1);
		for (String group : groupsOfSubpopulationsForPersonAnalysis.keySet()) {
			Table filtered = trips.where(
				trips.stringColumn("subpopulation").isIn(groupsOfSubpopulationsForPersonAnalysis.get(group))
			);
			writeTripDistributionPerGroup(filtered, bins, inp, x, group);
		}
		for (String group : groupsOfSubpopulationsForCommercialAnalysis.keySet()) {
			Table filtered = trips.where(
				trips.stringColumn("subpopulation").isIn(groupsOfSubpopulationsForCommercialAnalysis.get(group))
			);
			writeTripDistributionPerGroup(filtered, bins, inp, x, group);
		}
		writeTripDistributionPerGroup(trips, bins, inp, x, "total");
	}

	private void writeTripDistributionPerGroup(Table trips, double[] bins, LoessInterpolator inp, double[] x, String group) throws IOException {
		Map<String, double[]> dists = new LinkedHashMap<>();

		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("mode_share_distance_distribution_%s.csv", group)),
			CSVFormat.DEFAULT)) {
			printer.print("dist");

			for (String mode : modeOrder) {
				double[] distances = trips.where(
						trips.stringColumn("main_mode").equalsIgnoreCase(mode))
					.numberColumn("traveled_distance").asDoubleArray();
				if (distances.length == 0) continue;
				printer.print(mode);

				double[] hist = calcHistogram(distances, bins);

				double[] y = inp.smooth(x, hist);
				dists.put(mode, y);
			}

			printer.println();

			for (int i = 0; i < x.length; i++) {

				double sum = 0;
				for (String s : modeOrder) {
					if (dists.containsKey(s))
						sum += Math.max(0, dists.get(s)[i]);
				}

				printer.print(x[i]);
				for (String s : modeOrder) {
					if (dists.containsKey(s)) {
						double value = Math.max(0, dists.get(s)[i]) / sum;
						printer.print(value);
					}
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

		if (!groupsOfSubpopulationsForPersonAnalysis.isEmpty())
			// filters for all subpopulations that are used for person analysis
			joined = joined.where(joined.stringColumn("subpopulation").isIn(groupsOfSubpopulationsForPersonAnalysis.values().stream()
				.flatMap(Collection::stream)
				.collect(Collectors.toSet())));

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
		// filters for all subpopulations that are used for person analysis
		Table filteredForPersons = trips.where(trips.stringColumn("subpopulation").isIn(groupsOfSubpopulationsForPersonAnalysis.values().stream()
			.flatMap(Collection::stream)
			.collect(Collectors.toSet())));
		Table aggr = filteredForPersons.summarize("trip_id", count).by("end_activity_type", "main_mode");

		Comparator<Row> cmp = Comparator.comparing(row -> row.getString("end_activity_type"));
		aggr = aggr.sortOn(cmp.thenComparing(row -> row.getString("main_mode")));

		aggr.doubleColumn(aggr.columnCount() - 1).setName("share");
		aggr.column("end_activity_type").setName("purpose");

		Set<String> purposes = (Set<String>) aggr.column("purpose").asSet();

		// Norm each purpose to 1
		// It was not clear if the purpose is a string or text colum, therefor this code uses the abstract version
		for (String label : purposes) {
			DoubleColumn all = aggr.doubleColumn("share");
			Selection sel = ((StringColumn) aggr.column("purpose")).isEqualTo(label);

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
