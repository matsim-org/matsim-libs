package org.matsim.application.analysis.activity;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.Property;
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
	@CommandLine.Option(names = "--id-column", description = "Column to use as ID for the shapefile", required = true)
	private String idColumn;

	@CommandLine.Option(names = "--activity-mapping", description = "Map of patterns to merge activity types", split = ";")
	private Map<String, String> activityMapping;

	public static void main(String[] args) {
		new ActivityCountAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		HashMap<String, Set<String>> formattedActivityMapping = new HashMap<>();

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

		ShpOptions.Index index = shp.createIndex(idColumn);


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
			Feature feature = index.queryFeature(coord);

			if (feature == null) {
				continue;
			}

			Property prop = feature.getProperty(idColumn);
			if (prop == null)
				throw new IllegalArgumentException("No property found for column %s".formatted(idColumn));

			Object region = prop.getValue();
			if (region != null && region.toString().length() > 0) {

				// Add region to the activity counts and person activity tracker if not already present
				regionActivityCounts.computeIfAbsent(region, k -> new Object2IntOpenHashMap<>());
				personActivityTracker.computeIfAbsent(region, k -> new HashSet<>());

				Set<String> trackedActivities = personActivityTracker.get(region);
				String personActivityKey = person + "_" + activity;

				// adding activity only if it has not been counted for the person in the region
				if (!trackedActivities.contains(personActivityKey)) {
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
			TextColumn regionColumn = TextColumn.create("region");
			DoubleColumn activityColumn = DoubleColumn.create("count");
			resultTable.addColumns(regionColumn, activityColumn);
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
				row.setString("region", region.toString());
				row.setDouble("count", value);
			}
			resultTable.addColumns(activityColumn.divide(activityColumn.sum()).setName("count_normalized"));
			resultTable.write().csv(output.getPath("activities_%s_per_region.csv", activity).toFile());
			log.info("Wrote activity counts for {} to {}", activity, output.getPath("activities_%s_per_region.csv", activity));
		}

		return 0;
	}
}
