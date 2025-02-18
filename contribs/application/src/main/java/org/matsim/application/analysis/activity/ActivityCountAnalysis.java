package org.matsim.application.analysis.activity;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.*;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.selection.Selection;

import java.util.*;
import java.util.regex.Pattern;

@CommandSpec(
	requires = {"activities.csv"},
	produces = {"activities_%s_per_region.csv"}
)
public class ActivityCountAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(ActivityCountAnalysis.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(ActivityCountAnalysis.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(ActivityCountAnalysis.class);
	@CommandLine.Mixin
	private ShpOptions shp;
	@CommandLine.Mixin
	private SampleOptions sample;
	@CommandLine.Mixin
	private CrsOptions crs;

	/**
	 * Specifies the column in the shapefile used as the region ID.
	 */
	@CommandLine.Option(names = "--id-column", description = "Column to use as ID for the shapefile", required = true)
	private String idColumn;

	/**
	 * Maps patterns to merge activity types into a single category.
	 * Example: `home;work` can merge activities "home1" and "work1" into categories "home" and "work".
	 */
	@CommandLine.Option(names = "--activity-mapping", description = "Map of patterns to merge activity types", split = ";")
	private Map<String, String> activityMapping;

	/**
	 * Specifies activity types that should be counted only once per agent per region.
	 */
	@CommandLine.Option(names = "--single-occurrence", description = "Activity types that are only counted once per agent", split = ";")
	private Set<String> singleOccurrence;

	public static void main(String[] args) {
		new ActivityCountAnalysis().execute(args);
	}

	/**
	 * Executes the activity count analysis.
	 *
	 * @return Exit code (0 for success).
	 * @throws Exception if errors occur during execution.
	 */
	@Override
	public Integer call() throws Exception {

		// Prepares the activity mappings and reads input data
		HashMap<String, Set<String>> formattedActivityMapping = new HashMap<>();
		Map<String, Double> regionAreaMap = new HashMap<>();

		if (this.activityMapping == null) this.activityMapping = new HashMap<>();

		for (Map.Entry<String, String> entry : this.activityMapping.entrySet()) {
			String pattern = entry.getKey();
			String activity = entry.getValue();
			Set<String> activities = new HashSet<>(Arrays.asList(activity.split(",")));
			formattedActivityMapping.put(pattern, activities);
		}

		// Reading the input csv
		Table activities = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(input.getPath("activities.csv")))
			.columnTypesPartial(Map.of("person", ColumnType.TEXT, "activity_type", ColumnType.TEXT))
			.sample(false)
			.separator(CsvOptions.detectDelimiter(input.getPath("activities.csv"))).build());

		// remove the underscore and the number from the activity_type column
		TextColumn activityType = activities.textColumn("activity_type");
		activityType.set(Selection.withRange(0, activityType.size()), activityType.replaceAll("_[0-9]{2,}$", ""));

		ShpOptions.Index index = crs.getInputCRS() == null ? shp.createIndex(idColumn) : shp.createIndex(crs.getInputCRS(), idColumn);

		// stores the counts of activities per region
		Object2ObjectOpenHashMap<Object, Object2IntMap<String>> regionActivityCounts = new Object2ObjectOpenHashMap<>();
		// stores the activities that have been counted for each person in each region
		Object2ObjectOpenHashMap<Object, Set<String>> personActivityTracker = new Object2ObjectOpenHashMap<>();

		// iterate over the csv rows
		for (Row row : activities) {
			String person = row.getString("person");
			String activity = row.getText("activity_type");

			for (Map.Entry<String, Set<String>> entry : formattedActivityMapping.entrySet()) {
				String pattern = entry.getKey();
				Set<String> activities2 = entry.getValue();
				for (String act : activities2) {
					if (Pattern.matches(act, activity)) {
						activity = pattern;
						break;
					}
				}
			}

			Coord coord = new Coord(row.getDouble("coord_x"), row.getDouble("coord_y"));

			// get the region for the current coordinate
			SimpleFeature feature = index.queryFeature(coord);

			if (feature == null) {
				continue;
			}

			Geometry geometry = (Geometry) feature.getDefaultGeometry();

			Property prop = feature.getProperty(idColumn);
			if (prop == null)
				throw new IllegalArgumentException("No property found for column %s".formatted(idColumn));

			Object region = prop.getValue();
			if (region != null && region.toString().length() > 0) {

				double area = geometry.getArea();
				regionAreaMap.put(region.toString(), area);

				// Add region to the activity counts and person activity tracker if not already present
				regionActivityCounts.computeIfAbsent(region, k -> new Object2IntOpenHashMap<>());
				personActivityTracker.computeIfAbsent(region, k -> new HashSet<>());

				Set<String> trackedActivities = personActivityTracker.get(region);
				String personActivityKey = person + "_" + activity;

				// adding activity only if it has not been counted for the person in the region
				if (singleOccurrence == null || !singleOccurrence.contains(activity) || !trackedActivities.contains(personActivityKey)) {
					Object2IntMap<String> activityCounts = regionActivityCounts.get(region);
					activityCounts.mergeInt(activity, 1, Integer::sum);

					// mark the activity as counted for the person in the region
					trackedActivities.add(personActivityKey);
				}
			}
		}

		Set<String> uniqueActivities = new HashSet<>();

		for (Object2IntMap<String> map : regionActivityCounts.values()) {
			uniqueActivities.addAll(map.keySet());
		}

		for (String activity : uniqueActivities) {
			Table resultTable = Table.create();
			TextColumn regionColumn = TextColumn.create("id");
			DoubleColumn activityColumn = DoubleColumn.create("count");
			DoubleColumn distributionColumn = DoubleColumn.create("relative_density");
			DoubleColumn countRatioColumn = DoubleColumn.create("density");
			DoubleColumn areaColumn = DoubleColumn.create("area");

			resultTable.addColumns(regionColumn, activityColumn, distributionColumn, countRatioColumn, areaColumn);
			for (Map.Entry<Object, Object2IntMap<String>> entry : regionActivityCounts.entrySet()) {
				Object region = entry.getKey();
				double value = 0;
				for (Map.Entry<String, Integer> entry2 : entry.getValue().object2IntEntrySet()) {
					String ect = entry2.getKey();
					if (Pattern.matches(ect, activity)) {
						value = entry2.getValue() * sample.getUpscaleFactor();
						break;
					}
				}


				Row row = resultTable.appendRow();
				row.setString("id", region.toString());
				row.setDouble("count", value);
			}

			for (Row row : resultTable) {
				Double area = regionAreaMap.get(row.getString("id"));
				if (area != null) {
					row.setDouble("area", area);
					row.setDouble("density", row.getDouble("count") / area);
				} else {
					log.warn("Area for region {} is not found", row.getString("id"));
				}
			}

			Double averageDensity = countRatioColumn.mean();

			for (Row row : resultTable) {
				Double value = row.getDouble("density");
				if (averageDensity != 0) {
					row.setDouble("relative_density", value / averageDensity);
				} else {
					row.setDouble("relative_density", 0.0);
				}
			}


			resultTable.write().csv(output.getPath("activities_%s_per_region.csv", activity).toFile());
			log.info("Wrote activity counts for {} to {}", activity, output.getPath("activities_%s_per_region.csv", activity));
		}

		return 0;
	}
}
